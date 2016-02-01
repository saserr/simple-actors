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

import static android.actor.Channel.Delivery.ERROR;
import static android.actor.Channel.Delivery.SUCCESS;
import static android.actor.ChannelProviders.delivery;
import static android.actor.ChannelProviders.immediateness;
import static android.actor.Providers.cartesian;
import static android.actor.Providers.successOrFailure;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

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
    public final void attach() {
        new StrictExpectations() {{
            mChannelFactory.create(mDestination);
        }};

        assertThat("mailbox attach", mMailbox.attach(mChannelFactory), is(true));
    }

    @Test(dependsOnGroups = "sanity.channel.mailbox",
            dependsOnMethods = "sendMessageWhenUnattached",
            dataProvider = "delivery", dataProviderClass = ChannelProviders.class)
    public final void attachEmptiesMailbox(final Providers.Value<Integer> delivery) {
        new StrictExpectations() {{
            mChannel = mChannelFactory.create(mDestination);
            Send.withRetries(mChannel, mMessage);
            result = delivery.value();
        }};

        assertThat("message send", mMailbox.send(mMessage), is(SUCCESS));
        final boolean success = delivery.value() == SUCCESS;
        assertThat("mailbox attach", mMailbox.attach(mChannelFactory), is(success));
    }

    @Test(dependsOnGroups = "sanity.channel.mailbox", dependsOnMethods = "attach",
            expectedExceptions = UnsupportedOperationException.class,
            expectedExceptionsMessageRegExp = "Mailbox is already attached!")
    public final void reattach() {
        new StrictExpectations() {{
            mChannelFactory.create(mDestination);
        }};

        assertThat("mailbox attach", mMailbox.attach(mChannelFactory), is(true));
        mMailbox.attach(mChannelFactory);
    }

    @Test(dependsOnGroups = "sanity.channel.mailbox", dependsOnMethods = "attach")
    public final void attachAfterDetach() {
        new StrictExpectations() {{
            mChannelFactory.create(mDestination);
            times = 2;
        }};

        assertThat("mailbox attach", mMailbox.attach(mChannelFactory), is(true));
        assertThat("mailbox detach", mMailbox.detach(), is(true));
        assertThat("mailbox attach", mMailbox.attach(mChannelFactory), is(true));
    }

    @Test(groups = {"sanity", "sanity.channel", "sanity.channel.mailbox"})
    public final void sendMessageWhenUnattached() {
        assertThat("mailbox send", mMailbox.send(mMessage), is(SUCCESS));
    }

    @Test(groups = {"sanity", "sanity.channel", "sanity.channel.mailbox"},
            dependsOnMethods = "attach",
            dataProvider = "delivery", dataProviderClass = ChannelProviders.class)
    public final void sendMessageWhenAttached(final Providers.Value<Integer> delivery) {
        new StrictExpectations() {{
            mChannel = mChannelFactory.create(mDestination);
            mChannel.send(mMessage);
            result = delivery.value();
        }};

        assertThat("mailbox attach", mMailbox.attach(mChannelFactory), is(true));
        assertThat("mailbox send", mMailbox.send(mMessage), is(delivery.value()));
    }

    @Test(dependsOnGroups = "sanity.channel.mailbox",
            dependsOnMethods = "sendMessageWhenUnattached",
            dataProvider = "success or failure", dataProviderClass = Providers.class)
    public final void immediatelyStopWhenUnattached(final Providers.Boolean success) {
        new StrictExpectations() {{
            mDestination.stop(true);
            result = success.value();
        }};

        assertThat("mailbox send", mMailbox.send(mMessage), is(SUCCESS));
        assertThat("mailbox send", mMailbox.send(mMessage), is(SUCCESS));
        assertThat("mailbox stop", mMailbox.stop(true), is(success.value()));
    }

    @Test(dependsOnGroups = "sanity.channel.mailbox",
            dependsOnMethods = "sendMessageWhenUnattached",
            dataProvider = "(success, delivery)")
    public final void notImmediatelyStopWhenUnattached(final Providers.Boolean success,
                                                       final Providers.Value<Integer> delivery) {
        new StrictExpectations() {{
            Send.withRetries(mDestination, mMessage, 1);
            result = delivery.value();
        }};

        if (delivery.value() != ERROR) {
            new StrictExpectations() {{
                Send.withRetries(mDestination, mMessage, 1);
                result = SUCCESS;
                mDestination.stop(false);
                result = success.value();
            }};
        }

        assertThat("mailbox send", mMailbox.send(mMessage), is(SUCCESS));
        assertThat("mailbox send", mMailbox.send(mMessage), is(SUCCESS));
        final boolean stopped = (delivery.value() == ERROR) || success.value();
        assertThat("mailbox stop", mMailbox.stop(false), is(stopped));
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
    }

    @Test(dependsOnGroups = "sanity.channel.mailbox",
            dependsOnMethods = "immediatelyStopWhenUnattached",
            dataProvider = "(success, immediateness)")
    public final void doubleStop(final Providers.Boolean success,
                                 final Providers.Boolean immediately) {
        new StrictExpectations() {{
            mDestination.stop(true);
            result = success.value();
        }};

        if (!success.value()) {
            new StrictExpectations() {{
                mDestination.stop(immediately.value());
                result = true;
            }};
        }

        assertThat("mailbox first stop", mMailbox.stop(true), is(success.value()));
        assertThat("mailbox second stop", mMailbox.stop(immediately.value()), is(true));
    }

    @Test(dependsOnGroups = "sanity.channel.mailbox",
            dependsOnMethods = "immediatelyStopWhenUnattached",
            expectedExceptions = UnsupportedOperationException.class,
            expectedExceptionsMessageRegExp = Channel.STOPPED)
    public final void sendAfterStop() {
        new StrictExpectations() {{
            mDestination.stop(false);
            result = true;
        }};

        assertThat("channel stop", mMailbox.stop(false), is(true));
        mMailbox.send(mMessage);
    }

    @Test(dependsOnGroups = "sanity.channel.mailbox",
            dependsOnMethods = "immediatelyStopWhenUnattached")
    public final void attachAfterStop() {
        new StrictExpectations() {{
            mDestination.stop(false);
            result = true;
        }};

        assertThat("channel stop", mMailbox.stop(false), is(true));
        assertThat("channel attach", mMailbox.attach(mChannelFactory), is(false));
    }

    @NonNull
    @DataProvider(name = "(success, immediateness)")
    private static Object[][] successAndImmediateness() {
        return cartesian(successOrFailure(), immediateness());
    }

    @NonNull
    @DataProvider(name = "(success, delivery)")
    private static Object[][] successAndDelivery() {
        return cartesian(successOrFailure(), delivery());
    }

    private interface Message {}
}
