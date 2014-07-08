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

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.jetbrains.annotations.NonNls;

import java.lang.annotation.Retention;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.annotation.RetentionPolicy.SOURCE;

public class System {

    public static final System OnMainThread = new System(Executors.mainThread());

    private static final Executor SINGLE_THREAD = Executors.singleThread();
    @NonNls
    private static final String UNKNOWN_STATE = "Unknown state: ";
    @NonNls
    public static final String SYSTEM_STOPPED = "Actor system is stopped";

    private final Executor mExecutor;

    private final Lock mLock = new ReentrantLock();
    private final Map<String, Registration<?>> mRegistrations = new HashMap<>();
    @State
    private int mState = State.STARTED;

    public System() {
        this(SINGLE_THREAD);
    }

    public System(@NonNull final Executor executor) {
        super();

        mExecutor = executor;
    }

    public final boolean isStarted() {
        final boolean result;

        mLock.lock();
        try {
            result = mState == State.STARTED;
        } finally {
            mLock.unlock();
        }

        return result;
    }

    public final boolean isPaused() {
        final boolean result;

        mLock.lock();
        try {
            result = mState == State.PAUSED;
        } finally {
            mLock.unlock();
        }

        return result;
    }

    public final boolean isStopped() {
        final boolean result;

        mLock.lock();
        try {
            result = mState == State.STOPPED;
        } finally {
            mLock.unlock();
        }

        return result;
    }

    public final boolean start() {
        boolean success = true;

        mLock.lock();
        try {
            switch (mState) {
                case State.PAUSED:
                    mState = State.STARTED;
                    for (final Registration<?> registration : mRegistrations.values()) {
                        success = registration.start(mExecutor) && success;
                    }
                    break;
                case State.STARTED:
                    /* do nothing */
                    break;
                case State.STOPPED:
                    throw new UnsupportedOperationException(SYSTEM_STOPPED);
                default:
                    throw new UnsupportedOperationException(UNKNOWN_STATE + mState);
            }
        } finally {
            mLock.unlock();
        }

        return success;
    }

    public final boolean pause() {
        boolean success = true;

        mLock.lock();
        try {
            switch (mState) {
                case State.STARTED:
                    mState = State.PAUSED;
                    for (final Registration<?> registration : mRegistrations.values()) {
                        success = registration.pause() && success;
                    }
                    break;
                case State.PAUSED:
                    /* do nothing */
                    break;
                case State.STOPPED:
                    throw new UnsupportedOperationException(SYSTEM_STOPPED);
                default:
                    throw new UnsupportedOperationException(UNKNOWN_STATE + mState);
            }
        } finally {
            mLock.unlock();
        }

        return success;
    }

    public final boolean stop(final boolean immediately) {
        boolean success = true;

        mLock.lock();
        try {
            switch (mState) {
                case State.STARTED:
                case State.PAUSED:
                    mState = State.STOPPED;
                    for (final Registration<?> registration : mRegistrations.values()) {
                        success = registration.stop(immediately) && success;
                    }
                    mRegistrations.clear();
                    break;
                case State.STOPPED:
                    /* do nothing */
                    break;
                default:
                    throw new UnsupportedOperationException(UNKNOWN_STATE + mState);
            }
        } finally {
            mLock.unlock();
        }

        return success;
    }

    @NonNull
    public final <M> Reference<M> with(@NonNls @NonNull final String name,
                                       @NonNull final Actor<M> actor) {
        final Reference<M> reference = new Reference<>(name);

        mLock.lock();
        try {
            if (mState == State.STOPPED) {
                throw new UnsupportedOperationException(SYSTEM_STOPPED);
            }
            if (mRegistrations.containsKey(name)) {
                throw new IllegalArgumentException("Actor " + name + " is already registered!");
            }

            final Registration<M> registration = new Registration<>(this, reference, actor);
            if (mState == State.STARTED) {
                if (registration.start(mExecutor)) {
                    mRegistrations.put(name, registration);
                } else {
                    throw new UnsupportedOperationException("Actor could not be started!");
                }
            } else {
                mRegistrations.put(name, registration);
            }
        } finally {
            mLock.unlock();
        }

        return reference;
    }

    protected final <M> boolean onStop(@NonNull final Registration<M> registration) {
        final boolean success;

        mLock.lock();
        try {
            final String name = registration.getName();
            success = !mRegistrations.containsKey(name) || mRegistrations.remove(name).stop(true);
        } finally {
            mLock.unlock();
        }

        return success;
    }

    @Retention(SOURCE)
    @IntDef({State.STARTED, State.PAUSED, State.STOPPED})
    private @interface State {
        int STARTED = 1;
        int PAUSED = 2;
        int STOPPED = 3;
    }

    private static class Registration<M> implements Reference.DirectCall<M>, Handler.Callback {

        @NonNull
        private final System mSystem;
        @NonNull
        private final Reference<M> mReference;
        @NonNull
        private final Actor<M> mActor;

        private final Executor.Task mTask = new Executor.Task() {

            @Override
            public boolean attach(@NonNull final Looper looper) {
                return mReference.onAttach(new Handler(looper, Registration.this));
            }

            @Override
            public boolean detach() {
                final boolean result = mReference.onDetach();
                mSubmission = null;
                return result;
            }
        };

        private boolean mCanDirectCall = true;
        @Nullable
        private Executor.Submission mSubmission;

        private Registration(@NonNull final System system,
                             @NonNull final Reference<M> reference,
                             @NonNull final Actor<M> actor) {
            super();

            mSystem = system;
            mReference = reference;
            mActor = actor;

            reference.setDirectCall(this);
            mActor.postStart(mSystem, mReference);
        }

        @NonNls
        @NonNull
        public final String getName() {
            return mReference.getName();
        }

        public final boolean start(@NonNull final Executor executor) {
            final boolean stopped = (mSubmission == null) || mSubmission.stop();
            if (stopped) {
                mSubmission = executor.submit(mTask);
            }
            return stopped && (mSubmission != null);
        }

        public final boolean pause() {
            return mReference.pause();
        }

        public final boolean stop(final boolean immediately) {
            final boolean success;

            if (immediately) {
                mActor.preStop();
                mReference.onStop();
                if (mSubmission == null) {
                    success = true;
                } else {
                    success = mSubmission.stop();
                    mSubmission = null;
                }
            } else {
                success = mReference.stop();
            }

            return success;
        }

        @Override
        public final boolean handleSystemMessage(final int message) {
            final boolean processed;

            switch (message) {
                case Reference.PAUSE:
                    processed = (mSubmission == null) || mSubmission.stop();
                    mSubmission = null;
                    break;
                case Reference.STOP:
                    processed = mSystem.onStop(this);
                    break;
                default:
                    processed = false;
            }

            return processed;
        }

        @Override
        public final boolean handleUserMessage(@NonNull final M message) {
            final boolean success = mCanDirectCall;

            if (success) {
                mCanDirectCall = false;
                mActor.onMessage(message);
                mCanDirectCall = true;
            }

            return success;
        }

        @Override
        @SuppressWarnings("unchecked")
        public final boolean handleMessage(@NonNull final Message message) {
            final boolean processed;

            switch (message.what) {
                case Reference.SYSTEM_MESSAGE:
                    processed = handleSystemMessage(message.arg1);
                    break;
                case Reference.USER_MESSAGE:
                    processed = handleUserMessage((M) message.obj);
                    break;
                default:
                    processed = false;
            }

            if (processed) {
                message.recycle();
            }

            return processed;
        }

        @Override
        public final int hashCode() {
            return mReference.hashCode();
        }

        @Override
        public final boolean equals(@Nullable final Object other) {
            boolean result = this == other;

            if (!result && (other instanceof Registration<?>)) {
                result = mReference.equals(((Registration<?>) other).mReference);
            }

            return result;
        }
    }
}
