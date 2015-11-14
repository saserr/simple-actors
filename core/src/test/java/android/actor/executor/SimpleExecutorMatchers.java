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

public final class SimpleExecutorMatchers {

    private SimpleExecutorMatchers() {
        super();
    }

    @NonNull
    public static Matcher<SimpleExecutor> empty() {
        return new Empty();
    }

    @NonNull
    public static Matcher<SimpleExecutor> hasSize(final int size) {
        return new HasSize(size);
    }

    private static final class Empty extends TypeSafeMatcher<SimpleExecutor> {

        @Override
        protected boolean matchesSafely(final SimpleExecutor executor) {
            return executor.isEmpty();
        }

        @Override
        protected void describeMismatchSafely(final SimpleExecutor executor,
                                              final Description mismatchDescription) {
            mismatchDescription.appendText("was not empty"); //NON-NLS
        }

        @Override
        public void describeTo(final Description description) {
            description.appendText("empty"); //NON-NLS
        }
    }

    private static final class HasSize extends TypeSafeMatcher<SimpleExecutor> {

        private final int mSize;

        private HasSize(final int size) {
            super();

            mSize = size;
        }

        @Override
        protected boolean matchesSafely(final SimpleExecutor executor) {
            return mSize == executor.size();
        }

        @Override
        protected void describeMismatchSafely(final SimpleExecutor executor,
                                              final Description mismatchDescription) {
            mismatchDescription.appendText("had size " + executor.size()); //NON-NLS
        }

        @Override
        public void describeTo(final Description description) {
            description.appendText("has size " + mSize); //NON-NLS
        }
    }
}
