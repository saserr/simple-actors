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

package android.actor;

import android.actor.executor.Executable;
import android.actor.messenger.BufferedMessenger;
import android.actor.messenger.Mailbox;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.NotThreadSafe;
import net.jcip.annotations.ThreadSafe;

import org.jetbrains.annotations.NonNls;

import java.lang.annotation.Retention;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static android.util.Log.DEBUG;
import static android.util.Log.INFO;
import static java.lang.annotation.RetentionPolicy.SOURCE;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

@ThreadSafe
public class Reference<M> implements Executable {

    private static final String TAG = Reference.class.getSimpleName();

    @NonNull
    private final Context mContext;
    @NonNull
    private final Actor.Name mName;
    @NonNull
    private final Actor<M> mActor;
    @NonNull
    private final Callback mCallback;
    @NonNls
    @NonNull
    private final String mActorStopped;

    private final Lock mLock = new ReentrantLock();
    private final Mailbox<M> mMailbox = new Mailbox<>();

    @Nullable
    @GuardedBy("mLock")
    private BufferedMessenger<M> mMessenger;
    @Nullable
    @GuardedBy("mLock")
    private Task<M> mTask;

    @GuardedBy("mLock")
    private boolean mStopped = false;

    public Reference(@NonNull final Context context,
                     @NonNull final Actor<M> actor,
                     @NonNull final Callback callback) {
        super();

        mContext = context;
        mName = context.getName();
        mActor = actor;
        mCallback = callback;
        mActorStopped = this + " is stopped";
    }

    @NonNull
    public final Actor.Name getName() {
        return mName;
    }

    public final boolean isStopped() {
        final boolean result;

        mLock.lock();
        try {
            result = mStopped;
        } finally {
            mLock.unlock();
        }

        return result;
    }

    public final boolean tell(@NonNull final M message) {
        return tell(message, 0, MILLISECONDS);
    }

    public final boolean tell(@NonNull final M message,
                              final int delay,
                              @NonNull final TimeUnit unit) {
        final boolean success;

        mLock.lock();
        try {
            if (mStopped) {
                throw new UnsupportedOperationException(mActorStopped);
            }

            final long delayInMillis = unit.toMillis(delay);
            success = (mMessenger == null) ?
                    mMailbox.put(message, delayInMillis) :
                    mMessenger.send(message, delayInMillis);
        } finally {
            mLock.unlock();
        }

        return success;
    }

    public final boolean stop() {
        final boolean success;

        mLock.lock();
        try {
            if (mStopped) {
                success = true;
            } else {
                if (mMessenger == null) {
                    throw new UnsupportedOperationException(mActorStopped);
                }

                success = mMessenger.isAttached() ?
                        mMessenger.send(ControlMessage.STOP) :
                        mMessenger.stop(false);
            }

            mStopped = true;
        } finally {
            mLock.unlock();
        }

        return success;
    }

    @Override
    public final boolean attach(@NonNull final Messenger.Factory factory) {
        final boolean success;

        mLock.lock();
        try {
            if (mStopped || (mMessenger == null)) {
                throw new UnsupportedOperationException(mActorStopped);
            }

            success = mMessenger.attach(factory);
        } finally {
            mLock.unlock();
        }

        if (success && Log.isLoggable(TAG, DEBUG)) {
            Log.d(TAG, this + " attached to new messenger"); //NON-NLS
        }

        return success;
    }

    @Override
    public final boolean detach() {
        final boolean success;

        mLock.lock();
        try {
            if (mStopped) {
                if (mMessenger == null) {
                    success = true;
                } else {
                    if (Log.isLoggable(TAG, DEBUG)) {
                        Log.d(TAG, this + " stopping the messenger"); //NON-NLS
                    }
                    success = mMessenger.stop(true);
                    mMessenger = null;
                }
            } else {
                if (mMessenger == null) {
                    throw new UnsupportedOperationException(mActorStopped);
                }

                mMessenger.detach();
                success = true;
            }
        } finally {
            mLock.unlock();
        }

        if (success && Log.isLoggable(TAG, DEBUG)) {
            Log.d(TAG, this + " detached from the messenger"); //NON-NLS
        }

        return success;
    }

    @NonNls
    @NonNull
    @Override
    public final String toString() {
        return "Actor(" + mName + ')';
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public final <N extends M> Reference<N> cast() {
        return (Reference<N>) this;
    }

    protected final boolean start(@NonNull final Executor executor) {
        boolean success = false;

        mLock.lock();
        try {
            if (mStopped) {
                throw new UnsupportedOperationException(mActorStopped);
            }

            if (mTask == null) {
                try {
                    mActor.postStart(mContext, this);
                    mTask = new Task<>(this, mActor);
                    mMessenger = new BufferedMessenger<>(mTask);
                    success = true;
                    while (success && !mMailbox.isEmpty()) {
                        success = mMailbox.take().send(mMessenger);
                    }
                } catch (final Throwable error) {
                    Log.e(TAG, this + " failed to start! Stopping", error); // NON-NLS
                    success = false;
                }
            } else {
                success = true;
            }

            if (success) {
                success = mTask.start(executor);
            } else {
                stop(true);
            }
        } finally {
            mLock.unlock();
        }

        if (success && Log.isLoggable(TAG, INFO)) {
            Log.i(TAG, this + " started"); //NON-NLS
        }

        return success;
    }

    protected final boolean pause() {
        final boolean success;

        mLock.lock();
        try {
            if (mStopped) {
                throw new UnsupportedOperationException(mActorStopped);
            }

            success = (mMessenger == null) || mMessenger.send(ControlMessage.PAUSE);
        } finally {
            mLock.unlock();
        }

        if (success && Log.isLoggable(TAG, INFO)) {
            Log.i(TAG, this + " paused"); //NON-NLS
        }

        return success;
    }

    protected final boolean stop(final boolean immediately) {
        boolean success = false;

        if (immediately) {
            mCallback.onStop(mName);

            mLock.lock();
            try {
                mStopped = true;

                if (mTask == null) {
                    success = true;
                } else {
                    mActor.preStop();
                    success = mTask.stop();
                }
            } catch (final Throwable error) {
                Log.e(TAG, this + " failed to stop ", error); // NON-NLS
            } finally {
                mLock.unlock();
            }
        } else {
            success = stop();
        }

        if (success && Log.isLoggable(TAG, INFO)) {
            Log.i(TAG, this + " stopped"); //NON-NLS
        }

        return success;
    }

    public interface Callback {
        void onStop(@NonNull final Actor.Name name);
    }

    @Retention(SOURCE)
    @IntDef({ControlMessage.PAUSE, ControlMessage.STOP})
    private @interface ControlMessage {
        int PAUSE = 1;
        int STOP = 2;
    }

    @NotThreadSafe
    private static final class Task<M> implements Messenger.Callback<M> {

        @NonNull
        private final Reference<M> mReference;
        @NonNull
        private final Actor<M> mActor;

        private final Semaphore mDirectCall = new Semaphore(1);

        @Nullable
        private Executor.Submission mSubmission;

        private Task(@NonNull final Reference<M> reference, @NonNull final Actor<M> actor) {
            super();

            mReference = reference;
            mActor = actor;
        }

        public boolean start(@NonNull final Executor executor) {
            final boolean stopped = stop();
            if (stopped) {
                mSubmission = executor.submit(mReference);
            }
            return stopped && (mSubmission != null);
        }

        public boolean stop() {
            final boolean success = (mSubmission == null) || mSubmission.stop();
            if (success) {
                mSubmission = null;
            }
            return success;
        }

        @Override
        public boolean onMessage(@NonNls final int message) {
            final boolean processed;

            switch (message) {
                case ControlMessage.PAUSE:
                    processed = (mSubmission == null) || mSubmission.stop();
                    mSubmission = null;
                    break;
                case ControlMessage.STOP:
                    processed = mReference.stop(true);
                    break;
                default:
                    processed = false;
            }

            if (processed && Log.isLoggable(TAG, DEBUG)) {
                Log.d(TAG, mReference + " handled system message " + message); //NON-NLS
            }

            return processed;
        }

        @Messenger.Delivery
        @Override
        public int onMessage(@NonNls @NonNull final M message) {
            int result = Messenger.Delivery.SUCCESS;

            if (mDirectCall.tryAcquire()) {
                try {
                    mActor.onMessage(message);
                } catch (final Throwable error) {
                    Log.e(TAG, mReference + " failed to receive message " + message + "! Stopping", error); // NON-NLS
                    if (!mReference.stop(true)) {
                        Log.e(TAG, mReference + " couldn't be stopped after the failure! Ignoring the failure"); // NON-NLS
                    }
                    result = Messenger.Delivery.FAILURE_NO_RETRY;
                } finally {
                    mDirectCall.release();
                }
            } else {
                result = Messenger.Delivery.FAILURE_CAN_RETRY;
            }

            if ((result == Messenger.Delivery.SUCCESS) && Log.isLoggable(TAG, DEBUG)) {
                Log.d(TAG, mReference + " handled user message " + message); //NON-NLS
            }

            return result;
        }
    }
}
