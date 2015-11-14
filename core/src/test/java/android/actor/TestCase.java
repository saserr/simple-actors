/*
 * Copyright 2015 the original author or authors
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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.testng.annotations.AfterMethod;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import mockit.FullVerifications;
import mockit.Mocked;
import mockit.Verifications;

public abstract class TestCase {

    @Mocked
    private Log mMockedLog;

    private final Random mRandom = new SecureRandom();

    @AfterMethod
    public final void verifyCorrectLogging() {
        new Verifications() {{
            Log.d(anyString, anyString);
            times = 0;
            Log.d(anyString, anyString, (Throwable) any);
            times = 0;

            Log.i(anyString, anyString);
            times = 0;
            Log.i(anyString, anyString, (Throwable) any);
            times = 0;

            Log.v(anyString, anyString);
            times = 0;
            Log.v(anyString, anyString, (Throwable) any);
            times = 0;
        }};
    }

    @NonNull
    protected final <V> V notNull(@Nullable final V value) {
        assert value != null;
        return value;
    }

    @NonNull
    protected final <V> V a(@NonNull final RandomDataGenerator<V> generator) {
        return generator.next(mRandom);
    }

    @NonNull
    protected final <V> V isA(@NonNull final RandomDataGenerator<V> generator) {
        return a(generator);
    }

    protected static void verifyNoMoreInteractions(@NonNull final Object... mocks) {
        new FullVerifications(mocks) {
        };
    }

    protected abstract static class RandomDataGenerator<V> {

        @NonNull
        public abstract V next(@NonNull final Random random);

        public final RandomDataGenerator<V> thatIs(@NonNull final Filter<? super V> filter) {
            return new Filtered<>(this, filter);
        }

        private static final class Filtered<V> extends RandomDataGenerator<V> {

            @NonNull
            private final RandomDataGenerator<V> mOriginal;
            @NonNull
            private final Filter<? super V> mFilter;

            private Filtered(@NonNull final RandomDataGenerator<V> original,
                             @NonNull final Filter<? super V> filter) {
                super();

                mOriginal = original;
                mFilter = filter;
            }

            @NonNull
            @Override
            public V next(@NonNull final Random random) {
                V result;
                do {
                    result = mOriginal.next(random);
                } while (!mFilter.isValid(result));
                return result;
            }
        }
    }

    protected static final RandomDataGenerator<Integer> RandomInteger =
            new RandomDataGenerator<Integer>() {
                @NonNull
                @Override
                public Integer next(@NonNull final Random random) {
                    return random.nextInt();
                }
            };

    protected static final RandomDataGenerator<Long> RandomLong =
            new RandomDataGenerator<Long>() {
                @NonNull
                @Override
                public Long next(@NonNull final Random random) {
                    return random.nextLong();
                }
            };

    protected static final RandomDataGenerator<String> RandomString =
            new RandomDataGenerator<String>() {
                @NonNull
                @Override
                public String next(@NonNull final Random random) {
                    return Long.toHexString(random.nextLong());
                }
            };

    protected interface Filter<V> {
        boolean isValid(@NonNull final V v);
    }

    protected static final Filter<Number> Positive = new Filter<Number>() {
        @Override
        public boolean isValid(@NonNull final Number number) {
            return number.longValue() > 0;
        }
    };

    protected static final Filter<Number> NonPositive = new Filter<Number>() {
        @Override
        public boolean isValid(@NonNull final Number number) {
            return number.longValue() <= 0;
        }
    };

    protected static Filter<Number> differentFrom(@NonNull final Number... numbers) {
        return new DifferentFrom(numbers);
    }

    private static final class DifferentFrom implements Filter<Number> {

        private final Set<Long> mNumbers;

        private DifferentFrom(@NonNull final Number... numbers) {
            super();

            mNumbers = new HashSet<>(numbers.length);
            for (final Number number : numbers) {
                mNumbers.add(number.longValue());
            }
        }

        @Override
        public boolean isValid(@NonNull final Number number) {
            return !mNumbers.contains(number.longValue());
        }
    }
}

