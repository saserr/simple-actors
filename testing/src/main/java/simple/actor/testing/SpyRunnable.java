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

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A {@link Runnable} that remembers how many times it has been executed. This {@code Runnable}
 * should only be used for testing.
 */
public final class SpyRunnable implements Runnable {

    private final AtomicInteger mExecutedTimes = new AtomicInteger(0 /* initialValue */);

    @Override
    public void run() {
        mExecutedTimes.incrementAndGet();
    }

    /** Returns how many times {@link Runnable} has executed. */
    public int getExecutedTimes() {
        return mExecutedTimes.get();
    }
}
