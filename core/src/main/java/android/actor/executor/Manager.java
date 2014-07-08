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

import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Manager implements Dispatcher.Callback {

    private final Lock mLock = new ReentrantLock();
    private final Collection<Executor.Task> mTasks = new HashSet<>();

    @Nullable
    private Looper mLooper;

    public Manager() {
        super();
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

    @Override
    public final boolean onStart(@NonNull final Looper looper) {
        boolean success = true;

        mLock.lock();
        try {
            mLooper = looper;
            for (final Executor.Task task : mTasks) {
                success = task.attach(looper) && success;
                if (!success) {
                    mTasks.remove(task);
                }
            }
        } finally {
            mLock.unlock();
        }

        return success;
    }

    @Override
    public final boolean onStop() {
        boolean success = true;

        mLock.lock();
        try {
            mLooper = null;
            for (final Executor.Task task : mTasks) {
                success = task.detach() && success;
            }
        } finally {
            mLock.unlock();
        }

        return success;
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
