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

package android.actor;

import org.jetbrains.annotations.NotNull;

import java.security.SecureRandom;
import java.util.Random;

public abstract class TestCase extends junit.framework.TestCase {

    private final Random mRandom = new SecureRandom();

    @NotNull
    protected final <V> V a(@NotNull final RandomDataGenerator<V> generator) {
        return generator.next(mRandom);
    }

    @NotNull
    protected final <V> V isA(@NotNull final RandomDataGenerator<V> generator) {
        return a(generator);
    }

    protected interface RandomDataGenerator<V> {
        @NotNull
        V next(@NotNull final Random random);
    }

    protected static final RandomDataGenerator<Integer> RandomInteger = new RandomDataGenerator<Integer>() {
        @NotNull
        @Override
        public Integer next(@NotNull final Random random) {
            return random.nextInt();
        }
    };

    protected static final RandomDataGenerator<String> RandomString = new RandomDataGenerator<String>() {
        @NotNull
        @Override
        public String next(@NotNull final Random random) {
            return Long.toHexString(random.nextLong());
        }
    };
}

