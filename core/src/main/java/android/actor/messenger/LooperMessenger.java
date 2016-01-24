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

package android.actor.messenger;

import android.actor.Messenger;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import net.jcip.annotations.ThreadSafe;

import java.lang.annotation.Retention;

import static java.lang.Thread.currentThread;
import static java.lang.annotation.RetentionPolicy.SOURCE;

@ThreadSafe
public class LooperMessenger<M> implements Messenger<M> {

    @NonNull
    private final Looper mLooper;
    @NonNull
    private final Messenger.Callback<M> mCallback;
    @NonNull
    private final Handler mHandler;

    public LooperMessenger(@NonNull final Looper looper,
                           @NonNull final Messenger.Callback<M> callback) {
        super();

        mLooper = looper;
        mCallback = callback;
        mHandler = new Handler(looper, new HandlerCallback<>(callback));
    }

    public final boolean hasUndeliveredMessages() {
        return mHandler.hasMessages(MessageType.CONTROL) ||
                mHandler.hasMessages(MessageType.USER);
    }

    public final boolean isOnCurrentThread() {
        return mLooper.getThread() == currentThread();
    }

    @Override
    public final boolean send(final int message) {
        return (isOnCurrentThread() && !hasUndeliveredMessages()) ?
                mCallback.onMessage(message) :
                mHandler.sendMessage(mHandler.obtainMessage(MessageType.CONTROL, message, 0));
    }

    @Override
    public final boolean send(@NonNull final M message) {
        final boolean success;

        @Messenger.Delivery final int delivered =
                (isOnCurrentThread() && !hasUndeliveredMessages()) ?
                        mCallback.onMessage(message) :
                        Delivery.FAILURE_CAN_RETRY;

        if (delivered == Delivery.FAILURE_CAN_RETRY) {
            success = mHandler.sendMessage(mHandler.obtainMessage(MessageType.USER, message));
        } else {
            success = delivered == Delivery.SUCCESS;
        }

        return success;
    }

    @Override
    public final boolean stop(final boolean immediately) {
        if (immediately) {
            mHandler.removeMessages(MessageType.CONTROL);
            mHandler.removeMessages(MessageType.USER);
        }

        return !hasUndeliveredMessages();
    }

    @Retention(SOURCE)
    @IntDef({MessageType.CONTROL, MessageType.USER})
    @VisibleForTesting
    @interface MessageType {
        int CONTROL = 1;
        int USER = 2;
    }

    @ThreadSafe
    private static final class HandlerCallback<M> implements Handler.Callback {

        @NonNull
        private final Messenger.Callback<M> mCallback;

        private HandlerCallback(@NonNull final Messenger.Callback<M> callback) {
            super();

            mCallback = callback;
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean handleMessage(@NonNull final Message message) {
            final boolean processed;

            switch (message.what) {
                case MessageType.CONTROL:
                    processed = mCallback.onMessage(message.arg1);
                    break;
                case MessageType.USER:
                    processed = mCallback.onMessage((M) message.obj) == Messenger.Delivery.SUCCESS;
                    break;
                default:
                    processed = false;
            }

            return processed;
        }
    }
}
