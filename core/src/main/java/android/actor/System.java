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

import org.jetbrains.annotations.NonNls;

import java.lang.annotation.Retention;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.annotation.RetentionPolicy.SOURCE;

public class System {

    public static final System OnMainThread = new System(Executors.mainThread());

    @NonNls
    private static final String UNKNOWN_STATE = "Unknown state: ";
    @NonNls
    public static final String SYSTEM_STOPPED = "Actor system is stopped";

    private final Executor mExecutor;

    private final Lock mLock = new ReentrantLock();
    private final Map<String, Reference<?>> mReferences = new HashMap<>();
    @State
    private int mState = State.STARTED;

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
                    for (final Reference<?> reference : mReferences.values()) {
                        success = reference.start(mExecutor) && success;
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
                    for (final Reference<?> reference : mReferences.values()) {
                        success = reference.pause() && success;
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
                    for (final Reference<?> reference : mReferences.values()) {
                        success = reference.stop(immediately) && success;
                    }
                    mReferences.clear();
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
        final Reference<M> reference = new Reference<>(this, name, actor);

        mLock.lock();
        try {
            if (mState == State.STOPPED) {
                throw new UnsupportedOperationException(SYSTEM_STOPPED);
            }
            if (mReferences.containsKey(name)) {
                throw new IllegalArgumentException(reference + " is already registered!");
            }

            if (mState == State.STARTED) {
                if (reference.start(mExecutor)) {
                    mReferences.put(name, reference);
                } else {
                    throw new UnsupportedOperationException(reference + " could not be started!");
                }
            } else {
                mReferences.put(name, reference);
            }
        } finally {
            mLock.unlock();
        }

        return reference;
    }

    protected final <M> void onStop(@NonNull final Reference<M> reference) {
        mLock.lock();
        try {
            mReferences.remove(reference.getName());
        } finally {
            mLock.unlock();
        }
    }

    @Retention(SOURCE)
    @IntDef({State.STARTED, State.PAUSED, State.STOPPED})
    private @interface State {
        int STARTED = 1;
        int PAUSED = 2;
        int STOPPED = 3;
    }
}
