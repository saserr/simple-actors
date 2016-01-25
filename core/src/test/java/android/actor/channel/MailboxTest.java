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
import android.actor.Providers;
import android.actor.TestCase;
import android.support.annotation.NonNull;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import mockit.Injectable;
import mockit.StrictExpectations;

import static android.actor.Channel.Delivery.FAILURE_CAN_RETRY;
import static android.actor.Channel.Delivery.FAILURE_NO_RETRY;
import static android.actor.Channel.Delivery.SUCCESS;
import static android.actor.ChannelProviders.retries;
import static android.actor.Providers.booleans;
import static android.actor.Providers.cartesian;
import static android.actor.Providers.successOrFailure;
import static android.actor.channel.MailboxMatchers.attached;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

public class MailboxTest extends TestCase {

    @Injectable
    private Channel<String> mDestination;
    @Injectable
    private Channel.Factory mChannelFactory;

    private Channel<String> mChannel;
    private Mailbox<String> mMailbox;

    @BeforeMethod
    public final void setUp() {
        mMailbox = new Mailbox<>(mDestination);
    }

    @AfterMethod
    public final void tearDown() {
        verifyNoMoreInteractions(mDestination, mChannelFactory);
    }

    @Test(groups = {"sanity", "sanity.mailbox"})
    public final void newlyCreatedMailboxIsUnattached() {
        assertThat("mailbox", mMailbox, is(not(attached())));
    }

    @Test(groups = {"sanity", "sanity.mailbox"}, dependsOnMethods = "attach")
    public final void isAttachedAfterAttach() {
        new StrictExpectations() {{
            mChannelFactory.create(mDestination);
        }};

        assertThat("mailbox attach", mMailbox.attach(mChannelFactory), is(true));
        assertThat("mailbox", mMailbox, is(attached()));
    }

    @Test(groups = {"sanity", "sanity.mailbox"}, dependsOnMethods = "attach")
    public final void isAttachedAfterDetach() {
        new StrictExpectations() {{
            mChannelFactory.create(mDestination);
        }};

        assertThat("mailbox attach", mMailbox.attach(mChannelFactory), is(true));
        mMailbox.detach();
        assertThat("mailbox", mMailbox, is(not(attached())));
    }

    @Test(groups = {"sanity", "sanity.mailbox"})
    public final void attach() {
        new StrictExpectations() {{
            mChannelFactory.create(mDestination);
        }};

        assertThat("mailbox attach", mMailbox.attach(mChannelFactory), is(true));
    }

    @Test(dependsOnGroups = "sanity.mailbox",
            dependsOnMethods = "sendUserMessageWhenUnattached",
            dataProvider = "(retries, success)")
    public final void attachEmptiesMailbox(final Providers.Boolean retries,
                                           final Providers.Boolean success) {
        final String message = isA(RandomUserMessage);

        new StrictExpectations() {{
            mChannel = mChannelFactory.create(mDestination);
        }};

        if (retries.value()) {
            new StrictExpectations() {{
                mChannel.send(message);
                result = FAILURE_CAN_RETRY;
            }};
        }

        new StrictExpectations() {{
            mChannel.send(message);
            result = success.value() ? SUCCESS : FAILURE_NO_RETRY;
        }};

        assertThat("message send", mMailbox.send(new Mailbox.Message.User<>(message)), is(true));
        assertThat("mailbox attach", mMailbox.attach(mChannelFactory), is(success.value()));
    }

    @Test(dependsOnGroups = "sanity.mailbox",
            dataProvider = "success or failure", dataProviderClass = Providers.class)
    public final void reattach(final Providers.Boolean success) {
        new StrictExpectations() {{
            final Channel<String> channel = mChannelFactory.create(mDestination);
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

    @Test(groups = {"sanity", "sanity.mailbox"}, dataProvider = "(retries, success)")
    public final void sendControlMessageWhenEmptyAndUnattached(final Providers.Boolean retries,
                                                               final Providers.Boolean success) {
        final int message = isA(RandomControlMessage);

        if (retries.value()) {
            new StrictExpectations() {{
                mDestination.send(message);
                result = FAILURE_CAN_RETRY;
            }};
        }

        new StrictExpectations() {{
            mDestination.send(message);
            result = success.value() ? SUCCESS : FAILURE_NO_RETRY;
        }};

        final Mailbox.Message.Control<String> control = new Mailbox.Message.Control<>(message);
        assertThat("message send", mMailbox.send(control), is(success.value()));
    }

    @Test(groups = {"sanity", "sanity.mailbox"},
            dependsOnMethods = "sendUserMessageWhenUnattached")
    public final void sendControlMessageWhenNotEmptyAndUnattached() {
        final String user = isA(RandomUserMessage);
        final int control = isA(RandomControlMessage);

        assertThat("message send", mMailbox.send(new Mailbox.Message.User<>(user)), is(true));
        assertThat("message send", mMailbox.send(new Mailbox.Message.Control<String>(control)), is(true));
    }

    @Test(groups = {"sanity", "sanity.mailbox"}, dependsOnMethods = "attach",
            dataProvider = "(retries, success)")
    public final void sendControlMessageWhenAttached(final Providers.Boolean retries,
                                                     final Providers.Boolean success) {
        final int message = isA(RandomControlMessage);

        new StrictExpectations() {{
            mChannel = mChannelFactory.create(mDestination);
        }};

        if (retries.value()) {
            new StrictExpectations() {{
                mChannel.send(message);
                result = FAILURE_CAN_RETRY;
            }};
        }

        new StrictExpectations() {{
            mChannel.send(message);
            result = success.value() ? SUCCESS : FAILURE_NO_RETRY;
        }};

        assertThat("mailbox attach", mMailbox.attach(mChannelFactory), is(true));
        final Mailbox.Message.Control<String> control = new Mailbox.Message.Control<>(message);
        assertThat("mailbox send", mMailbox.send(control), is(success.value()));
    }

    @Test(groups = {"sanity", "sanity.mailbox"})
    public final void sendUserMessageWhenUnattached() {
        assertThat("mailbox send", mMailbox.send(new Mailbox.Message.User<>(a(RandomUserMessage))), is(true));
    }

    @Test(groups = {"sanity", "sanity.mailbox"}, dependsOnMethods = "attach",
            dataProvider = "(retries, success)")
    public final void sendUserMessageWhenAttached(final Providers.Boolean retries,
                                                  final Providers.Boolean success) {
        final String message = isA(RandomUserMessage);

        new StrictExpectations() {{
            mChannel = mChannelFactory.create(mDestination);
        }};

        if (retries.value()) {
            new StrictExpectations() {{
                mChannel.send(message);
                result = FAILURE_CAN_RETRY;
            }};
        }

        new StrictExpectations() {{
            mChannel.send(message);
            result = success.value() ? SUCCESS : FAILURE_NO_RETRY;
        }};

        assertThat("mailbox attach", mMailbox.attach(mChannelFactory), is(true));
        final Mailbox.Message.User<String> user = new Mailbox.Message.User<>(message);
        assertThat("mailbox send", mMailbox.send(user), is(success.value()));
    }

    @Test(dependsOnGroups = "sanity.mailbox", dependsOnMethods = "sendUserMessageWhenUnattached",
            dataProvider = "immediateness")
    public final void stopWhenUnattached(final Providers.Boolean immediately) {
        final String message = isA(RandomUserMessage);

        if (!immediately.value()) {
            new StrictExpectations() {{
                mDestination.send(message);
            }};
        }

        assertThat("mailbox send", mMailbox.send(new Mailbox.Message.User<>(message)), is(true));
        assertThat("mailbox stop", mMailbox.stop(immediately.value()), is(true));
    }

    @Test(dependsOnGroups = "sanity.mailbox", dependsOnMethods = "attach",
            dataProvider = "(success, immediateness)")
    public final void stopWhenAttached(final Providers.Boolean success,
                                       final Providers.Boolean immediately) {
        new StrictExpectations() {{
            final Channel<String> channel = mChannelFactory.create(mDestination);
            channel.stop(immediately.value());
            result = success.value();
        }};

        assertThat("mailbox attach", mMailbox.attach(mChannelFactory), is(true));
        assertThat("mailbox stop", mMailbox.stop(immediately.value()), is(success.value()));
        assertThat("mailbox", mMailbox, is(success.value() ? not(attached()) : attached()));
    }

    private static final RandomDataGenerator<Integer> RandomControlMessage = RandomInteger;
    private static final RandomDataGenerator<String> RandomUserMessage = RandomString;

    @NonNull
    @DataProvider(name = "(retries, success)")
    private static Object[][] retriesAndSuccess() {
        return cartesian(retries(), successOrFailure());
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
