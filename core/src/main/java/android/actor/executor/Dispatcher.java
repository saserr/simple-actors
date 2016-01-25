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

import android.actor.Channels;
import android.actor.Executor;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.jcip.annotations.ThreadSafe;

import org.jetbrains.annotations.NonNls;

import java.util.concurrent.atomic.AtomicReference;

@ThreadSafe
public class Dispatcher {

    @NonNull
    private final Loop mLoop;
    @NonNull
    private final SimpleExecutor mExecutor;

    public Dispatcher() {
        super();

        mExecutor = new SimpleExecutor();
        mLoop = new Loop(mExecutor);
    }

    public final void start() {
        new Thread(mLoop).start();
    }

    public final void stop() {
        mLoop.stop();
    }

    public final boolean isEmpty() {
        return mExecutor.isEmpty();
    }

    public final int size() {
        return mExecutor.size();
    }

    @Nullable
    public final Executor.Submission submit(@NonNls @NonNull final Executable executable) {
        return mExecutor.submit(executable);
    }

    @ThreadSafe
    private static final class Loop implements Runnable {

        @NonNull
        private final SimpleExecutor mExecutor;

        private final AtomicReference<Looper> mLooper = new AtomicReference<>();

        private Loop(@NonNull final SimpleExecutor executor) {
            super();

            mExecutor = executor;
        }

        @Override
        public void run() {
            Looper.prepare();

            final Looper current = Looper.myLooper();
            if (current == null) {
                throw new IllegalStateException("No looper associated with this thread");
            }

            if (mLooper.compareAndSet(null, current) && mExecutor.start(Channels.from(current))) {
                try {
                    Looper.loop();
                } finally {
                    mExecutor.stop();
                    mLooper.set(null);
                }
            } else {
                current.quit();
            }
        }

        public void stop() {
            final Looper current = mLooper.getAndSet(null);
            if (current != null) {
                current.quit();
            }
        }
    }
}
