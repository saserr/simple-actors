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

import android.actor.Messenger;
import android.actor.Providers;
import android.actor.TestCase;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import mockit.Injectable;
import mockit.StrictExpectations;

import static android.actor.messenger.MailboxMatchers.empty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

public class MailboxTest extends TestCase {

    @Injectable
    private Messenger.Callback<String> mCallback;
    @Injectable
    private Messenger<String> mMessenger;

    private Mailbox<String> mMailbox;

    @BeforeMethod
    public final void setUp() {
        mMailbox = new Mailbox<>();
    }

    @AfterMethod
    public final void tearDown() {
        verifyNoMoreInteractions(mCallback, mMessenger);
    }

    @Test(groups = {"sanity", "sanity.mailbox"})
    public final void newlyCreatedMailboxIsEmpty() {
        assertThat("mailbox", mMailbox, is(empty()));
    }

    @Test(groups = {"sanity", "sanity.mailbox"},
            dataProvider = "delivery", dataProviderClass = MessengerProviders.class)
    public final void sendUserMessageToCallback(final Providers.Value<Integer> delivery) {
        final String message = isA(RandomUserMessage);

        new StrictExpectations() {{
            mCallback.onMessage(message);
            result = delivery.value();
        }};

        assertThat("mailbox put", mMailbox.put(message), is(true));
        final boolean success = delivery.value() == Messenger.Delivery.SUCCESS;
        assertThat("mailbox take", mMailbox.take().send(mCallback), is(success));
    }

    @Test(groups = {"sanity", "sanity.mailbox"},
            dataProvider = "success or failure", dataProviderClass = Providers.class)
    public final void sendUserMessageToMessenger(final Providers.Boolean success) {
        final String message = isA(RandomUserMessage);

        new StrictExpectations() {{
            mMessenger.send(message);
            result = success.value();
        }};

        assertThat("mailbox put", mMailbox.put(message), is(true));
        assertThat("mailbox take", mMailbox.take().send(mMessenger), is(success.value()));
    }

    @Test(groups = {"sanity", "sanity.mailbox"},
            dataProvider = "success or failure", dataProviderClass = Providers.class)
    public final void sendControlMessageToCallback(final Providers.Boolean success) {
        final int message = isA(RandomControlMessage);

        new StrictExpectations() {{
            mCallback.onMessage(message);
            result = success.value();
        }};

        assertThat("mailbox put", mMailbox.put(message), is(true));
        assertThat("mailbox take", mMailbox.take().send(mCallback), is(success.value()));
    }

    @Test(groups = {"sanity", "sanity.mailbox"},
            dataProvider = "success or failure", dataProviderClass = Providers.class)
    public final void sendControlMessageToMessenger(final Providers.Boolean success) {
        final int message = isA(RandomControlMessage);

        new StrictExpectations() {{
            mMessenger.send(message);
            result = success.value();
        }};

        assertThat("mailbox put", mMailbox.put(message), is(true));
        assertThat("mailbox take", mMailbox.take().send(mMessenger), is(success.value()));
    }

    @Test(dependsOnGroups = "sanity.mailbox")
    public final void notEmptyAfterPut() {
        assertThat("mailbox put", mMailbox.put(a(RandomControlMessage)), is(true));
        assertThat("mailbox", mMailbox, is(not(empty())));
    }

    @Test(dependsOnGroups = "sanity.mailbox")
    public final void emptyAfterClear() {
        assertThat("mailbox put", mMailbox.put(a(RandomControlMessage)), is(true));
        mMailbox.clear();
        assertThat("mailbox", mMailbox, is(empty()));
    }

    @Test(dependsOnGroups = "sanity.mailbox")
    public final void emptyAfterAllMessagesAreTaken() {
        assertThat("mailbox put", mMailbox.put(a(RandomControlMessage)), is(true));
        assertThat("mailbox take", mMailbox.take(), is(notNullValue()));
        assertThat("mailbox", mMailbox, is(empty()));
    }

    private static final RandomDataGenerator<Integer> RandomControlMessage = RandomInteger;
    private static final RandomDataGenerator<String> RandomUserMessage = RandomString;
}
