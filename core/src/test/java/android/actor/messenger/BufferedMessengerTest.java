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
import mockit.Mocked;
import mockit.StrictExpectations;

import static android.actor.Providers.booleans;
import static android.actor.Providers.cartesian;
import static android.actor.Providers.successOrFailure;
import static android.actor.messenger.BufferedMessengerMatchers.attached;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

public class BufferedMessengerTest extends TestCase {

    @Mocked
    private Mailbox<String> mAllMailboxes;

    @Injectable
    private Messenger.Callback<String> mCallback;
    @Injectable
    private Messenger.Factory mMessengerFactory;

    private Mailbox<String> mMailbox;
    private BufferedMessenger<String> mMessenger;

    @BeforeMethod
    public final void setUp() {
        new StrictExpectations() {{
            mMailbox = new Mailbox<>();
        }};

        mMessenger = new BufferedMessenger<>(mCallback);
    }

    @AfterMethod
    public final void tearDown() {
        verifyNoMoreInteractions(mAllMailboxes);
        verifyNoMoreInteractions(mMailbox, mMessenger, mMessengerFactory);
    }

    @Test(groups = {"sanity", "sanity.mailbox"})
    public final void newlyCreatedMessengerIsUnattached() {
        assertThat("messenger", mMessenger, is(not(attached())));
    }

    @Test(groups = {"sanity", "sanity.mailbox"}, dependsOnMethods = "attach")
    public final void isAttachedAfterAttach() {
        new StrictExpectations() {{
            mMessengerFactory.create(mCallback);
            mMailbox.isEmpty();
            result = true;
        }};

        assertThat("messenger attach", mMessenger.attach(mMessengerFactory), is(true));
        assertThat("messenger", mMessenger, is(attached()));
    }

    @Test(groups = {"sanity", "sanity.mailbox"}, dependsOnMethods = "attach")
    public final void isAttachedAfterDetach() {
        new StrictExpectations() {{
            mMessengerFactory.create(mCallback);
            mMailbox.isEmpty();
            result = true;
        }};

        assertThat("messenger attach", mMessenger.attach(mMessengerFactory), is(true));
        mMessenger.detach();
        assertThat("messenger", mMessenger, is(not(attached())));
    }

    @Test(groups = {"sanity", "sanity.messenger", "sanity.messenger.buffered"})
    public final void attach() {
        new StrictExpectations() {{
            mMessengerFactory.create(mCallback);
            mMailbox.isEmpty();
            result = true;
        }};

        assertThat("messenger attach", mMessenger.attach(mMessengerFactory), is(true));
    }

    @Test(dependsOnGroups = "sanity.messenger.buffered",
            dataProvider = "success or failure", dataProviderClass = Providers.class)
    public final void attachEmptiesMailbox(final Providers.Boolean success) {
        new StrictExpectations() {{
            final Messenger<String> messenger = mMessengerFactory.create(mCallback);
            mMailbox.isEmpty();
            result = false;
            final Mailbox.Message<String> message = mMailbox.take();
            message.send(messenger);
            result = success.value();
        }};

        if (success.value()) {
            new StrictExpectations() {{
                mMailbox.isEmpty();
                result = true;
            }};
        }

        assertThat("messenger attach", mMessenger.attach(mMessengerFactory), is(success.value()));
    }

    @Test(dependsOnGroups = "sanity.messenger.buffered",
            dataProvider = "success or failure", dataProviderClass = Providers.class)
    public final void reattach(final Providers.Boolean success) {
        new StrictExpectations() {{
            final Messenger<String> messenger = mMessengerFactory.create(mCallback);
            mMailbox.isEmpty();
            result = true;
            messenger.stop(false);
            result = success.value();
        }};

        if (success.value()) {
            new StrictExpectations() {{
                mMessengerFactory.create(mCallback);
                mMailbox.isEmpty();
                result = true;
            }};
        }

        assertThat("messenger attach", mMessenger.attach(mMessengerFactory), is(true));
        assertThat("messenger reattach", mMessenger.attach(mMessengerFactory), is(success.value()));
    }

    @Test(groups = {"sanity", "sanity.messenger", "sanity.messenger.buffered"},
            dataProvider = "(success, emptiness)")
    public final void sendControlMessageWhenUnattached(final Providers.Boolean success,
                                                       final Providers.Boolean empty) {
        final int message = isA(RandomControlMessage);

        new StrictExpectations() {{
            mMailbox.isEmpty();
            result = empty.value();
        }};

        if (empty.value()) {
            new StrictExpectations() {{
                mCallback.onMessage(message);
                result = success.value();
            }};
        } else {
            new StrictExpectations() {{
                mMailbox.put(message);
                result = success.value();
            }};
        }

        assertThat("messenger send", mMessenger.send(message), is(success.value()));
    }

    @Test(groups = {"sanity", "sanity.messenger", "sanity.messenger.buffered"},
            dependsOnMethods = "attach",
            dataProvider = "success or failure", dataProviderClass = Providers.class)
    public final void sendControlMessageWhenAttached(final Providers.Boolean success) {
        final int message = isA(RandomControlMessage);

        new StrictExpectations() {{
            final Messenger<String> messenger = mMessengerFactory.create(mCallback);
            mMailbox.isEmpty();
            result = true;
            messenger.send(message);
            result = success.value();
        }};

        assertThat("messenger attach", mMessenger.attach(mMessengerFactory), is(true));
        assertThat("messenger send", mMessenger.send(message), is(success.value()));
    }

    @Test(groups = {"sanity", "sanity.messenger", "sanity.messenger.buffered"},
            dataProvider = "success or failure", dataProviderClass = Providers.class)
    public final void sendUserMessageWhenUnattached(final Providers.Boolean success) {
        final String message = isA(RandomUserMessage);
        final long delay = isA(RandomDelay);

        new StrictExpectations() {{
            mMailbox.put(message, delay);
            result = success.value();
        }};

        assertThat("messenger send", mMessenger.send(message, delay), is(success.value()));
    }

    @Test(groups = {"sanity", "sanity.messenger", "sanity.messenger.buffered"},
            dependsOnMethods = "attach",
            dataProvider = "success or failure", dataProviderClass = Providers.class)
    public final void sendUserMessageWhenAttached(final Providers.Boolean success) {
        final String message = isA(RandomUserMessage);
        final long delay = isA(RandomDelay);

        new StrictExpectations() {{
            final Messenger<String> messenger = mMessengerFactory.create(mCallback);
            mMailbox.isEmpty();
            result = true;
            messenger.send(message, delay);
            result = success.value();
        }};

        assertThat("messenger attach", mMessenger.attach(mMessengerFactory), is(true));
        assertThat("messenger send", mMessenger.send(message, delay), is(success.value()));
    }

    @Test(dependsOnGroups = "sanity.messenger.buffered",
            dataProvider = "(immediateness, emptiness)")
    public final void stopWhenUnattached(final Providers.Boolean immediately,
                                         final Providers.Boolean empty) {
        if (immediately.value()) {
            new StrictExpectations() {{
                mMailbox.clear();
            }};
        } else {
            new StrictExpectations() {{
                mMailbox.isEmpty();
                result = empty.value();
            }};

            if (!empty.value()) {
                new StrictExpectations() {{
                    final Mailbox.Message<String> message = mMailbox.take();
                    message.send(mCallback);
                    result = true;
                    mMailbox.isEmpty();
                    result = true;
                }};
            }
        }

        assertThat("messenger stop", mMessenger.stop(immediately.value()), is(true));
    }

    @Test(dependsOnGroups = "sanity.messenger.buffered", dependsOnMethods = "attach",
            dataProvider = "(success, immediateness, emptiness)")
    public final void stopWhenAttached(final Providers.Boolean success,
                                       final Providers.Boolean immediately,
                                       final Providers.Boolean empty) {
        new StrictExpectations() {{
            final Messenger<String> messenger = mMessengerFactory.create(mCallback);
            mMailbox.isEmpty();
            result = true;
            messenger.stop(immediately.value());
            result = success.value();
        }};

        if (success.value()) {
            if (immediately.value()) {
                new StrictExpectations() {{
                    mMailbox.clear();
                }};
            } else {
                new StrictExpectations() {{
                    mMailbox.isEmpty();
                    result = empty.value();
                }};

                if (!empty.value()) {
                    new StrictExpectations() {{
                        final Mailbox.Message<String> message = mMailbox.take();
                        message.send(mCallback);
                        result = true;
                        mMailbox.isEmpty();
                        result = true;
                    }};
                }
            }
        }

        assertThat("messenger attach", mMessenger.attach(mMessengerFactory), is(true));
        assertThat("messenger stop", mMessenger.stop(immediately.value()), is(success.value()));
        assertThat("messenger", mMessenger, is(success.value() ? not(attached()) : attached()));
    }

    private static final RandomDataGenerator<Integer> RandomControlMessage = RandomInteger;
    private static final RandomDataGenerator<String> RandomUserMessage = RandomString;
    private static final RandomDataGenerator<Long> RandomDelay = RandomLong.thatIs(Positive);

    @NonNull
    @DataProvider(name = "(success, immediateness, emptiness)")
    private static Object[][] successAndImmediatenessAndEmptiness() {
        return cartesian(successOrFailure(), immediatenessAndEmptiness());
    }

    @NonNull
    @DataProvider(name = "(success, emptiness)")
    private static Object[][] successAndEmptiness() {
        return cartesian(successOrFailure(), emptiness());
    }

    @NonNull
    @DataProvider(name = "(immediateness, emptiness)")
    private static Object[][] immediatenessAndEmptiness() {
        return cartesian(booleans("not immediately", "immediately"), emptiness());
    }

    @NonNull
    @DataProvider(name = "emptiness")
    private static Object[][] emptiness() {
        return booleans("not empty", "empty");
    }
}
