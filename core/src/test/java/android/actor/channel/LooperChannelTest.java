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
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import mockit.Injectable;
import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.StrictExpectations;
import mockit.Verifications;

import static android.actor.Channel.Delivery.FAILURE_CAN_RETRY;
import static android.actor.Channel.Delivery.FAILURE_NO_RETRY;
import static android.actor.Channel.Delivery.SUCCESS;
import static android.actor.ChannelProviders.retries;
import static android.actor.Configuration.MaximumNumberOfRetries;
import static android.actor.Providers.booleans;
import static android.actor.Providers.cartesian;
import static android.actor.Providers.successOrFailure;
import static android.actor.Providers.without;
import static android.actor.channel.LooperChannel.Message.Type.CONTROL;
import static android.actor.channel.LooperChannel.Message.Type.USER;
import static android.actor.channel.LooperChannelMatchers.hasNoUndeliveredMessages;
import static android.actor.channel.LooperChannelMatchers.onCurrentThread;
import static java.lang.Thread.currentThread;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

public class LooperChannelTest extends TestCase {

    @Mocked
    private Handler mAllHandlers;

    @Injectable
    private Looper mLooper;
    @Injectable
    private Channel<String> mDestination;
    @Injectable
    private Message mMessage;

    private LooperChannel<String> mChannel;
    private Handler mHandler;

    @BeforeMethod
    public final void setUp() {
        new StrictExpectations() {{
            mHandler = new Handler(mLooper);
        }};

        mChannel = new LooperChannel<>(mLooper, mDestination);
    }

    @AfterMethod
    public final void tearDown() {
        new Verifications() {{
            mHandler.hasMessages(CONTROL, any);
            minTimes = 0;
            mHandler.hasMessages(USER, any);
            minTimes = 0;
            mLooper.getThread();
            minTimes = 0;
        }};

        verifyNoMoreInteractions(mAllHandlers);
        verifyNoMoreInteractions(mHandler, mLooper, mDestination);
    }

    @Test(dependsOnGroups = "sanity.channel.looper",
            dataProvider = "success or failure", dataProviderClass = Providers.class)
    public final void isOnCurrentThread(final Providers.Boolean success) {
        new StrictExpectations() {{
            mLooper.getThread();
            result = success.value() ? currentThread() : null;
        }};

        assertThat("channel", mChannel,
                is(success.value() ? onCurrentThread() : not(onCurrentThread())));
    }

    @Test(dependsOnGroups = "sanity.channel.looper", dataProvider = "messages")
    public final void hasUndeliveredMessages(final Providers.Boolean hasControlMessages,
                                             final Providers.Boolean hasUserMessages) {
        new NonStrictExpectations() {{
            mHandler.hasMessages(CONTROL, any);
            result = hasControlMessages.value();
            mHandler.hasMessages(USER, any);
            result = hasUserMessages.value();
        }};

        assertThat("channel", mChannel,
                (hasControlMessages.value() || hasUserMessages.value()) ?
                        LooperChannelMatchers.hasUndeliveredMessages() :
                        hasNoUndeliveredMessages());
    }

    @Test(groups = {"sanity", "sanity.channel", "sanity.channel.looper"},
            dataProvider = "(success, thread, messages)")
    public final void sendControlMessage(final Providers.Boolean success,
                                         final Providers.Boolean onSameThread,
                                         final Providers.Boolean messages) {
        final int message = isA(RandomControlMessage);

        new StrictExpectations(Message.class) {{
            final Message msg = Message.obtain(mHandler, (Runnable) any);
            mHandler.sendMessage(msg);
            result = success.value();
        }};

        mock(mChannel, onSameThread.value(), messages.value());

        final int delivery = success.value() ? SUCCESS : FAILURE_NO_RETRY;
        assertThat("channel send", mChannel.send(message), is(delivery));
    }

    @Test(groups = {"sanity", "sanity.channel", "sanity.channel.looper"},
            dataProvider = "success or failure", dataProviderClass = Providers.class)
    public final void sendControlMessageWithNoRetry(final Providers.Boolean success) {
        final int message = isA(RandomControlMessage);
        final int delivery = success.value() ? SUCCESS : FAILURE_NO_RETRY;

        new StrictExpectations() {{
            mDestination.send(message);
            result = delivery;
        }};

        mock(mChannel, true, false);

        assertThat("channel send", mChannel.send(message), is(delivery));
    }

    @Test(groups = {"sanity", "sanity.channel", "sanity.channel.looper"},
            dataProvider = "success or failure", dataProviderClass = Providers.class)
    public final void sendControlMessageWithRetry(final Providers.Boolean success) {
        final int message = isA(RandomControlMessage);

        new StrictExpectations() {{
            mDestination.send(message);
            result = FAILURE_CAN_RETRY;
        }};

        new StrictExpectations(Message.class) {{
            final Message msg = Message.obtain(mHandler, (Runnable) any);
            mHandler.sendMessage(msg);
            result = success.value();
        }};

        mock(mChannel, true, false);

        final int delivery = success.value() ? SUCCESS : FAILURE_NO_RETRY;
        assertThat("channel send", mChannel.send(message), is(delivery));
    }

    @Test(groups = {"sanity", "sanity.channel", "sanity.channel.looper"},
            dataProvider = "(success, thread, messages)")
    public final void sendUserMessage(final Providers.Boolean success,
                                      final Providers.Boolean onSameThread,
                                      final Providers.Boolean messages) {
        final String message = isA(RandomUserMessage);

        new StrictExpectations(Message.class) {{
            final Message msg = Message.obtain(mHandler, (Runnable) any);
            mHandler.sendMessage(msg);
            result = success.value();
        }};

        mock(mChannel, onSameThread.value(), messages.value());

        final int delivery = success.value() ? SUCCESS : FAILURE_NO_RETRY;
        assertThat("channel send", mChannel.send(message), is(delivery));
    }

    @Test(groups = {"sanity", "sanity.channel", "sanity.channel.looper"},
            dataProvider = "success or failure", dataProviderClass = Providers.class)
    public final void sendUserMessageWithNoRetry(final Providers.Boolean success) {
        final String message = isA(RandomUserMessage);
        final int delivery = success.value() ? SUCCESS : FAILURE_NO_RETRY;

        new StrictExpectations() {{
            mDestination.send(message);
            result = delivery;
        }};

        mock(mChannel, true, false);

        assertThat("channel send", mChannel.send(message), is(delivery));
    }

    @Test(groups = {"sanity", "sanity.channel", "sanity.channel.looper"},
            dataProvider = "success or failure", dataProviderClass = Providers.class)
    public final void sendUserMessageWithRetry(final Providers.Boolean success) {
        final String message = isA(RandomUserMessage);

        new StrictExpectations() {{
            mDestination.send(message);
            result = FAILURE_CAN_RETRY;
        }};

        new StrictExpectations(Message.class) {{
            final Message msg = Message.obtain(mHandler, (Runnable) any);
            mHandler.sendMessage(msg);
            result = success.value();
        }};

        mock(mChannel, true, false);

        final int delivery = success.value() ? SUCCESS : FAILURE_NO_RETRY;
        assertThat("channel send", mChannel.send(message), is(delivery));
    }

    @Test(dependsOnGroups = "sanity.channel.looper",
            dataProvider = "(immediateness, messages)")
    public final void stop(final Providers.Boolean immediately,
                           final Providers.Boolean messages) {
        if (immediately.value()) {
            new StrictExpectations() {{
                mHandler.removeMessages(CONTROL, any);
                mHandler.removeMessages(USER, any);
            }};
        }

        mock(mChannel, messages.value());

        assertThat("channel send", mChannel.stop(immediately.value()), is(!messages.value()));
    }

    @Test(groups = {"sanity", "sanity.channel", "sanity.channel.looper"},
            dataProvider = "delivery", dataProviderClass = ChannelProviders.class)
    public final void deliverControlMessage(final Providers.Value<Integer> delivery) {
        final int message = isA(RandomControlMessage);

        new StrictExpectations() {{
            mDestination.send(message);
            result = delivery.value();
        }};

        final Object token = new Object();
        final LooperChannel.Message.Control control =
                new LooperChannel.Message.Control(mHandler, token, mDestination, message);
        assertThat("message deliver", control.deliver(), is(delivery.value()));
    }

    @Test(groups = {"sanity", "sanity.channel", "sanity.channel.looper"},
            dataProvider = "delivery", dataProviderClass = ChannelProviders.class)
    public final void deliverUserMessage(final Providers.Value<Integer> delivery) {
        final String message = isA(RandomUserMessage);

        new StrictExpectations() {{
            mDestination.send(message);
            result = delivery.value();
        }};

        final Object token = new Object();
        final LooperChannel.Message.User<String> user =
                new LooperChannel.Message.User<>(mHandler, token, mDestination, message);
        assertThat("message deliver", user.deliver(), is(delivery.value()));
    }

    @Test(groups = {"sanity", "sanity.channel", "sanity.channel.looper"},
            dataProvider = "(retries, success)")
    public final void runControlMessage(final Providers.Boolean retries,
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

        final Object token = new Object();
        new LooperChannel.Message.Control(mHandler, token, mDestination, message).run();
    }

    @Test(dependsOnGroups = "sanity.channel.looper")
    public final void runControlMessageWithTooManyRetries() {
        final int message = isA(RandomControlMessage);

        new StrictExpectations() {{
            mDestination.send(message);
            times = MaximumNumberOfRetries.get();
            result = FAILURE_CAN_RETRY;
        }};

        final Object token = new Object();
        new LooperChannel.Message.Control(mHandler, token, mDestination, message).run();
    }

    @Test(groups = {"sanity", "sanity.channel", "sanity.channel.looper"},
            dataProvider = "(retries, success)")
    public final void runUserMessage(final Providers.Boolean retries,
                                     final Providers.Boolean success) {
        final String message = isA(RandomUserMessage);

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

        final Object token = new Object();
        new LooperChannel.Message.User<>(mHandler, token, mDestination, message).run();
    }

    @Test(dependsOnGroups = "sanity.channel.looper")
    public final void runUserMessageWithTooManyRetries() {
        final String message = isA(RandomUserMessage);

        new StrictExpectations() {{
            mDestination.send(message);
            times = MaximumNumberOfRetries.get();
            result = FAILURE_CAN_RETRY;
        }};

        final Object token = new Object();
        new LooperChannel.Message.User<>(mHandler, token, mDestination, message).run();
    }

    private static final RandomDataGenerator<Integer> RandomControlMessage = RandomInteger;
    private static final RandomDataGenerator<String> RandomUserMessage = RandomString;

    private static <M> void mock(@NonNull final Channel<M> channel,
                                 final boolean sameThread,
                                 final boolean undeliveredMessages) {
        new MockUp<Channel<M>>(channel) {

            @Mock
            boolean isOnCurrentThread() {
                return sameThread;
            }

            @Mock
            boolean hasUndeliveredMessages() {
                return undeliveredMessages;
            }
        };
    }

    private static <M> void mock(@NonNull final Channel<M> channel,
                                 final boolean undeliveredMessages) {
        new MockUp<Channel<M>>(channel) {
            @Mock
            boolean hasUndeliveredMessages() {
                return undeliveredMessages;
            }
        };
    }

    @NonNull
    @DataProvider(name = "(success, thread, messages)")
    private static Object[][] successAndThreadAndMessages() {
        return without(
                cartesian(
                        successOrFailure(),
                        cartesian(
                                booleans("different thread", "same thread"),
                                booleans("no undelivered messages", "with undelivered messages"))),
                /* failure, same thread, no undelivered messages*/
                new Object[]{false, true, false},
                /* success, same thread, no undelivered messages*/
                new Object[]{true, true, false}
        );
    }

    @NonNull
    @DataProvider(name = "(retries, success)")
    private static Object[][] retriesAndSuccess() {
        return cartesian(retries(), successOrFailure());
    }

    @NonNull
    @DataProvider(name = "(immediateness, messages)")
    private static Object[][] immediatenessAndMessages() {
        return cartesian(
                booleans("not immediately", "immediately"),
                booleans("no undelivered messages", "with undelivered messages"));
    }

    @NonNull
    @DataProvider(name = "messages")
    private static Object[][] messages() {
        return cartesian(
                booleans("no undelivered control messages", "with undelivered control messages"),
                booleans("no undelivered user messages", "with undelivered user messages"));
    }
}
