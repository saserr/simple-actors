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

import android.actor.Channel;
import android.actor.Executor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

import org.jetbrains.annotations.NonNls;

import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static android.util.Log.DEBUG;
import static android.util.Log.INFO;

@ThreadSafe
public class SimpleExecutor implements Executor {

    private static final String TAG = SimpleExecutor.class.getSimpleName();

    // TODO use open linked list with O(1) prepend and remove instead of HashSet
    @GuardedBy("mLock")
    private final Collection<Executable> mExecutables = new HashSet<>();
    private final Lock mLock = new ReentrantLock();

    @Nullable
    @GuardedBy("mLock")
    private Channel.Factory mFactory;
    @GuardedBy("mLock")
    private boolean mStopped = false;

    public SimpleExecutor() {
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

    public final boolean start(@NonNull final Channel.Factory factory) {
        boolean success = true;

        mLock.lock();
        try {
            if (mStopped) {
                throw new UnsupportedOperationException(Executor.STOPPED);
            }

            mFactory = factory;
            for (final Executable executable : mExecutables) {
                if (!executable.attach(mFactory)) {
                    Log.w(TAG, executable + " failed to attach"); //NON-NLS
                    success = false;
                }
            }
        } finally {
            mLock.unlock();
        }

        return success;
    }

    @Override
    public final void stop() {
        mLock.lock();
        try {
            if (!mStopped) {
                if (mFactory != null) {
                    mFactory = null;
                    for (final Executable executable : mExecutables) {
                        if (!executable.detach()) {
                            Log.w(TAG, executable + " failed to detach"); //NON-NLS
                        }
                    }
                }
                mStopped = true;
            }
        } finally {
            mLock.unlock();
        }
    }

    @Nullable
    @Override
    public final Executor.Submission submit(@NonNls @NonNull final Executable executable) {
        @org.jetbrains.annotations.Nullable final Executor.Submission submission;

        mLock.lock();
        try {
            if (mStopped) {
                throw new UnsupportedOperationException(Executor.STOPPED);
            }

            if (mExecutables.add(executable)) {
                if ((mFactory == null) || executable.attach(mFactory)) {
                    if (Log.isLoggable(TAG, DEBUG)) {
                        Log.d(TAG, executable + " submitted"); //NON-NLS
                    }

                    submission = new Executor.Submission() {
                        @Override
                        public boolean stop() {
                            final boolean success;

                            mLock.lock();
                            try {
                                if (mExecutables.contains(executable)) {
                                    success = (mFactory == null) || executable.detach();
                                    if (success) {
                                        mExecutables.remove(executable);
                                    } else {
                                        Log.w(TAG, "Submission for " + executable + " failed to be stopped"); //NON-NLS
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
                    Log.w(TAG, executable + " failed to be attached"); //NON-NLS
                    mExecutables.remove(executable);
                    submission = null;
                }
            } else {
                if (Log.isLoggable(TAG, INFO)) {
                    Log.i(TAG, executable + " already submitted"); //NON-NLS
                }
                submission = null;
            }
        } finally {
            mLock.unlock();
        }

        return submission;
    }
}
