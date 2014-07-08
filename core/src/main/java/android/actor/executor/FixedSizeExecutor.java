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

package android.actor.executor;

import android.actor.Executor;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class FixedSizeExecutor implements Executor {

    @NonNull
    private final List<Manager> mManagers;

    private final Lock mLock = new ReentrantLock();

    private boolean mStopped = false;

    public FixedSizeExecutor(final int size) {
        super();

        if (size < 1) {
            throw new IllegalArgumentException("Size must be grater than zero!");
        }

        mManagers = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            mManagers.add(new Manager());
        }
    }

    @Nullable
    @Override
    public final Submission submit(@NonNull final Task task) {
        Manager best;

        mLock.lock();
        try {
            if (mStopped) {
                throw new UnsupportedOperationException("Executor is stopped!");
            }

            best = mManagers.get(0);
            final int size = mManagers.size();
            for (int i = 1; (i < size) && !best.isEmpty(); i++) {
                final Manager manager = mManagers.get(i);
                if (manager.size() < best.size()) {
                    best = manager;
                }
            }
        } finally {
            mLock.unlock();
        }

        return best.submit(task);
    }

    @Override
    public final boolean stop() {
        mLock.lock();
        try {
            for (final Manager manager : mManagers) {
                manager.stop();
            }
            mManagers.clear();
            mStopped = true;
        } finally {
            mLock.unlock();
        }

        return true;
    }

    public class Manager implements Dispatcher.Callback {

        @NonNull
        private final Dispatcher mDispatcher;

        private final Lock mLock = new ReentrantLock();
        private final Collection<Task> mTasks = new HashSet<>();

        @Nullable
        private Looper mLooper;

        public Manager() {
            super();

            mDispatcher = new Dispatcher(this);
            mDispatcher.start();
        }

        public final boolean isEmpty() {
            final boolean result;

            mLock.lock();
            try {
                result = mTasks.isEmpty();
            } finally {
                mLock.unlock();
            }

            return result;
        }

        public final int size() {
            final int result;

            mLock.lock();
            try {
                result = mTasks.size();
            } finally {
                mLock.unlock();
            }

            return result;
        }

        public final void stop() {
            mDispatcher.stop();
        }

        @Override
        public final void onStart(@NonNull final Looper looper) {
            mLock.lock();
            try {
                mLooper = looper;
                for (final Executor.Task task : mTasks) {
                    if (!task.attach(looper)) {
                        mTasks.remove(task);
                    }
                }
            } finally {
                mLock.unlock();
            }
        }

        @Override
        public final void onStop() {
            mLock.lock();
            try {
                mLooper = null;
                for (final Executor.Task task : mTasks) {
                    task.stop();
                }
                mTasks.clear();
            } finally {
                mLock.unlock();
            }
        }

        @Nullable
        public final Executor.Submission submit(@NonNull final Executor.Task task) {
            @org.jetbrains.annotations.Nullable final Executor.Submission submission;

            mLock.lock();
            try {
                if (mTasks.add(task)) {
                    if ((mLooper == null) || task.attach(mLooper)) {
                        submission = new Executor.Submission() {
                            @Override
                            public boolean stop() {
                                final boolean success;

                                mLock.lock();
                                try {
                                    if (mTasks.remove(task)) {
                                        success = (mLooper == null) || task.detach();
                                        if (!success) {
                                            mTasks.add(task);
                                        }
                                    } else {
                                        success = true;
                                    }
                                } finally {
                                    mLock.unlock();
                                }

                                return success;
                            }
                        };
                    } else {
                        mTasks.remove(task);
                        submission = null;
                    }
                } else {
                    submission = null;
                }
            } finally {
                mLock.unlock();
            }

            return submission;
        }
    }
}
