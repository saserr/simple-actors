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
    private final Collection<Executable> mExecutables = new HashSet<>();

    @Nullable
    private Looper mLooper;

    public Manager() {
        super();
    }

    public final boolean isEmpty() {
        final boolean result;

        mLock.lock();
        try {
            result = mExecutables.isEmpty();
        } finally {
            mLock.unlock();
        }

        return result;
    }

    public final int size() {
        final int result;

        mLock.lock();
        try {
            result = mExecutables.size();
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
            for (final Executable executable : mExecutables) {
                success = executable.attach(looper) && success;
                if (!success) {
                    mExecutables.remove(executable);
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
            if (mLooper != null) {
                mLooper = null;
                for (final Executable executable : mExecutables) {
                    success = executable.detach() && success;
                }
            }
        } finally {
            mLock.unlock();
        }

        return success;
    }

    @Nullable
    public final Executor.Submission submit(@NonNull final Executable executable) {
        @org.jetbrains.annotations.Nullable final Executor.Submission submission;

        mLock.lock();
        try {
            if (mExecutables.add(executable)) {
                if ((mLooper == null) || executable.attach(mLooper)) {
                    submission = new Executor.Submission() {
                        @Override
                        public boolean stop() {
                            final boolean success;

                            mLock.lock();
                            try {
                                if (mExecutables.remove(executable)) {
                                    success = (mLooper == null) || executable.detach();
                                    if (!success) {
                                        mExecutables.add(executable);
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
                    mExecutables.remove(executable);
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
