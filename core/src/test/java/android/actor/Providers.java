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

import org.jetbrains.annotations.NonNls;
import org.testng.annotations.DataProvider;

import java.util.ArrayList;
import java.util.List;

import static java.lang.System.arraycopy;

public final class Providers {

    @NonNull
    public static Object[][] without(@NonNull final Object[][] values,
                                     @NonNull final Object[]... excludes) {
        Object[][] result = values;
        for (final Object[] excluded : excludes) {
            result = remove(result, excluded);
        }
        return result;
    }

    @NonNull
    private static Object[][] remove(@NonNull final Object[][] values,
                                     @NonNull final Object[] excluded) {
        final List<Object[]> result = new ArrayList<>(values.length);

        for (final Object[] value : values) {
            for (int i = 0; i < excluded.length; i++) {
                if (!((Value<?>) value[i]).value().equals(excluded[i])) {
                    result.add(value);
                    break;
                }
            }
        }

        return result.toArray(new Object[result.size()][]);
    }

    @NonNull
    public static <T> Value<T> value(@NonNls @NonNull final String name, @NonNull final T value) {
        return new Value<>(name, value);
    }

    @NonNull
    @DataProvider(name = "success or failure")
    public static Object[][] successOrFailure() {
        return provider(Boolean.asTrue("success"), Boolean.asFalse("failure"));
    }

    @NonNull
    public static Object[][] booleans(@NonNls @NonNull final String falseCase,
                                      @NonNls @NonNull final String trueCase) {
        return provider(Boolean.asFalse(falseCase), Boolean.asTrue(trueCase));
    }

    @NonNull
    public static Object[][] provider(@NonNull final Object... data) {
        final Object[][] result = new Object[data.length][];
        for (int i = 0; i < data.length; i++) {
            result[i] = new Object[]{data[i]};
        }
        return result;
    }

    @NonNull
    public static Object[][] cartesian(@NonNull final Object[][] first,
                                       @NonNull final Object[][] second) {
        final int length = first.length * second.length;
        final Object[][] result = new Object[length][];
        int i = 0;
        for (final Object[] x : first) {
            for (final Object[] y : second) {
                result[i] = new Object[x.length + y.length];
                arraycopy(x, 0, result[i], 0, x.length);
                arraycopy(y, 0, result[i], x.length, y.length);
                i++;
            }
        }
        return result;
    }

    public static final class Boolean extends Value<java.lang.Boolean> {

        private Boolean(@NonNls @NonNull final String name, final boolean value) {
            super(name, value);
        }

        @NonNull
        public static Boolean asFalse(@NonNls @NonNull final String name) {
            return new Boolean(name, false);
        }

        @NonNull
        public static Boolean asTrue(@NonNls @NonNull final String name) {
            return new Boolean(name, true);
        }
    }

    public static class Value<T> {

        @NonNls
        @NonNull
        private final String mName;
        @NonNull
        private final T mValue;

        protected Value(@NonNls @NonNull final String name, @NonNull final T value) {
            super();

            mName = name;
            mValue = value;
        }

        @NonNull
        public T value() {
            return mValue;
        }

        @NonNls
        @NonNull
        @Override
        public String toString() {
            return mName;
        }
    }

    private Providers() {
        super();
    }
}
