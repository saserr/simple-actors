/*
 * Copyright 2017 Sanjin Sehic
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

package simple.actor.testing;

import java.util.concurrent.atomic.AtomicBoolean;

import simple.actor.Channel;
import simple.actor.Runner;

/**
 * A {@link Runner} that will return {@link Channel Channels} that will execute sent {@link Runnable
 * Runnables} on the calling thread. This {@code Runner} should only be used for testing.
 */
public final class SameThreadRunner implements Runner {

    /**
     * Creates a {@link Channel} that will execute sent {@link Runnable Runnables} on the calling
     * thread until stopped.
     */
    @Override
    public Channel<Runnable> create() {
        return new SameThreadExecutor();
    }

    /**
     * A {@link Channel} that executes sent {@link Runnable Runnables} on the calling thread until
     * stopped. This {@link Channel} should only be used for testing.
     */
    private static final class SameThreadExecutor implements Channel<Runnable> {

        private final AtomicBoolean mStopped = new AtomicBoolean(false /*initial value*/);

        /**
         * Executes the given {@link Runnable} on the calling thread unless this {@link Channel} was
         * previously {@link #stop stopped}.
         */
        @Override
        public boolean send(final Runnable command) {
            if (mStopped.get()) {
                return false;
            }

            command.run();
            return true;
        }

        @Override
        public void stop() {
            mStopped.set(true);
        }
    }
}
