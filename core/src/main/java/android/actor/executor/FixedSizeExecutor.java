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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static android.actor.executor.SimpleExecutor.EXECUTOR_STOPPED;

@ThreadSafe
public class FixedSizeExecutor implements Executor {

    @NonNull
    private final List<Dispatcher> mDispatchers;

    private final Lock mLock = new ReentrantLock();

    @GuardedBy("mLock")
    private boolean mStopped = false;

    public FixedSizeExecutor(final int size) {
        super();

        if (size < 1) {
            throw new IllegalArgumentException("Size must be grater than zero");
        }

        mDispatchers = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            final Dispatcher dispatcher = new Dispatcher();
            dispatcher.start();
            mDispatchers.add(dispatcher);
        }
    }

    @Nullable
    @Override
    public final Submission submit(@NonNull final Executable executable) {
        Dispatcher best;

        mLock.lock();
        try {
            if (mStopped) {
                throw new UnsupportedOperationException(EXECUTOR_STOPPED);
            }

            best = mDispatchers.get(0);
            final int size = mDispatchers.size();
            for (int i = 1; (i < size) && !best.isEmpty(); i++) {
                final Dispatcher dispatcher = mDispatchers.get(i);
                if (dispatcher.size() < best.size()) {
                    best = dispatcher;
                }
            }
        } finally {
            mLock.unlock();
        }

        return best.submit(executable);
    }

    @Override
    public final void stop() {
        mLock.lock();
        try {
            if (!mStopped) {
                for (final Dispatcher dispatcher : mDispatchers) {
                    dispatcher.stop();
                }
                mDispatchers.clear();
                mStopped = true;
            }
        } finally {
            mLock.unlock();
        }
    }
}
