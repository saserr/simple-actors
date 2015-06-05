/*
 * Copyright 2014 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.actor.executor;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;

import java.lang.annotation.Retention;

import static java.lang.Thread.currentThread;
import static java.lang.annotation.RetentionPolicy.SOURCE;

public class LooperMessenger<M> implements Messenger<M> {

    @NonNull
    private final Messenger.Callback<M> mCallback;
    @NonNull
    private final Handler mHandler;
    @NonNull
    private final Thread mThread;

    public LooperMessenger(@NonNull final Looper looper,
                           @NonNull final Messenger.Callback<M> callback) {
        super();

        mCallback = callback;
        mHandler = new Handler(looper, new HandlerCallback<>(callback));
        mThread = looper.getThread();
    }

    public final boolean hasUndeliveredMessages() {
        return mHandler.hasMessages(MessageType.SYSTEM) ||
                mHandler.hasMessages(MessageType.USER);
    }

    public final boolean isCurrentThread() {
        return mThread.equals(currentThread());
    }

    @Override
    public final boolean send(final int message) {
        return (!hasUndeliveredMessages() && isCurrentThread() && mCallback.onMessage(message)) ||
                mHandler.sendMessage(mHandler.obtainMessage(MessageType.SYSTEM, message, 0));
    }

    @Override
    public final boolean send(@NonNull final M message, final long delay) {
        final boolean success;

        if ((delay <= 0) && !hasUndeliveredMessages() && isCurrentThread() && mCallback.onMessage(message)) {
            success = true;
        } else {
            final Message msg = mHandler.obtainMessage(MessageType.USER, message);
            success = (delay > 0) ?
                    mHandler.sendMessageDelayed(msg, delay) :
                    mHandler.sendMessage(msg);
        }

        return success;
    }

    @Override
    public final boolean stop(final boolean immediately) {
        if (immediately) {
            mHandler.removeMessages(MessageType.SYSTEM);
            mHandler.removeMessages(MessageType.USER);
        }

        return !hasUndeliveredMessages();
    }

    @Retention(SOURCE)
    @IntDef({MessageType.SYSTEM, MessageType.USER})
    private @interface MessageType {
        int SYSTEM = 1;
        int USER = 2;
    }

    private static class HandlerCallback<M> implements Handler.Callback {

        @NonNull
        private final Messenger.Callback<M> mCallback;

        private HandlerCallback(@NonNull final Messenger.Callback<M> callback) {
            super();
            mCallback = callback;
        }

        @Override
        @SuppressWarnings("unchecked")
        public final boolean handleMessage(@NonNull final Message message) {
            final boolean processed;

            switch (message.what) {
                case MessageType.SYSTEM:
                    processed = mCallback.onMessage(message.arg1);
                    break;
                case MessageType.USER:
                    processed = mCallback.onMessage((M) message.obj);
                    break;
                default:
                    processed = false;
            }

            return processed;
        }
    }
}
