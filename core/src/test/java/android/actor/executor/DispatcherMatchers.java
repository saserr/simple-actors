/*
 * Copyright 2016 the original author or authors
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

import android.support.annotation.NonNull;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public final class DispatcherMatchers {

    private DispatcherMatchers() {
        super();
    }

    @NonNull
    public static Matcher<Dispatcher> empty() {
        return new Empty();
    }

    @NonNull
    public static Matcher<Dispatcher> hasSize(final int size) {
        return new HasSize(size);
    }

    private static final class Empty extends TypeSafeMatcher<Dispatcher> {

        @Override
        protected boolean matchesSafely(final Dispatcher dispatcher) {
            return dispatcher.isEmpty();
        }

        @Override
        protected void describeMismatchSafely(final Dispatcher dispatcher,
                                              final Description mismatchDescription) {
            mismatchDescription.appendText("was not empty"); //NON-NLS
        }

        @Override
        public void describeTo(final Description description) {
            description.appendText("empty"); //NON-NLS
        }
    }

    private static final class HasSize extends TypeSafeMatcher<Dispatcher> {

        private final int mSize;

        private HasSize(final int size) {
            super();

            mSize = size;
        }

        @Override
        protected boolean matchesSafely(final Dispatcher dispatcher) {
            return mSize == dispatcher.size();
        }

        @Override
        protected void describeMismatchSafely(final Dispatcher dispatcher,
                                              final Description mismatchDescription) {
            mismatchDescription.appendText("had size " + dispatcher.size()); //NON-NLS
        }

        @Override
        public void describeTo(final Description description) {
            description.appendText("has size " + mSize); //NON-NLS
        }
    }
}
