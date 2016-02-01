/*
 * Copyright 2016 the original author or authors
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
import android.actor.Configuration;
import android.actor.Providers;
import android.actor.TestCase;
import android.actor.util.Retry;
import android.support.annotation.NonNull;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import mockit.Injectable;
import mockit.StrictExpectations;

import static android.actor.Channel.Delivery.ERROR;
import static android.actor.Channel.Delivery.FAILURE;
import static android.actor.Channel.Delivery.SUCCESS;
import static android.actor.Providers.booleans;
import static android.actor.Providers.cartesian;
import static android.actor.Providers.successOrFailure;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.testng.Assert.fail;

public class SendTest extends TestCase {

    @Injectable
    private Channel<Message> mChannel;
    @Injectable
    private Message mMessage;

    private Send<Message> mSend;

    @BeforeMethod
    public final void setUp() {
        mSend = new Send<>(mChannel, mMessage);
    }

    @AfterMethod
    public final void tearDown() {
        verifyNoMoreInteractions(mChannel, mMessage);
    }

    @Test(groups = {"sanity", "sanity.send"},
            dataProvider = "delivery", dataProviderClass = ChannelProviders.class)
    public final void execute(final Providers.Value<Integer> delivery) {
        new StrictExpectations() {{
            mChannel.send(mMessage);
            result = delivery.value();
        }};

        switch (delivery.value()) {
            case SUCCESS:
                assertThat("send", mSend.execute(), is(Retry.SUCCESS));
                break;
            case FAILURE:
                assertThat("send", mSend.execute(), is(Retry.AGAIN));
                break;
            case ERROR:
                assertThat("send", mSend.execute(), is(Retry.FAILURE));
                break;
            default:
                fail("Unknown delivery: " + delivery.value()); //NON-NLS
        }
    }

    @Test(groups = {"sanity", "sanity.send"}, dataProvider = "(retries, success)")
    public final void withRetries(final Providers.Boolean retries,
                                  final Providers.Boolean success) {
        if (retries.value()) {
            new StrictExpectations() {{
                mChannel.send(mMessage);
                result = FAILURE;
            }};
        }

        new StrictExpectations() {{
            mChannel.send(mMessage);
            result = success.value() ? SUCCESS : ERROR;
        }};

        final int result = success.value() ? Retry.SUCCESS : Retry.FAILURE;
        assertThat("send with retries", Send.withRetries(mChannel, mMessage), is(result));
    }

    @Test(dependsOnGroups = "sanity.send")
    public final void withTooManyRetries() {
        new StrictExpectations() {{
            mChannel.send(mMessage);
            times = Configuration.MaximumNumberOfRetries.get();
            result = FAILURE;
        }};

        assertThat("send with retries", Send.withRetries(mChannel, mMessage), is(Retry.AGAIN));
    }

    @NonNull
    @DataProvider(name = "(retries, success)")
    public static Object[][] retriesAndSuccess() {
        return cartesian(booleans("no retries", "with retries"), successOrFailure());
    }

    private interface Message {}
}
