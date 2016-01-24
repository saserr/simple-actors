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
import android.support.annotation.NonNull;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import mockit.Injectable;
import mockit.StrictExpectations;

import static android.actor.Providers.booleans;
import static android.actor.Providers.cartesian;
import static android.actor.Providers.successOrFailure;
import static android.actor.messenger.MailboxMatchers.attached;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

public class MailboxTest extends TestCase {

    @Injectable
    private Messenger.Callback<String> mCallback;
    @Injectable
    private Messenger.Factory mMessengerFactory;

    private Mailbox<String> mMailbox;

    @BeforeMethod
    public final void setUp() {
        mMailbox = new Mailbox<>(mCallback);
    }

    @AfterMethod
    public final void tearDown() {
        verifyNoMoreInteractions(mCallback, mMessengerFactory);
    }

    @Test(groups = {"sanity", "sanity.mailbox"})
    public final void newlyCreatedMailboxIsUnattached() {
        assertThat("mailbox", mMailbox, is(not(attached())));
    }

    @Test(groups = {"sanity", "sanity.mailbox"}, dependsOnMethods = "attach")
    public final void isAttachedAfterAttach() {
        new StrictExpectations() {{
            mMessengerFactory.create(mCallback);
        }};

        assertThat("mailbox attach", mMailbox.attach(mMessengerFactory), is(true));
        assertThat("mailbox", mMailbox, is(attached()));
    }

    @Test(groups = {"sanity", "sanity.mailbox"}, dependsOnMethods = "attach")
    public final void isAttachedAfterDetach() {
        new StrictExpectations() {{
            mMessengerFactory.create(mCallback);
        }};

        assertThat("mailbox attach", mMailbox.attach(mMessengerFactory), is(true));
        mMailbox.detach();
        assertThat("mailbox", mMailbox, is(not(attached())));
    }

    @Test(groups = {"sanity", "sanity.mailbox"})
    public final void attach() {
        new StrictExpectations() {{
            mMessengerFactory.create(mCallback);
        }};

        assertThat("mailbox attach", mMailbox.attach(mMessengerFactory), is(true));
    }

    @Test(dependsOnGroups = "sanity.mailbox",
            dependsOnMethods = {"sendUserMessageWhenUnattached",
                    "sendControlMessageWhenNotEmptyAndUnattached"},
            dataProvider = "success or failure", dataProviderClass = Providers.class)
    public final void attachEmptiesMailbox(final Providers.Boolean success) {
        final String message = isA(RandomUserMessage);

        new StrictExpectations() {{
            final Messenger<String> messenger = mMessengerFactory.create(mCallback);
            messenger.send(message);
            result = success.value();
        }};

        assertThat("mailbox send", mMailbox.send(message), is(true));
        assertThat("mailbox attach", mMailbox.attach(mMessengerFactory), is(success.value()));
    }

    @Test(dependsOnGroups = "sanity.mailbox",
            dataProvider = "success or failure", dataProviderClass = Providers.class)
    public final void reattach(final Providers.Boolean success) {
        new StrictExpectations() {{
            final Messenger<String> messenger = mMessengerFactory.create(mCallback);
            messenger.stop(false);
            result = success.value();
        }};

        if (success.value()) {
            new StrictExpectations() {{
                mMessengerFactory.create(mCallback);
            }};
        }

        assertThat("mailbox attach", mMailbox.attach(mMessengerFactory), is(true));
        assertThat("mailbox reattach", mMailbox.attach(mMessengerFactory), is(success.value()));
    }

    @Test(groups = {"sanity", "sanity.mailbox"},
            dataProvider = "success or failure", dataProviderClass = Providers.class)
    public final void sendControlMessageWhenEmptyAndUnattached(final Providers.Boolean success) {
        final int message = isA(RandomControlMessage);

        new StrictExpectations() {{
            mCallback.onMessage(message);
            result = success.value();
        }};

        assertThat("mailbox send", mMailbox.send(message), is(success.value()));
    }

    @Test(groups = {"sanity", "sanity.mailbox"},
            dependsOnMethods = "sendUserMessageWhenUnattached")
    public final void sendControlMessageWhenNotEmptyAndUnattached() {
        assertThat("mailbox send", mMailbox.send(a(RandomUserMessage)), is(true));
        assertThat("mailbox send", mMailbox.send(a(RandomControlMessage)), is(true));
    }

    @Test(groups = {"sanity", "sanity.mailbox"}, dependsOnMethods = "attach",
            dataProvider = "success or failure", dataProviderClass = Providers.class)
    public final void sendControlMessageWhenAttached(final Providers.Boolean success) {
        final int message = isA(RandomControlMessage);

        new StrictExpectations() {{
            final Messenger<String> messenger = mMessengerFactory.create(mCallback);
            messenger.send(message);
            result = success.value();
        }};

        assertThat("mailbox attach", mMailbox.attach(mMessengerFactory), is(true));
        assertThat("mailbox send", mMailbox.send(message), is(success.value()));
    }

    @Test(groups = {"sanity", "sanity.mailbox"})
    public final void sendUserMessageWhenUnattached() {
        assertThat("mailbox send", mMailbox.send(a(RandomUserMessage)), is(true));
    }

    @Test(groups = {"sanity", "sanity.mailbox"}, dependsOnMethods = "attach",
            dataProvider = "success or failure", dataProviderClass = Providers.class)
    public final void sendUserMessageWhenAttached(final Providers.Boolean success) {
        final String message = isA(RandomUserMessage);

        new StrictExpectations() {{
            final Messenger<String> messenger = mMessengerFactory.create(mCallback);
            messenger.send(message);
            result = success.value();
        }};

        assertThat("mailbox attach", mMailbox.attach(mMessengerFactory), is(true));
        assertThat("mailbox send", mMailbox.send(message), is(success.value()));
    }

    @Test(dependsOnGroups = "sanity.mailbox", dependsOnMethods = "sendUserMessageWhenUnattached",
            dataProvider = "immediateness")
    public final void stopWhenUnattached(final Providers.Boolean immediately) {
        final String message = isA(RandomUserMessage);

        if (!immediately.value()) {
            new StrictExpectations() {{
                mCallback.onMessage(message);
            }};
        }

        assertThat("mailbox send", mMailbox.send(message), is(true));
        assertThat("mailbox stop", mMailbox.stop(immediately.value()), is(true));
    }

    @Test(dependsOnGroups = "sanity.mailbox", dependsOnMethods = "attach",
            dataProvider = "(success, immediateness)")
    public final void stopWhenAttached(final Providers.Boolean success,
                                       final Providers.Boolean immediately) {
        new StrictExpectations() {{
            final Messenger<String> messenger = mMessengerFactory.create(mCallback);
            messenger.stop(immediately.value());
            result = success.value();
        }};

        assertThat("mailbox attach", mMailbox.attach(mMessengerFactory), is(true));
        assertThat("mailbox stop", mMailbox.stop(immediately.value()), is(success.value()));
        assertThat("mailbox", mMailbox, is(success.value() ? not(attached()) : attached()));
    }

    private static final RandomDataGenerator<Integer> RandomControlMessage = RandomInteger;
    private static final RandomDataGenerator<String> RandomUserMessage = RandomString;

    @NonNull
    @DataProvider(name = "(success, immediateness)")
    private static Object[][] successAndImmediateness() {
        return cartesian(successOrFailure(), immediateness());
    }

    @NonNull
    @DataProvider(name = "immediateness")
    private static Object[][] immediateness() {
        return booleans("not immediately", "immediately");
    }
}
