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

public final class TimeMatchers {

    @NonNull
    public static Matcher<Long> after(final long from) {
        return new After(from);
    }

    private static class After extends TypeSafeMatcher<Long> {

        private final long mFrom;

        After(final long from) {
            super();

            mFrom = from;
        }

        @Override
        protected final boolean matchesSafely(final Long actual) {
            return actual >= mFrom;
        }

        @Override
        public final void describeTo(@NonNull final Description description) {
            description.appendText("a time after ").appendText(String.valueOf(mFrom)); //NON-NLS
        }
    }

    private TimeMatchers() {
        super();
    }
}
