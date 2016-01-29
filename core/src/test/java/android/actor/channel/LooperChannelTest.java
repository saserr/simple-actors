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
import static android.actor.Providers.booleans;
import static android.actor.Providers.cartesian;
import static android.actor.Providers.successOrFailure;
import static android.actor.Providers.without;
import static android.actor.channel.LooperChannelMatchers.hasNoUndeliveredMessages;
import static android.actor.channel.LooperChannelMatchers.onCurrentThread;
import static java.lang.Thread.currentThread;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

public class LooperChannelTest extends TestCase {

    @Mocked
    private Handler mAllHandlers;
    @Mocked
    private Send<?> mAllSends;

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
            mHandler.hasMessages(LooperChannel.Deliver.Type, any);
            minTimes = 0;
            mLooper.getThread();
            minTimes = 0;
        }};

        verifyNoMoreInteractions(mAllHandlers, mAllSends);
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
    public final void hasUndeliveredMessages(final Providers.Boolean messages) {
        new NonStrictExpectations() {{
            mHandler.hasMessages(LooperChannel.Deliver.Type, any);
            result = messages.value();
        }};

        assertThat("channel", mChannel,
                messages.value() ?
                        LooperChannelMatchers.hasUndeliveredMessages() :
                        hasNoUndeliveredMessages());
    }

    @Test(groups = {"sanity", "sanity.channel", "sanity.channel.looper"},
            dataProvider = "(success, thread, messages)")
    public final void sendMessage(final Providers.Boolean success,
                                  final Providers.Boolean onSameThread,
                                  final Providers.Boolean messages) {
        final String message = isA(RandomMessage);

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
    public final void sendMessageNoRetry(final Providers.Boolean success) {
        final String message = isA(RandomMessage);
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
    public final void sendMessageWithRetry(final Providers.Boolean success) {
        final String message = isA(RandomMessage);

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
                mHandler.removeMessages(LooperChannel.Deliver.Type, any);
            }};
        }

        mock(mChannel, messages.value());

        assertThat("channel send", mChannel.stop(immediately.value()), is(!messages.value()));
    }

    @Test(groups = {"sanity", "sanity.channel", "sanity.channel.looper"})
    public final void runMessage() {
        final String message = isA(RandomMessage);

        new StrictExpectations() {{
            Send.withRetries(mDestination, message);
        }};

        new LooperChannel.Deliver<>(mDestination, message).run();
    }

    private static final RandomDataGenerator<String> RandomMessage = RandomString;

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
                                messages())),
                /* failure, same thread, no undelivered messages*/
                new Object[]{false, true, false},
                /* success, same thread, no undelivered messages*/
                new Object[]{true, true, false}
        );
    }

    @NonNull
    @DataProvider(name = "(immediateness, messages)")
    private static Object[][] immediatenessAndMessages() {
        return cartesian(
                booleans("not immediately", "immediately"),
                messages());
    }

    @NonNull
    @DataProvider(name = "messages")
    private static Object[][] messages() {
        return booleans("no undelivered messages", "with undelivered messages");
    }
}
