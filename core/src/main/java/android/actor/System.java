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

import android.actor.channel.Mailbox;
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

import static android.util.Log.INFO;
import static java.lang.annotation.RetentionPolicy.SOURCE;

@ThreadSafe
public class System implements Actor.Repository {

    private static final String TAG = System.class.getSimpleName();

    @NonNls
    private static final String UNKNOWN_STATE = "Unknown state: ";
    @NonNls
    public static final String STOPPED = "Actor system is stopped";

    @NonNull
    private final Executor mExecutor;

    @GuardedBy("mLock")
    private final Map<Actor.Name, Entry<?>> mEntries = new HashMap<>();
    private final Lock mLock = new ReentrantLock();

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
                    for (final Entry<?> entry : mEntries.values()) {
                        // TODO don't start the actors if it's paused
                        if (!entry.start(mExecutor)) {
                            Log.w(TAG, entry + " failed to start"); //NON-NLS
                            success = false;
                        }
                    }
                    break;
                case State.STARTED:
                    /* do nothing */
                    break;
                case State.STOPPED:
                    throw new UnsupportedOperationException(STOPPED);
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
                    for (final Entry<?> entry : mEntries.values()) {
                        if (!entry.pause()) {
                            Log.w(TAG, entry + " failed to pause"); //NON-NLS
                            success = false;
                        }
                    }
                    break;
                case State.PAUSED:
                    /* do nothing */
                    break;
                case State.STOPPED:
                    throw new UnsupportedOperationException(STOPPED);
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
                    for (final Entry<?> entry : mEntries.values()) {
                        if (!entry.stop(immediately)) {
                            Log.w(TAG, entry + " failed to stop"); //NON-NLS
                            success = false;
                        }
                    }
                    mEntries.clear();
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

    public final boolean start(@NonNull final Actor.Name name) {
        boolean success = true;

        mLock.lock();
        try {
            if (mEntries.containsKey(name)) {
                // TODO don't start the actor if system is paused
                success = mEntries.get(name).start(mExecutor);
                if (success) {
                    // TODO speed up by having the parent actor know its children
                    for (final Map.Entry<Actor.Name, Entry<?>> other : mEntries.entrySet()) {
                        if (name.isParentOf(other.getKey())) {
                            success = success && other.getValue().start(mExecutor);
                        }
                    }
                }
            }
        } finally {
            mLock.unlock();
        }

        return success;
    }

    public final boolean pause(@NonNull final Actor.Name name) {
        boolean success = true;

        mLock.lock();
        try {
            if (mEntries.containsKey(name)) {
                success = mEntries.get(name).pause();
                if (success) {
                    // TODO speed up by having the parent actor know its children
                    for (final Map.Entry<Actor.Name, Entry<?>> other : mEntries.entrySet()) {
                        if (name.isParentOf(other.getKey())) {
                            success = success && other.getValue().pause();
                        }
                    }
                }
            }
        } finally {
            mLock.unlock();
        }

        return success;
    }

    public final boolean stop(@NonNull final Actor.Name name) {
        boolean success = true;

        mLock.lock();
        try {
            if (mEntries.containsKey(name)) {
                success = mEntries.remove(name).stop(false);
                if (success) {
                    // TODO speed up by having the parent actor know its children
                    for (final Map.Entry<Actor.Name, Entry<?>> other : mEntries.entrySet()) {
                        if (name.isParentOf(other.getKey())) {
                            success = success && other.getValue().stop(true);
                        }
                    }
                }
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
        mLock.lock();
        try {
            return register(new Actor.Name(null, name), actor);
        } finally {
            mLock.unlock();
        }
    }

    @NonNull
    @GuardedBy("mLock")
    private <M> Reference<M> register(@NonNull final Actor.Name name,
                                      @NonNull final Actor<M> actor) {
        if (mState == State.STOPPED) {
            throw new UnsupportedOperationException(STOPPED);
        }
        if (mEntries.containsKey(name)) {
            throw new IllegalArgumentException(name + " is already registered!");
        }

        final Entry<M> entry = new Entry<>(this, name, actor);

        if (mState == State.STARTED) {
            if (entry.start(mExecutor)) {
                mEntries.put(name, entry);
            } else {
                throw new UnsupportedOperationException(entry + " could not be started!");
            }
        } else {
            mEntries.put(name, entry);
        }

        return entry.getReference();
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
    private static final class Entry<M> implements Context {

        @NonNull
        private final System mSystem;
        @NonNull
        private final Actor.Name mName;

        @NonNull
        private final Actor.Channel<M> mChannel;
        @NonNull
        private final Mailbox<M> mMailbox;
        @NonNull
        private final Mailbox.Runner mRunner;
        @NonNull
        private final Reference<M> mReference;

        private Entry(@NonNull final System system,
                      @NonNull final Actor.Name name,
                      @NonNull final Actor<M> actor) {
            super();

            mSystem = system;
            mName = name;
            mChannel = new Actor.Channel<>(name, actor);
            mMailbox = new Mailbox<>(mChannel);
            mRunner = new Mailbox.Runner(mMailbox);
            // TODO use factory
            mReference = new Reference<>(system, name, mMailbox);
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
        public Reference<M> getReference() {
            return mReference;
        }

        public boolean start(@NonNull final Executor executor) {
            return mChannel.start(this, mReference) && mRunner.start(executor);
        }

        public boolean pause() {
            final boolean success = mRunner.stop();
            if (success && Log.isLoggable(TAG, INFO)) {
                Log.i(TAG, this + " paused"); //NON-NLS
            }
            return success;
        }

        public boolean stop(final boolean immediately) {
            return mMailbox.stop(immediately) && mRunner.stop();
        }

        @NonNull
        @Override
        public <T> Reference<T> register(@NonNls @NonNull final String name,
                                         @NonNull final Actor<T> actor) {
            return mSystem.register(new Actor.Name(mName, name), actor);
        }

        @NonNls
        @NonNull
        @Override
        public String toString() {
            return mChannel.toString();
        }
    }
}
