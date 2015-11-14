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
import android.util.Log;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

import org.jetbrains.annotations.NonNls;

import java.lang.annotation.Retention;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static android.util.Log.DEBUG;
import static java.lang.annotation.RetentionPolicy.SOURCE;

@ThreadSafe
public class System implements Actor.Repository {

    private static final String TAG = System.class.getSimpleName();

    @NonNls
    private static final String UNKNOWN_STATE = "Unknown state: ";
    @NonNls
    public static final String SYSTEM_STOPPED = "Actor system is stopped";

    @NonNull
    private final Executor mExecutor;

    @GuardedBy("mLock")
    private final Map<Actor.Name, Reference<?>> mReferences = new HashMap<>();
    private final Lock mLock = new ReentrantLock();

    private final Reference.Callback mCallback = new Reference.Callback() {
        @Override
        public void onStop(@NonNull final Actor.Name name) {
            stop(name);
        }
    };

    @State
    @GuardedBy("mLock")
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
                        if (!reference.start(mExecutor)) {
                            Log.w(TAG, reference + " failed to start"); //NON-NLS
                            success = false;
                        }
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
                        if (!reference.pause()) {
                            Log.w(TAG, reference + " failed to pause"); //NON-NLS
                            success = false;
                        }
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
                        if (!reference.stop(immediately)) {
                            Log.w(TAG, reference + " failed to stop"); //NON-NLS
                            success = false;
                        }
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
    @Override
    public final <M> Reference<M> register(@NonNls @NonNull final String name,
                                           @NonNull final Actor<M> actor) {
        return register(new Actor.Name(null, name), actor);
    }

    @NonNull
    private <M> Reference<M> register(@NonNull final Actor.Name name, @NonNull final Actor<M> actor) {
        final Context context = new Context(name, this);
        final Reference<M> reference;

        mLock.lock();
        try {
            if (mState == State.STOPPED) {
                throw new UnsupportedOperationException(SYSTEM_STOPPED);
            }
            if (mReferences.containsKey(name)) {
                throw new IllegalArgumentException(name + " is already registered!");
            }

            // TODO use factory
            reference = new Reference<>(context, actor, mCallback);

            if (mState == State.STARTED) {
                if (reference.start(mExecutor)) {
                    mReferences.put(name, reference);
                } else {
                    throw new UnsupportedOperationException(reference + " could not be started!");
                }
            } else {
                mReferences.put(name, reference);
            }

            if (Log.isLoggable(TAG, DEBUG)) {
                Log.d(TAG, name + " added"); //NON-NLS
            }
        } finally {
            mLock.unlock();
        }

        return reference;
    }

    private void stop(@NonNull final Actor.Name name) {
        mLock.lock();
        try {
            if (mReferences.containsKey(name)) {
                mReferences.remove(name);
                if (Log.isLoggable(TAG, DEBUG)) {
                    Log.d(TAG, name + " removed"); //NON-NLS
                }

                for (final Map.Entry<Actor.Name, Reference<?>> other : mReferences.entrySet()) {
                    if (name.isParentOf(other.getKey())) {
                        other.getValue().stop(true);
                    }
                }
            }
        } finally {
            mLock.unlock();
        }
    }

    @NonNull
    public static System onMainThread() {
        return new System(Executors.mainThread());
    }

    @Retention(SOURCE)
    @IntDef({State.STARTED, State.PAUSED, State.STOPPED})
    private @interface State {
        int STARTED = 1;
        int PAUSED = 2;
        int STOPPED = 3;
    }

    @ThreadSafe
    private static final class Context implements android.actor.Context {

        @NonNull
        private final Actor.Name mName;
        @NonNull
        private final System mSystem;

        private Context(@NonNull final Actor.Name name, @NonNull final System system) {
            super();

            mName = name;
            mSystem = system;
        }

        @NonNull
        @Override
        public Actor.Name getName() {
            return mName;
        }

        @NonNull
        @Override
        public System getSystem() {
            return mSystem;
        }

        @NonNull
        @Override
        public <M> Reference<M> register(@NonNls @NonNull final String name,
                                         @NonNull final Actor<M> actor) {
            return mSystem.register(new Actor.Name(mName, name), actor);
        }
    }
}
