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

import android.actor.Captured;
import android.actor.Messenger;
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
import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.StrictExpectations;
import mockit.Verifications;

import static android.actor.Providers.booleans;
import static android.actor.Providers.cartesian;
import static android.actor.Providers.successOrFailure;
import static android.actor.messenger.LooperMessenger.MessageType.CONTROL;
import static android.actor.messenger.LooperMessenger.MessageType.USER;
import static android.actor.messenger.LooperMessengerMatchers.hasNoUndeliveredMessages;
import static android.actor.messenger.LooperMessengerMatchers.onCurrentThread;
import static android.actor.messenger.MessengerProviders.delivery;
import static java.lang.Thread.currentThread;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

public class LooperMessengerTest extends TestCase {

    @Mocked
    private Handler mAllHandlers;

    @Injectable
    private Looper mLooper;
    @Injectable
    private Messenger.Callback<String> mMessengerCallback;
    @Injectable
    private Message mMessage;

    private LooperMessenger<String> mMessenger;
    private Handler mHandler;
    private Handler.Callback mHandlerCallback;

    @BeforeMethod
    public final void setUp() {
        mHandlerCallback = Captured.as(Handler.Callback.class);

        new StrictExpectations() {{
            mHandler = new Handler(mLooper, withArgThat(is(Captured.into(mHandlerCallback))));
        }};

        mMessenger = new LooperMessenger<>(mLooper, mMessengerCallback);
    }

    @AfterMethod
    public final void tearDown() {
        new Verifications() {{
            mHandler.hasMessages(CONTROL);
            minTimes = 0;
            mHandler.hasMessages(USER);
            minTimes = 0;
            mLooper.getThread();
            minTimes = 0;
        }};

        verifyNoMoreInteractions(mAllHandlers);
        verifyNoMoreInteractions(mHandler, mLooper, mMessengerCallback);
    }

    @Test(dependsOnGroups = "sanity.messenger.looper",
            dataProvider = "success or failure", dataProviderClass = Providers.class)
    public final void isOnCurrentThread(final Providers.Boolean success) {
        new StrictExpectations() {{
            mLooper.getThread();
            result = success.value() ? currentThread() : null;
        }};

        assertThat("messenger", mMessenger,
                is(success.value() ? onCurrentThread() : not(onCurrentThread())));
    }

    @Test(dependsOnGroups = "sanity.messenger.looper", dataProvider = "messages")
    public final void hasUndeliveredMessages(final Providers.Boolean hasControlMessages,
                                             final Providers.Boolean hasUserMessages) {
        new NonStrictExpectations() {{
            mHandler.hasMessages(CONTROL);
            result = hasControlMessages.value();
            mHandler.hasMessages(USER);
            result = hasUserMessages.value();
        }};

        assertThat("messenger", mMessenger,
                (hasControlMessages.value() || hasUserMessages.value()) ?
                        LooperMessengerMatchers.hasUndeliveredMessages() :
                        hasNoUndeliveredMessages());
    }

    @Test(groups = {"sanity", "sanity.messenger", "sanity.messenger.looper"},
            dataProvider = "(success, thread, messages)")
    public final void sendControlMessage(final Providers.Boolean success,
                                         final Providers.Boolean onSameThread,
                                         final Providers.Boolean messages) {
        final Integer message = isA(RandomControlMessage);

        new NonStrictExpectations(mMessenger) {{
            mMessenger.isOnCurrentThread();
            result = onSameThread.value();
            mMessenger.hasUndeliveredMessages();
            result = messages.value();
        }};

        if (onSameThread.value() && !messages.value()) {
            new StrictExpectations() {{
                mMessengerCallback.onMessage(message);
                result = success.value();
            }};
        } else {
            new StrictExpectations() {{
                final Message msg = mHandler.obtainMessage(CONTROL, message, anyInt);
                mHandler.sendMessage(msg);
                result = success.value();
            }};
        }

        assertThat("messenger send", mMessenger.send(message), is(success.value()));
    }

    @Test(groups = {"sanity", "sanity.messenger", "sanity.messenger.looper"},
            dataProvider = "(delivery, thread, messages)")
    public final void sendUserMessageWithNoDelay(final Providers.Value<Integer> delivery,
                                                 final Providers.Boolean onSameThread,
                                                 final Providers.Boolean messages) {
        final String message = isA(RandomUserMessage);
        final long noDelay = isA(RandomNoDelay);

        new NonStrictExpectations(mMessenger) {{
            mMessenger.isOnCurrentThread();
            result = onSameThread.value();
            mMessenger.hasUndeliveredMessages();
            result = messages.value();
        }};

        if (onSameThread.value() && !messages.value()) {
            new StrictExpectations() {{
                mMessengerCallback.onMessage(message);
                result = delivery.value();
            }};

            if (delivery.value() == Messenger.Delivery.FAILURE_CAN_RETRY) {
                new StrictExpectations() {{
                    final Message msg = mHandler.obtainMessage(USER, message);
                    mHandler.sendMessage(msg);
                    result = delivery.value() == Messenger.Delivery.SUCCESS;
                }};
            }
        } else {
            new StrictExpectations() {{
                final Message msg = mHandler.obtainMessage(USER, message);
                mHandler.sendMessage(msg);
                result = delivery.value() == Messenger.Delivery.SUCCESS;
            }};
        }

        final boolean success = delivery.value() == Messenger.Delivery.SUCCESS;
        assertThat("messenger send", mMessenger.send(message, noDelay), is(success));
    }

    @Test(groups = {"sanity", "sanity.messenger", "sanity.messenger.looper"},
            dataProvider = "success or failure", dataProviderClass = Providers.class)
    public final void sendUserMessageWithDelay(final Providers.Boolean success) {
        final String message = isA(RandomUserMessage);
        final long delay = isA(RandomDelay);

        new StrictExpectations() {{
            final Message msg = mHandler.obtainMessage(USER, message);
            mHandler.sendMessageDelayed(msg, delay);
            result = success.value();
        }};

        assertThat("messenger send", mMessenger.send(message, delay), is(success.value()));

        new Verifications() {{
            mLooper.getThread();
            times = 0;
        }};
    }

    @Test(dependsOnGroups = "sanity.messenger.looper",
            dataProvider = "(immediateness, messages)")
    public final void stop(final Providers.Boolean immediately,
                           final Providers.Boolean messages) {
        if (immediately.value()) {
            new StrictExpectations() {{
                mHandler.removeMessages(CONTROL);
                mHandler.removeMessages(USER);
            }};
        }

        new StrictExpectations(mMessenger) {{
            mMessenger.hasUndeliveredMessages();
            result = messages.value();
        }};

        assertThat("messenger send", mMessenger.stop(immediately.value()), is(!messages.value()));
    }

    @Test(groups = {"sanity", "sanity.messenger", "sanity.messenger.looper"},
            dataProvider = "success or failure", dataProviderClass = Providers.class)
    public final void handleControlMessage(final Providers.Boolean success) {
        final int message = isA(RandomControlMessage);

        new StrictExpectations() {{
            mMessage.what = CONTROL;
            mMessage.arg1 = message;
            mMessengerCallback.onMessage(message);
            result = success.value();
        }};

        assertThat("messenger send", mHandlerCallback.handleMessage(mMessage), is(success.value()));
    }

    @Test(groups = {"sanity", "sanity.messenger", "sanity.messenger.looper"},
            dataProvider = "delivery", dataProviderClass = MessengerProviders.class)
    public final void handleUserMessage(final Providers.Value<Integer> delivery) {
        final String message = isA(RandomUserMessage);

        new StrictExpectations() {{
            mMessage.what = USER;
            mMessage.obj = message;
            mMessengerCallback.onMessage(message);
            result = delivery.value();
        }};

        final boolean success = delivery.value() == Messenger.Delivery.SUCCESS;
        assertThat("messenger send", mHandlerCallback.handleMessage(mMessage), is(success));
    }

    @Test(dependsOnGroups = "sanity.messenger.looper")
    public final void handleUnknownMessage() {
        final int type = isA(RandomInteger.thatIs(differentFrom(CONTROL, USER)));

        new StrictExpectations() {{
            mMessage.what = type;
        }};

        assertThat("messenger send", mHandlerCallback.handleMessage(mMessage), is(false));
    }

    private static final RandomDataGenerator<Integer> RandomControlMessage = RandomInteger;
    private static final RandomDataGenerator<String> RandomUserMessage = RandomString;
    private static final RandomDataGenerator<Long> RandomDelay = RandomLong.thatIs(Positive);
    private static final RandomDataGenerator<Long> RandomNoDelay = RandomLong.thatIs(NonPositive);

    @NonNull
    @DataProvider(name = "(success, thread, messages)")
    private static Object[][] successAndThreadAndMessages() {
        return cartesian(
                successOrFailure(),
                cartesian(
                        booleans("different thread", "same thread"),
                        booleans("no undelivered messages", "with undelivered messages")));
    }

    @NonNull
    @DataProvider(name = "(delivery, thread, messages)")
    private static Object[][] deliveryAndThreadAndMessages() {
        return cartesian(
                delivery(),
                cartesian(
                        booleans("different thread", "same thread"),
                        booleans("no undelivered messages", "with undelivered messages")));
    }

    @NonNull
    @DataProvider(name = "messages")
    private static Object[][] messages() {
        return cartesian(
                booleans("no undelivered control messages", "with undelivered control messages"),
                booleans("no undelivered user messages", "with undelivered user messages"));
    }

    @NonNull
    @DataProvider(name = "(immediateness, messages)")
    private static Object[][] immediatenessAndMessages() {
        return cartesian(
                booleans("not immediately", "immediately"),
                booleans("no undelivered messages", "with undelivered messages"));
    }
}
