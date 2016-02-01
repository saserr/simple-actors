/*
 * Copyright 2015 the original author or authors
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
import android.actor.Executor;
import android.actor.executor.Executable;
import android.actor.util.Retry;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

import org.jetbrains.annotations.NonNls;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@ThreadSafe
public class Mailbox<M> implements Channel<M>, Executable {

    private static final String TAG = Mailbox.class.getSimpleName();

    @NonNull
    private final Channel<M> mDestination;

    @GuardedBy("mLock")
    private final Queue<M> mMessages = new LinkedList<>();
    private final Lock mLock = new ReentrantLock();

    @Nullable
    @GuardedBy("mLock")
    private Channel<M> mCarrier;

    @GuardedBy("mLock")
    private boolean mStopped = false;

    public Mailbox(@NonNull final Channel<M> destination) {
        super();

        mDestination = destination;
    }

    @Override
    @Channel.Delivery
    public final int send(@NonNull final M message) {
        mLock.lock();
        try {
            if (mStopped) {
                throw new UnsupportedOperationException(Channel.STOPPED);
            }
            return (mCarrier == null) ? sendLater(message) : mCarrier.send(message);
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public final boolean stop(final boolean immediately) {
        boolean success;

        mLock.lock();
        try {
            if (mStopped) {
                success = true;
            } else {
                if (mCarrier == null) {
                    success = immediately || drain();
                    mMessages.clear();
                    success = !success || mDestination.stop(immediately);
                } else {
                    success = mCarrier.stop(immediately);
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
    private boolean drain() {
        boolean success = true;

        while (success && !mMessages.isEmpty()) {
            final M message = mMessages.poll();
            @Retry.Result final int sending =
                    Send.withRetries(mDestination, message, 1);
            if (sending != Retry.SUCCESS) {
                @NonNls final String failure = "Delivery of " + message +
                        " to " + mDestination + " while stopping has failed";
                if (sending == Retry.AGAIN) {
                    Log.w(TAG, failure + "! Dropping the message"); //NON-NLS
                } else if (sending == Retry.FAILURE) {
                    Log.e(TAG, failure + " irrecoverably! Will not deliver any more messages"); //NON-NLS
                    success = false;
                }
            }
        }

        return success;
    }

    @Override
    public final boolean attach(@NonNull final Channel.Factory factory) {
        boolean success;

        mLock.lock();
        try {
            if (mStopped) {
                success = false;
            } else {
                if (mCarrier != null) {
                    throw new UnsupportedOperationException("Mailbox is already attached!");
                }

                success = true;
                mCarrier = factory.create(mDestination);
                while (success && !mMessages.isEmpty()) {
                    if (Send.withRetries(mCarrier, mMessages.poll()) != Retry.SUCCESS) {
                        success = false;
                        mCarrier = null;
                    }
                }
            }
        } finally {
            mLock.unlock();
        }

        return success;
    }

    @Override
    public final boolean detach() {
        mLock.lock();
        try {
            mCarrier = null;
            return true;
        } finally {
            mLock.unlock();
        }
    }

    @Delivery
    @GuardedBy("mLock")
    private int sendLater(@NonNull final M message) {
        return mMessages.offer(message) ? Delivery.SUCCESS : Delivery.FAILURE;
    }

    @ThreadSafe
    public static class Runner {

        @NonNull
        private final Mailbox<?> mMailbox;

        private final Lock mLock = new ReentrantLock();

        @Nullable
        @GuardedBy("mLock")
        private Executor.Submission mSubmission;

        public Runner(@NonNull final Mailbox<?> mailbox) {
            super();

            mMailbox = mailbox;
        }

        public final boolean start(@NonNull final Executor executor) {
            mLock.lock();
            try {
                final boolean success = stop();
                if (success) {
                    mSubmission = executor.submit(mMailbox);
                }
                return success && (mSubmission != null);
            } finally {
                mLock.unlock();
            }
        }

        public final boolean stop() {
            mLock.lock();
            try {
                final boolean success = (mSubmission == null) || mSubmission.stop();
                if (success) {
                    mSubmission = null;
                }
                return success;
            } finally {
                mLock.unlock();
            }
        }
    }
}
