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

public final class ContextMatchers {

    @NonNull
    public static Matcher<Context> hasName(@NonNull final Actor.Name name) {
        return new HasName(name);
    }

    private static final class HasName extends TypeSafeMatcher<Context> {

        @NonNull
        private final Actor.Name mName;

        private HasName(@NonNull final Actor.Name name) {
            super();

            mName = name;
        }

        @Override
        protected boolean matchesSafely(final Context context) {
            return mName.equals(context.getName());
        }

        @Override
        protected void describeMismatchSafely(final Context context,
                                              @NonNls final Description mismatchDescription) {
            mismatchDescription.appendText("had name " + context.getName()); //NON-NLS
        }

        @Override
        public void describeTo(@NonNls final Description description) {
            description.appendText("has name " + mName); //NON-NLS
        }
    }

    private ContextMatchers() {
        super();
    }
}
