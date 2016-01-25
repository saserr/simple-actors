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

package android.actor.channel;

import android.support.annotation.NonNull;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public final class MailboxMatchers {

    @NonNull
    public static Matcher<Mailbox<?>> attached() {
        return new Attached();
    }

    private static final class Attached extends TypeSafeMatcher<Mailbox<?>> {

        @Override
        protected boolean matchesSafely(final Mailbox<?> mailbox) {
            return mailbox.isAttached();
        }

        @Override
        protected void describeMismatchSafely(final Mailbox<?> mailbox,
                                              final Description mismatchDescription) {
            mismatchDescription.appendText("was not attached"); //NON-NLS
        }

        @Override
        public void describeTo(final Description description) {
            description.appendText("attached"); //NON-NLS
        }
    }

    private MailboxMatchers() {
        super();
    }
}
