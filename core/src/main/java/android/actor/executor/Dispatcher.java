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

import org.jetbrains.annotations.NonNls;

import java.util.concurrent.atomic.AtomicReference;

public class Dispatcher {

    @NonNull
    private final Loop mLoop;
    @NonNull
    private final Manager mManager;

    public Dispatcher() {
        super();

        mManager = new Manager();
        mLoop = new Loop(mManager);
    }

    public final void start() {
        new Thread(mLoop).start();
    }

    public final void stop() {
        mLoop.stop();
    }

    public final boolean isEmpty() {
        return mManager.isEmpty();
    }

    public final int size() {
        return mManager.size();
    }

    @Nullable
    public final Executor.Submission submit(@NonNls @NonNull final Executable executable) {
        return mManager.submit(executable);
    }

    private static class Loop implements Runnable {

        @NonNull
        private final Manager mManager;

        private final AtomicReference<Looper> mLooper = new AtomicReference<>();

        private Loop(@NonNull final Manager manager) {
            super();

            mManager = manager;
        }

        @Override
        public final void run() {
            Looper.prepare();

            final Looper current = Looper.myLooper();
            if (mLooper.compareAndSet(null, current)) {
                try {
                    if (mManager.start(current)) {
                        Looper.loop();
                    }
                } finally {
                    mManager.stop();
                    mLooper.set(null);
                }
            } else {
                current.quit();
            }
        }

        public final void stop() {
            final Looper current = mLooper.getAndSet(null);
            if (current != null) {
                current.quit();
            }
        }
    }
}
