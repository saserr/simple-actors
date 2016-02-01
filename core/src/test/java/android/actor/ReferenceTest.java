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

package android.actor;

import android.actor.channel.Send;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import mockit.Injectable;
import mockit.Mocked;
import mockit.StrictExpectations;

import static android.actor.Channel.Delivery.SUCCESS;
import static android.actor.ReferenceMatchers.hasName;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class ReferenceTest extends TestCase {

    @Mocked
    private Send<?> mAllSends;

    @Injectable
    private System mSystem;
    @Injectable
    private Channel<Message> mChannel;
    @Injectable
    private Message mMessage;

    private Actor.Name mName;
    private Reference<Message> mReference;

    @BeforeMethod
    public final void setUp() {
        mName = new Actor.Name(null, a(RandomName));
        mReference = new Reference<>(mSystem, mName, mChannel);
    }

    @AfterMethod
    public final void tearDown() {
        verifyNoMoreInteractions(mAllSends);
        verifyNoMoreInteractions(mSystem, mChannel, mMessage);
    }

    @Test(dependsOnGroups = "sanity.reference")
    public final void getName() {
        assertThat("reference", mReference, hasName(mName));
    }

    @Test(groups = {"sanity", "sanity.reference"},
            dataProvider = "delivery", dataProviderClass = ChannelProviders.class)
    public final void tell(final Providers.Value<Integer> delivery) {
        new StrictExpectations() {{
            Send.withRetries(mChannel, mMessage);
            result = delivery.value();
        }};

        final boolean success = delivery.value() == SUCCESS;
        assertThat("reference tell", mReference.tell(mMessage), is(success));
    }

    @Test(groups = {"sanity", "sanity.reference"},
            dataProvider = "success or failure", dataProviderClass = Providers.class)
    public final void start(final Providers.Boolean success) {
        new StrictExpectations() {{
            mSystem.start(mName);
            result = success.value();
        }};

        assertThat("reference start", mReference.start(), is(success.value()));
    }

    @Test(dependsOnGroups = "sanity.reference",
            dataProvider = "success or failure", dataProviderClass = Providers.class)
    public final void pause(final Providers.Boolean success) {
        new StrictExpectations() {{
            mSystem.pause(mName);
            result = success.value();
        }};

        assertThat("reference pause", mReference.pause(), is(success.value()));
    }

    @Test(dependsOnGroups = "sanity.reference",
            dataProvider = "success or failure", dataProviderClass = Providers.class)
    public final void stop(final Providers.Boolean success) {
        new StrictExpectations() {{
            mSystem.stop(mName);
            result = success.value();
        }};

        assertThat("reference stop", mReference.stop(), is(success.value()));
    }

    private static final RandomDataGenerator<String> RandomName = RandomString;

    private interface Message {}
}
