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

package android.actor.messenger;

import android.support.annotation.NonNull;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.jetbrains.annotations.NonNls;

public final class LooperMessengerMatchers {

    @NonNull
    public static Matcher<LooperMessenger<?>> onCurrentThread() {
        return new OnCurrentThread();
    }

    @NonNull
    public static Matcher<LooperMessenger<?>> hasUndeliveredMessages() {
        return new HasUndeliveredMessages(true);
    }

    @NonNull
    public static Matcher<LooperMessenger<?>> hasNoUndeliveredMessages() {
        return new HasUndeliveredMessages(false);
    }

    private static final class OnCurrentThread extends TypeSafeMatcher<LooperMessenger<?>> {

        @Override
        protected boolean matchesSafely(final LooperMessenger<?> messenger) {
            return messenger.isOnCurrentThread();
        }

        @Override
        protected void describeMismatchSafely(final LooperMessenger<?> messenger,
                                              final Description mismatchDescription) {
            mismatchDescription.appendText("was not on current thread"); //NON-NLS
        }

        @Override
        public void describeTo(final Description description) {
            description.appendText("on current thread"); //NON-NLS
        }
    }

    private static final class HasUndeliveredMessages extends TypeSafeMatcher<LooperMessenger<?>> {

        private final boolean mValue;

        private HasUndeliveredMessages(final boolean value) {
            super();

            mValue = value;
        }

        @Override
        protected boolean matchesSafely(final LooperMessenger<?> messenger) {
            return mValue == messenger.hasUndeliveredMessages();
        }

        @Override
        protected void describeMismatchSafely(final LooperMessenger<?> messenger,
                                              final Description mismatchDescription) {
            mismatchDescription.appendText("had " + describe(messenger.hasUndeliveredMessages()) + "undelivered messages"); //NON-NLS
        }

        @Override
        public void describeTo(final Description description) {
            description.appendText("has " + describe(mValue) + "undelivered messages"); //NON-NLS
        }

        @NonNls
        @NonNull
        private static String describe(final boolean value) {
            return value ? "" : "no ";
        }
    }

    private LooperMessengerMatchers() {
        super();
    }
}