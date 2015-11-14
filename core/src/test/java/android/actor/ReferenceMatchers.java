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

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.jetbrains.annotations.NonNls;

public final class ReferenceMatchers {

    @NonNull
    public static Matcher<Reference<?>> hasName(@NonNull final Actor.Name name) {
        return new HasName(name);
    }

    @NonNull
    public static Matcher<Reference<?>> stopped() {
        return new Stopped();
    }

    private static final class HasName extends TypeSafeMatcher<Reference<?>> {

        @NonNull
        private final Actor.Name mName;

        private HasName(@NonNull final Actor.Name name) {
            super();

            mName = name;
        }

        @Override
        protected boolean matchesSafely(final Reference<?> reference) {
            return mName.equals(reference.getName());
        }

        @Override
        protected void describeMismatchSafely(final Reference<?> reference,
                                              @NonNls final Description mismatchDescription) {
            mismatchDescription.appendText("had name " + reference.getName()); //NON-NLS
        }

        @Override
        public void describeTo(@NonNls final Description description) {
            description.appendText("has name " + mName); //NON-NLS
        }
    }

    private static final class Stopped extends TypeSafeMatcher<Reference<?>> {

        @Override
        protected boolean matchesSafely(final Reference<?> reference) {
            return reference.isStopped();
        }

        @Override
        protected void describeMismatchSafely(final Reference<?> reference,
                                              final Description mismatchDescription) {
            mismatchDescription.appendText("was not stopped"); //NON-NLS
        }

        @Override
        public void describeTo(@NonNull final Description description) {
            description.appendText("stopped"); //NON-NLS
        }
    }

    private ReferenceMatchers() {
        super();
    }
}
