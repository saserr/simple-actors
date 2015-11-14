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

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public final class Captured {

    @SuppressWarnings("unchecked")
    public static <T> T as(@NonNull final Class<T> klass) {
        return (T) Proxy.newProxyInstance(
                Captured.class.getClassLoader(),
                new Class[]{klass},
                new Capturing<T>()
        );
    }

    @SuppressWarnings("unchecked")
    public static <T> Matcher<T> into(@NonNull final T proxy) {
        return (Matcher<T>) Proxy.getInvocationHandler(proxy);
    }

    private static class Capturing<T> extends BaseMatcher<T> implements InvocationHandler {

        @Nullable
        private T mValue;

        @Override
        @SuppressWarnings("unchecked")
        public final boolean matches(final Object item) {
            mValue = (T) item;
            return true;
        }

        @Override
        public final void describeTo(final Description description) {
            description.appendText("<captured>"); //NON-NLS
        }

        @Override
        public final Object invoke(final Object proxy,
                                   final Method method,
                                   final Object[] args) throws Throwable {
            assertThat("captured value", mValue, is(notNullValue()));
            try {
                return method.invoke(mValue, args);
            } catch (final InvocationTargetException e) {
                throw e.getCause();
            }
        }
    }

    private Captured() {
        super();
    }
}
