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

import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

import org.jetbrains.annotations.NonNls;

import java.lang.annotation.Retention;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static android.util.Log.DEBUG;
import static android.util.Log.INFO;
import static java.lang.annotation.RetentionPolicy.SOURCE;

public abstract class Actor<M> {

    // TODO an instance should only be allowed once to be register with a System

    protected abstract void onMessage(@NonNull final M m);

    protected void postStart(@NonNull final Context context,
                             @NonNull final Reference<M> self) {/* do nothing */}

    protected void preStop() {/* do nothing */}

    public interface Repository {
        @NonNull
        <M> Reference<M> register(@NonNls @NonNull final String name,
                                  @NonNull final Actor<M> actor);
    }

    @ThreadSafe
    public static class Name {

        @NonNls
        public static final char SEPARATOR = '/';

        @Nullable
        private final Name mParent;
        @NonNls
        @NonNull
        private final String mPart;

        public Name(@Nullable final Name parent, @NonNls @NonNull final String part) {
            super();

            if (part.indexOf(SEPARATOR) >= 0) {
                throw new IllegalArgumentException(
                        "Name part cannot contain '" + SEPARATOR + "' character"
                );
            }

            mParent = parent;
            mPart = part;
        }

        public final boolean isParentOf(@NonNull final Name other) {
            Name parent = other.mParent;

            while ((parent != null) && !equals(parent)) {
                parent = parent.mParent;
            }

            return parent != null;
        }

        @Override
        public final int hashCode() {
            return (31 * ((mParent == null) ? 0 : mParent.hashCode())) + mPart.hashCode();
        }

        @Override
        public final boolean equals(@Nullable final Object obj) {
            boolean result = this == obj;

            if (!result && (obj instanceof Name)) {
                final Name other = (Name) obj;
                result = mPart.equals(other.mPart) &&
                        ((mParent == null) ? (other.mParent == null) : mParent.equals(other.mParent));
            }

            return result;
        }

        @NonNls
        @NonNull
        @Override
        public final String toString() {
            return ((mParent == null) ? "" : mParent.toString()) + SEPARATOR + mPart;
        }
    }

    @ThreadSafe
    public static class Channel<M> implements android.actor.Channel<M> {

        private static final String TAG = Channel.class.getSimpleName();

        @NonNull
        private final Name mName;
        @NonNull
        @GuardedBy("mLock")
        private final Actor<M> mActor;

        private final Semaphore mDirectCall = new Semaphore(1);
        private final Lock mLock = new ReentrantLock();

        @State
        @GuardedBy("mLock")
        private int mState = State.INITIALIZED;

        public Channel(@NonNull final Name name, @NonNull final Actor<M> actor) {
            super();

            mName = name;
            mActor = actor;
        }

        @Override
        public final int send(@NonNls @NonNull final M message) {
            int result = Delivery.SUCCESS;

            mLock.lock();
            try {
                if (mState == State.INITIALIZED) {
                    throw new UnsupportedOperationException(this + " is not started!");
                }
                if (mState != State.STARTED) {
                    throw new UnsupportedOperationException(this + " is stopped!");
                }

                if (mDirectCall.tryAcquire()) {
                    try {
                        mActor.onMessage(message);
                    } catch (final Throwable error) {
                        Log.e(TAG, this + " failed to receive message " + message + "! Stopping", error); // NON-NLS
                        if (!stop(true)) {
                            Log.e(TAG, this + " couldn't be stopped after the failure! Ignoring the failure"); // NON-NLS
                        }
                        result = Delivery.ERROR;
                    } finally {
                        mDirectCall.release();
                    }
                } else {
                    result = Delivery.FAILURE;
                }
            } finally {
                mLock.unlock();
            }

            if ((result == Delivery.SUCCESS) && Log.isLoggable(TAG, DEBUG)) {
                Log.d(TAG, this + " handled user message " + message); //NON-NLS
            }

            return result;
        }

        public final boolean start(@NonNull final Context context,
                                   @NonNull final Reference<M> reference) {
            boolean success = false;

            mLock.lock();
            try {
                if (mState == State.STARTED) {
                    success = true;
                } else {
                    if (mState != State.INITIALIZED) {
                        throw new UnsupportedOperationException(this + " is stopped!");
                    }

                    try {
                        mActor.postStart(context, reference);
                        success = true;
                    } catch (final Throwable error) {
                        Log.e(TAG, this + " failed during start!", error); // NON-NLS
                    }
                }
            } finally {
                mLock.unlock();
            }

            if (success && Log.isLoggable(TAG, INFO)) {
                Log.i(TAG, this + " started"); //NON-NLS
            }

            return success;
        }

        @Override
        public final boolean stop(final boolean immediately) {
            @State final int previous;
            boolean success = false;

            mLock.lock();
            try {
                previous = mState;
                switch (mState) {
                    case State.STARTED:
                        try {
                            mActor.preStop();
                            success = true;
                        } catch (final Throwable error) {
                            Log.e(TAG, this + " failed during stop!", error); // NON-NLS
                        }
                        break;
                    case State.INITIALIZED:
                    case State.STOPPED:
                        success = true;
                        break;
                    default:
                        throw new IllegalStateException("Unknown reference state: " + mState);
                }
                if (success) {
                    mState = State.STOPPED;
                }
            } finally {
                mLock.unlock();
            }

            if ((previous != State.STOPPED) && success && Log.isLoggable(TAG, INFO)) {
                Log.i(TAG, this + " stopped"); //NON-NLS
            }

            return success;
        }

        @NonNls
        @NonNull
        @Override
        public final String toString() {
            return "Actor(" + mName + ')';
        }

        @Retention(SOURCE)
        @IntDef({State.INITIALIZED, State.STARTED, State.STOPPED})
        private @interface State {
            int INITIALIZED = 1;
            int STARTED = 2;
            int STOPPED = 3;
        }
    }
}
