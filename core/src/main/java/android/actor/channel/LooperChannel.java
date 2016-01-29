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

package android.actor.channel;

import android.actor.Channel;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import net.jcip.annotations.NotThreadSafe;
import net.jcip.annotations.ThreadSafe;

import org.jetbrains.annotations.NonNls;

import static android.util.Log.DEBUG;
import static java.lang.Thread.currentThread;

@ThreadSafe
public class LooperChannel<M> implements Channel<M> {

    private static final String TAG = LooperChannel.class.getSimpleName();

    private final Object mToken = new Object();

    @NonNull
    private final Looper mLooper;
    @NonNull
    private final Channel<M> mDestination;
    @NonNull
    private final Handler mHandler;

    public LooperChannel(@NonNull final Looper looper,
                         @NonNull final Channel<M> destination) {
        super();

        mLooper = looper;
        mDestination = destination;
        mHandler = new Handler(looper);
    }

    public final boolean hasUndeliveredMessages() {
        return mHandler.hasMessages(Deliver.Type, mToken);
    }

    public final boolean isOnCurrentThread() {
        return mLooper.getThread() == currentThread();
    }

    @Channel.Delivery
    @Override
    public final int send(@NonNull final M message) {
        @Channel.Delivery int result = (isOnCurrentThread() && !hasUndeliveredMessages()) ?
                mDestination.send(message) :
                Channel.Delivery.FAILURE_CAN_RETRY;

        if (result == Channel.Delivery.FAILURE_CAN_RETRY) {
            result = dispatch(message) ?
                    Channel.Delivery.SUCCESS :
                    Channel.Delivery.FAILURE_NO_RETRY;
        }

        return result;
    }

    @Override
    public final boolean stop(final boolean immediately) {
        if (immediately) {
            mHandler.removeMessages(Deliver.Type, mToken);
        }

        return !hasUndeliveredMessages();
    }

    private boolean dispatch(@NonNls @NonNull final M message) {
        final Message msg = Message.obtain(mHandler, new Deliver<>(mDestination, message));
        msg.what = Deliver.Type;
        msg.obj = mToken;
        final boolean success = mHandler.sendMessage(msg);

        if (success) {
            if (Log.isLoggable(TAG, DEBUG)) {
                Log.d(TAG, message + " dispatched"); //NON-NLS
            }
        } else {
            Log.e(TAG, "Failed to dispatch " + message + '!'); //NON-NLS
        }

        return success;
    }

    @ThreadSafe
    @VisibleForTesting
    static class Deliver<M> implements Runnable {

        public static final int Type = 1;

        @NonNull
        private final Channel<M> mDestination;
        @NonNull
        private final M mMessage;

        protected Deliver(@NonNull final Channel<M> destination, @NonNull final M message) {
            super();

            mDestination = destination;
            mMessage = message;
        }

        @Override
        public final void run() {
            Send.withRetries(mDestination, mMessage);
        }
    }
}
