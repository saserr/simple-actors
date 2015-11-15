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

public final class SystemMatchers {

    @NonNull
    public static Matcher<System> started() {
        return new State(State.Expected.Started);
    }

    @NonNull
    public static Matcher<System> paused() {
        return new State(State.Expected.Paused);
    }

    @NonNull
    public static Matcher<System> stopped() {
        return new State(State.Expected.Stopped);
    }

    private static final class State extends TypeSafeMatcher<System> {

        public enum Expected {
            Started, Paused, Stopped
        }

        @NonNull
        private final Expected mExpected;

        State(@NonNull final Expected expected) {
            super();

            mExpected = expected;
        }

        @Override
        protected boolean matchesSafely(@NonNull final System system) {
            boolean result = false;

            switch (mExpected) {
                case Started:
                    result = system.isStarted() && !system.isPaused() && !system.isStopped();
                    break;
                case Paused:
                    result = !system.isStarted() && system.isPaused() && !system.isStopped();
                    break;
                case Stopped:
                    result = !system.isStarted() && !system.isPaused() && system.isStopped();
                    break;
            }

            return result;
        }

        @Override
        protected void describeMismatchSafely(@NonNull final System system,
                                              @NonNull final Description mismatchDescription) {
            mismatchDescription.appendText("was "); //NON-NLS
            if (system.isStarted()) {
                mismatchDescription.appendText("started"); //NON-NLS
            } else if (system.isPaused()) {
                mismatchDescription.appendText("paused"); //NON-NLS
            } else if (system.isStopped()) {
                mismatchDescription.appendText("stopped"); //NON-NLS
            } else {
                mismatchDescription.appendText("unknown"); //NON-NLS
            }
        }

        @Override
        public void describeTo(@NonNull final Description description) {
            switch (mExpected) {
                case Started:
                    description.appendText("started"); //NON-NLS
                    break;
                case Paused:
                    description.appendText("paused"); //NON-NLS
                    break;
                case Stopped:
                    description.appendText("stopped"); //NON-NLS
                    break;
            }
        }
    }

    private SystemMatchers() {
        super();
    }
}
