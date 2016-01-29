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

import android.actor.Channel;
import android.actor.ChannelProviders;
import android.actor.Providers;
import android.actor.TestCase;
import android.support.annotation.NonNull;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import mockit.Injectable;
import mockit.Mocked;
import mockit.StrictExpectations;

import static android.actor.Channel.Delivery.SUCCESS;
import static android.actor.Providers.booleans;
import static android.actor.Providers.cartesian;
import static android.actor.Providers.successOrFailure;
import static android.actor.channel.MailboxMatchers.attached;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

public class MailboxTest extends TestCase {

    @Mocked
    private Send<?> mAllSends;

    @Injectable
    private Message mMessage;
    @Injectable
    private Channel<Message> mDestination;
    @Injectable
    private Channel.Factory mChannelFactory;

    private Channel<Message> mChannel;
    private Mailbox<Message> mMailbox;

    @BeforeMethod
    public final void setUp() {
        mMailbox = new Mailbox<>(mDestination);
    }

    @AfterMethod
    public final void tearDown() {
        verifyNoMoreInteractions(mAllSends);
        verifyNoMoreInteractions(mMessage, mDestination, mChannelFactory);
    }

    @Test(groups = {"sanity", "sanity.channel", "sanity.channel.mailbox"})
    public final void newlyCreatedMailboxIsUnattached() {
        assertThat("mailbox", mMailbox, is(not(attached())));
    }

    @Test(groups = {"sanity", "sanity.channel", "sanity.channel.mailbox"},
            dependsOnMethods = "attach")
    public final void isAttachedAfterAttach() {
        new StrictExpectations() {{
            mChannelFactory.create(mDestination);
        }};

        assertThat("mailbox attach", mMailbox.attach(mChannelFactory), is(true));
        assertThat("mailbox", mMailbox, is(attached()));
    }

    @Test(groups = {"sanity", "sanity.channel", "sanity.channel.mailbox"},
            dependsOnMethods = "attach")
    public final void isAttachedAfterDetach() {
        new StrictExpectations() {{
            mChannelFactory.create(mDestination);
        }};

        assertThat("mailbox attach", mMailbox.attach(mChannelFactory), is(true));
        mMailbox.detach();
        assertThat("mailbox", mMailbox, is(not(attached())));
    }

    @Test(groups = {"sanity", "sanity.channel", "sanity.channel.mailbox"})
    public final void attach() {
        new StrictExpectations() {{
            mChannelFactory.create(mDestination);
        }};

        assertThat("mailbox attach", mMailbox.attach(mChannelFactory), is(true));
    }

    @Test(dependsOnGroups = "sanity.channel.mailbox",
            dependsOnMethods = "sendUserMessageWhenUnattached",
            dataProvider = "success or failure", dataProviderClass = Providers.class)
    public final void attachEmptiesMailbox(final Providers.Boolean success) {
        new StrictExpectations() {{
            mMessage.isControl();
            result = false;
            mChannel = mChannelFactory.create(mDestination);
            Send.withRetries(mChannel, mMessage);
            result = success.value();
        }};

        assertThat("message send", mMailbox.send(mMessage), is(SUCCESS));
        assertThat("mailbox attach", mMailbox.attach(mChannelFactory), is(success.value()));
    }

    @Test(dependsOnGroups = "sanity.channel.mailbox",
            dataProvider = "success or failure", dataProviderClass = Providers.class)
    public final void reattach(final Providers.Boolean success) {
        new StrictExpectations() {{
            final Channel<Message> channel = mChannelFactory.create(mDestination);
            channel.stop(false);
            result = success.value();
        }};

        if (success.value()) {
            new StrictExpectations() {{
                mChannelFactory.create(mDestination);
            }};
        }

        assertThat("mailbox attach", mMailbox.attach(mChannelFactory), is(true));
        assertThat("mailbox reattach", mMailbox.attach(mChannelFactory), is(success.value()));
    }

    @Test(groups = {"sanity", "sanity.channel", "sanity.channel.mailbox"},
            dataProvider = "delivery", dataProviderClass = ChannelProviders.class)
    public final void sendControlMessageWhenEmptyAndUnattached(final Providers.Value<Integer> delivery) {
        new StrictExpectations() {{
            mMessage.isControl();
            result = true;
            mDestination.send(mMessage);
            result = delivery.value();
        }};

        assertThat("message send", mMailbox.send(mMessage), is(delivery.value()));
    }

    @Test(groups = {"sanity", "sanity.channel", "sanity.channel.mailbox"},
            dependsOnMethods = "sendUserMessageWhenUnattached")
    public final void sendControlMessageWhenNotEmptyAndUnattached() {
        new StrictExpectations() {{
            mMessage.isControl();
            result = new boolean[]{false, true};
        }};

        assertThat("message send", mMailbox.send(mMessage), is(SUCCESS));
        assertThat("message send", mMailbox.send(mMessage), is(SUCCESS));
    }

    @Test(groups = {"sanity", "sanity.channel", "sanity.channel.mailbox"},
            dependsOnMethods = "attach",
            dataProvider = "delivery", dataProviderClass = ChannelProviders.class)
    public final void sendControlMessageWhenAttached(final Providers.Value<Integer> delivery) {
        new StrictExpectations() {{
            mChannel = mChannelFactory.create(mDestination);
            mChannel.send(mMessage);
            result = delivery.value();
        }};

        assertThat("mailbox attach", mMailbox.attach(mChannelFactory), is(true));
        assertThat("mailbox send", mMailbox.send(mMessage), is(delivery.value()));
    }

    @Test(groups = {"sanity", "sanity.channel", "sanity.channel.mailbox"})
    public final void sendUserMessageWhenUnattached() {
        new StrictExpectations() {{
            mMessage.isControl();
            result = false;
        }};

        assertThat("mailbox send", mMailbox.send(mMessage), is(SUCCESS));
    }

    @Test(groups = {"sanity", "sanity.channel", "sanity.channel.mailbox"},
            dependsOnMethods = "attach",
            dataProvider = "delivery", dataProviderClass = ChannelProviders.class)
    public final void sendUserMessageWhenAttached(final Providers.Value<Integer> delivery) {
        new StrictExpectations() {{
            mChannel = mChannelFactory.create(mDestination);
            mChannel.send(mMessage);
            result = delivery.value();
        }};

        assertThat("mailbox attach", mMailbox.attach(mChannelFactory), is(true));
        assertThat("mailbox send", mMailbox.send(mMessage), is(delivery.value()));
    }

    @Test(dependsOnGroups = "sanity.channel.mailbox", dependsOnMethods = "sendUserMessageWhenUnattached",
            dataProvider = "immediateness")
    public final void stopWhenUnattached(final Providers.Boolean immediately) {
        new StrictExpectations() {{
            mMessage.isControl();
            result = false;
        }};

        if (!immediately.value()) {
            new StrictExpectations() {{
                Send.withRetries(mDestination, mMessage, 1);
            }};
        }

        assertThat("mailbox send", mMailbox.send(mMessage), is(SUCCESS));
        assertThat("mailbox stop", mMailbox.stop(immediately.value()), is(true));
    }

    @Test(dependsOnGroups = "sanity.channel.mailbox", dependsOnMethods = "attach",
            dataProvider = "(success, immediateness)")
    public final void stopWhenAttached(final Providers.Boolean success,
                                       final Providers.Boolean immediately) {
        new StrictExpectations() {{
            final Channel<Message> channel = mChannelFactory.create(mDestination);
            channel.stop(immediately.value());
            result = success.value();
        }};

        assertThat("mailbox attach", mMailbox.attach(mChannelFactory), is(true));
        assertThat("mailbox stop", mMailbox.stop(immediately.value()), is(success.value()));
        assertThat("mailbox", mMailbox, is(success.value() ? not(attached()) : attached()));
    }

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
