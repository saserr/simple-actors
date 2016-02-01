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
import android.actor.util.Retry;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.NotThreadSafe;
import net.jcip.annotations.ThreadSafe;

import org.jetbrains.annotations.NonNls;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static android.util.Log.DEBUG;
import static java.lang.Thread.currentThread;

@NotThreadSafe
public class LooperChannel<M> implements Channel<M> {

    private static final String TAG = LooperChannel.class.getSimpleName();

    @VisibleForTesting
    static final int TYPE = 1;

    @NonNull
    private final Looper mLooper;
    @NonNull
    private final Channel<M> mDestination;
    @NonNull
    @GuardedBy("mLock")
    private final Handler mHandler;

    private final Object mToken = new Object();
    private final Lock mLock = new ReentrantLock();

    @GuardedBy("mLock")
    private boolean mStopped = false;

    public LooperChannel(@NonNull final Looper looper,
                         @NonNull final Channel<M> destination) {
        super();

        mLooper = looper;
        mDestination = destination;
        mHandler = new Handler(looper);
    }

    @Channel.Delivery
    @Override
    public final int send(@NonNls @NonNull final M message) {
        @Channel.Delivery int result;

        mLock.lock();
        try {
            if (mStopped) {
                throw new UnsupportedOperationException(Channel.STOPPED);
            }

            result = (isOnCurrentThread() && !hasUndeliveredMessages()) ?
                    mDestination.send(message) :
                    Delivery.FAILURE;

            if (result == Delivery.FAILURE) {
                result = dispatch(new Deliver<>(this, mDestination, message)) ?
                        Delivery.SUCCESS :
                        Delivery.ERROR;

                if (result == Delivery.SUCCESS) {
                    if (Log.isLoggable(TAG, DEBUG)) {
                        Log.d(TAG, message + " dispatched"); //NON-NLS
                    }
                } else {
                    Log.e(TAG, "Failed to dispatch " + message + '!'); //NON-NLS
                }
            }
        } finally {
            mLock.unlock();
        }

        return result;
    }

    @Override
    public final boolean stop(final boolean immediately) {
        final boolean success;

        mLock.lock();
        try {
            if (mStopped) {
                success = true;
            } else {
                if (immediately) {
                    mHandler.removeMessages(TYPE, mToken);
                    success = mDestination.stop(true);
                } else {
                    success = hasUndeliveredMessages() ?
                            dispatch(new Stop(mDestination)) :
                            mDestination.stop(false);
                }

                if (success) {
                    mStopped = true;
                }
            }
        } finally {
            mLock.unlock();
        }

        return success;
    }

    @GuardedBy("mLock")
    private boolean dispatch(@NonNull final Runnable runnable) {
        final Message msg = Message.obtain(mHandler, runnable);
        msg.what = TYPE;
        msg.obj = mToken;
        return mHandler.sendMessage(msg);
    }

    @VisibleForTesting
    @GuardedBy("mLock")
    final boolean hasUndeliveredMessages() {
        return mHandler.hasMessages(TYPE, mToken);
    }

    @VisibleForTesting
    final boolean isOnCurrentThread() {
        return mLooper.getThread() == currentThread();
    }

    @ThreadSafe
    @VisibleForTesting
    static class Deliver<M> implements Runnable {

        @NonNull
        private final Channel<M> mSource;
        @NonNull
        private final Channel<M> mDestination;
        @NonNull
        private final M mMessage;

        protected Deliver(@NonNull final Channel<M> source,
                          @NonNull final Channel<M> destination,
                          @NonNull final M message) {
            super();

            mSource = source;
            mDestination = destination;
            mMessage = message;
        }

        @Override
        public final void run() {
            @Retry.Result final int sending = Send.withRetries(mDestination, mMessage);
            if (sending != Retry.SUCCESS) {
                @NonNls final String failure =
                        "Delivery of " + mMessage + " to " + mDestination + " has failed";
                if (sending == Retry.AGAIN) {
                    Log.w(TAG, failure + "! Dropping the message"); //NON-NLS
                } else if (sending == Retry.FAILURE) {
                    Log.e(TAG, failure + " irrecoverably! Stopping"); //NON-NLS
                    mSource.stop(true);
                }
            }
        }
    }

    @ThreadSafe
    @VisibleForTesting
    static class Stop implements Runnable {

        @NonNull
        private final Channel<?> mDestination;

        protected Stop(@NonNull final Channel<?> destination) {
            super();

            mDestination = destination;
        }

        @Override
        public final void run() {
            mDestination.stop(false);
        }
    }
}
