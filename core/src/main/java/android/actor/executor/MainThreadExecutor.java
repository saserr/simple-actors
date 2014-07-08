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

import static android.os.Looper.getMainLooper;

public class MainThreadExecutor implements Executor {

    private static final Looper MAIN_THREAD = getMainLooper();

    private final Lock mLock = new ReentrantLock();
    @NonNull
    private final Collection<Task> mTasks = new HashSet<>();

    private boolean mStopped = false;

    public MainThreadExecutor() {
        super();
    }

    @Nullable
    @Override
    public final Executor.Submission submit(@NonNull final Task task) {
        @org.jetbrains.annotations.Nullable final Executor.Submission submission;

        mLock.lock();
        try {
            if (mStopped) {
                throw new UnsupportedOperationException("Executor is stopped!");
            }

            if (mTasks.add(task)) {
                if (task.attach(MAIN_THREAD)) {
                    submission = new Executor.Submission() {
                        @Override
                        public boolean stop() {
                            final boolean success;

                            mLock.lock();
                            try {
                                success = !mTasks.remove(task) || task.detach();
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

    @Override
    public final void stop() {
        mLock.lock();
        try {
            for (final Task task : mTasks) {
                task.stop();
            }
            mTasks.clear();
            mStopped = true;
        } finally {
            mLock.unlock();
        }
    }
}
