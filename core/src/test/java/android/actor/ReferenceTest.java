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

import android.actor.channel.Mailbox;
import android.actor.channel.Send;
import android.actor.executor.Executable;
import android.support.annotation.NonNull;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.concurrent.atomic.AtomicReference;

import mockit.Injectable;
import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.StrictExpectations;
import mockit.Verifications;

import static android.actor.Channel.Delivery.FAILURE_CAN_RETRY;
import static android.actor.Channel.Delivery.FAILURE_NO_RETRY;
import static android.actor.Channel.Delivery.SUCCESS;
import static android.actor.ReferenceMatchers.hasName;
import static android.actor.ReferenceMatchers.stopped;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

@SuppressWarnings("unchecked")
public class ReferenceTest extends TestCase {

    @Mocked
    private Mailbox<Reference.Message<String>> mAllMailboxes;
    @Mocked
    private Send<?> mAllSends;

    @Injectable
    private Context mContext;
    @Injectable
    private Actor<String> mActor;
    @Injectable
    private Reference.Callback mCallback;
    @Injectable
    private Executor mExecutor;
    @Injectable
    private Channel.Factory mChannelFactory;

    private Actor.Name mName;
    private Mailbox<Reference.Message<String>> mMailbox;
    private Channel<Reference.Message<String>> mChannel;
    private Reference<String> mReference;

    @BeforeMethod
    public final void setUp() {
        mName = new Actor.Name(null, a(RandomName));
        new NonStrictExpectations() {{
            mContext.getName();
            result = mName;
        }};

        mReference = new Reference<>(mContext, mActor, mCallback);
        mChannel = Captured.as(Channel.class);

        new Verifications() {{
            mContext.getName();
            mMailbox = new Mailbox<>(withArgThat(is(Captured.into(mChannel))));
        }};
    }

    @AfterMethod
    public final void tearDown() {
        verifyNoMoreInteractions(mAllMailboxes, mAllSends);
        verifyNoMoreInteractions(mContext, mActor, mCallback, mExecutor, mChannelFactory);
    }

    @Test(dependsOnGroups = "sanity.reference")
    public final void getName() {
        assertThat("reference", mReference, hasName(mName));
    }

    @Test(groups = {"sanity", "sanity.reference"},
            dataProvider = "success or failure", dataProviderClass = Providers.class)
    public final void start(final Providers.Boolean success) {
        new StrictExpectations() {{
            mActor.postStart(mContext, mReference);
            final Executor.Submission submission = mExecutor.submit(mReference);
            result = success.value() ? submission : null;
        }};

        assertThat("reference start", mReference.start(mExecutor), is(success.value()));
    }

    @Test(dependsOnGroups = "sanity.reference")
    public final void actorPostStartFails() {
        new StrictExpectations(mReference) {{
            mActor.postStart(mContext, mReference);
            result = new RuntimeException("test failure");
            mReference.stop(true);
        }};

        assertThat("reference start", mReference.start(mExecutor), is(false));
    }

    @Test(groups = {"sanity", "sanity.reference"}, dependsOnMethods = "start",
            dataProvider = "success or failure", dataProviderClass = Providers.class)
    public final void attach(final Providers.Boolean success) {
        new StrictExpectations() {{
            mActor.postStart(mContext, mReference);
            mExecutor.submit(mReference);
            mMailbox.attach(mChannelFactory);
            result = success.value();
        }};

        assertThat("reference start", mReference.start(mExecutor), is(true));
        assertThat("reference attach", mReference.attach(mChannelFactory), is(success.value()));
    }

    @Test(dependsOnGroups = "sanity.reference",
            expectedExceptions = UnsupportedOperationException.class,
            expectedExceptionsMessageRegExp = "Actor\\(.*\\) hasn't been started!")
    public final void attachBeforeStart() {
        mReference.attach(mChannelFactory);
    }

    @Test(groups = {"sanity", "sanity.reference"}, dependsOnMethods = "start",
            dataProvider = "success or failure", dataProviderClass = Providers.class)
    public final void tell(final Providers.Boolean success) {
        final String message = isA(RandomMessage);

        new StrictExpectations() {{
            mActor.postStart(mContext, mReference);
            mExecutor.submit(mReference);
            Send.withRetries(mMailbox, new Reference.Message.User<>(message));
            result = success.value();
        }};

        assertThat("reference start", mReference.start(mExecutor), is(true));
        assertThat("reference tell", mReference.tell(message), is(success.value()));
    }

    @Test(groups = {"sanity", "sanity.reference"},
            dataProvider = "success or failure", dataProviderClass = Providers.class)
    public final void tellBeforeStart(final Providers.Boolean success) {
        final String message = isA(RandomMessage);

        new StrictExpectations() {{
            Send.withRetries(mMailbox, new Reference.Message.User<>(message));
            result = success.value();
        }};

        assertThat("reference tell", mReference.tell(message), is(success.value()));
    }

    @Test(groups = {"sanity", "sanity.reference"}, dependsOnMethods = "start")
    public final void receiveMessage() {
        final String message = isA(RandomMessage);

        new StrictExpectations() {{
            mActor.postStart(mContext, mReference);
            mExecutor.submit(mReference);
            mActor.onMessage(message);
        }};

        assertThat("reference start", mReference.start(mExecutor), is(true));
        assertThat("channel send", mChannel.send(new Reference.Message.User<>(message)), is(SUCCESS));
    }

    @Test(dependsOnGroups = "sanity.reference",
            dataProvider = "success or failure", dataProviderClass = Providers.class)
    public final void actorOnMessageFails(final Providers.Boolean success) {
        final String message = isA(RandomMessage);

        new StrictExpectations(mReference) {{
            mActor.postStart(mContext, mReference);
            mExecutor.submit(mReference);
            mActor.onMessage(message);
            result = new RuntimeException("test failure");
            mReference.stop(true);
            result = success.value();
        }};

        assertThat("reference start", mReference.start(mExecutor), is(true));
        final int failure = FAILURE_NO_RETRY;
        assertThat("channel send", mChannel.send(new Reference.Message.User<>(message)), is(failure));
    }

    @Test(dependsOnGroups = "sanity.reference")
    public final void detach() {
        new StrictExpectations() {{
            mActor.postStart(mContext, mReference);
            mExecutor.submit(mReference);
            mMailbox.detach();
        }};

        assertThat("reference start", mReference.start(mExecutor), is(true));
        assertThat("reference detach", mReference.detach(), is(true));
    }

    @Test(dependsOnGroups = "sanity.reference")
    public final void detachBeforeStart() {
        assertThat("reference detach", mReference.detach(), is(true));
    }

    @Test(dependsOnGroups = "sanity.reference",
            dataProvider = "success or failure", dataProviderClass = Providers.class)
    public final void pause(final Providers.Boolean success) {
        new StrictExpectations() {{
            mActor.postStart(mContext, mReference);
            mExecutor.submit(mReference);
            Send.withRetries(mMailbox, Reference.Message.Control.<String>pause());
            result = success.value();
        }};

        assertThat("reference start", mReference.start(mExecutor), is(true));
        assertThat("reference pause", mReference.pause(), is(success.value()));
    }

    @Test(dependsOnGroups = "sanity.reference")
    public final void pauseBeforeStart() {
        assertThat("reference pause", mReference.pause(), is(true));
    }

    @Test(dependsOnGroups = "sanity.reference",
            dataProvider = "success or failure", dataProviderClass = Providers.class)
    public final void receivePause(final Providers.Boolean success) {
        new StrictExpectations() {{
            mActor.postStart(mContext, mReference);
            final Executor.Submission submission = mExecutor.submit(mReference);
            notNull(submission).stop();
            result = success.value();
        }};

        assertThat("reference start", mReference.start(mExecutor), is(true));
        final int delivery = success.value() ? SUCCESS : FAILURE_CAN_RETRY;
        assertThat("channel send(PAUSE)", mChannel.send(Reference.Message.Control.<String>pause()), is(delivery));
    }

    @Test(dependsOnGroups = "sanity.reference", dependsOnMethods = "pause")
    public final void doubleReceivePause() {
        new StrictExpectations() {{
            mActor.postStart(mContext, mReference);
            final Executor.Submission submission = mExecutor.submit(mReference);
            notNull(submission).stop();
            result = true;
        }};

        assertThat("reference start", mReference.start(mExecutor), is(true));
        assertThat("channel send(PAUSE)", mChannel.send(Reference.Message.Control.<String>pause()), is(SUCCESS));
        assertThat("channel send(PAUSE)", mChannel.send(Reference.Message.Control.<String>pause()), is(SUCCESS));
    }

    @Test(dependsOnGroups = "sanity.reference", dependsOnMethods = "pause")
    public final void startAfterPause() {
        new StrictExpectations() {{
            mActor.postStart(mContext, mReference);
            final Executor.Submission submission = mExecutor.submit(mReference);
            Send.withRetries(mMailbox, Reference.Message.Control.<String>pause());
            result = true;
            notNull(submission).stop();
            result = true;
            mExecutor.submit(mReference);
        }};

        assertThat("reference start", mReference.start(mExecutor), is(true));
        assertThat("reference pause", mReference.pause(), is(true));
        assertThat("reference restart", mReference.start(mExecutor), is(true));
    }

    @Test(dependsOnGroups = "sanity.reference",
            dataProvider = "success or failure", dataProviderClass = Providers.class)
    public final void stopWhenAttached(final Providers.Boolean success) {
        new StrictExpectations() {{
            mActor.postStart(mContext, mReference);
            mExecutor.submit(mReference);
            mMailbox.isAttached();
            result = true;
            Send.withRetries(mMailbox, Reference.Message.Control.<String>stop());
            result = success.value();
        }};

        assertThat("reference start", mReference.start(mExecutor), is(true));
        assertThat("reference stop", mReference.stop(), is(success.value()));
        assertThat("reference", mReference, is(not(stopped())));
    }

    @Test(dependsOnGroups = "sanity.reference",
            dataProvider = "success or failure", dataProviderClass = Providers.class)
    public final void stopWhenUnattached(final Providers.Boolean success) {
        final AtomicReference<Executor.Submission> submission = new AtomicReference<>();
        new StrictExpectations() {{
            mActor.postStart(mContext, mReference);
            submission.set(mExecutor.submit(mReference));
            mMailbox.isAttached();
            result = false;
            mMailbox.stop(false);
            result = success.value();
        }};

        if (success.value()) {
            new StrictExpectations() {{
                submission.get().stop();
                result = true;
                mActor.preStop();
                mCallback.onStop(mName);
            }};
        }

        assertThat("reference start", mReference.start(mExecutor), is(true));
        assertThat("reference stop", mReference.stop(), is(success.value()));
        assertThat("reference", mReference, is(success.value() ? stopped() : not(stopped())));
    }

    @Test(dependsOnGroups = "sanity.reference",
            dataProvider = "success or failure", dataProviderClass = Providers.class)
    public final void stopBeforeStart(final Providers.Boolean success) {
        new StrictExpectations(mReference) {{
            mReference.stop(true);
            result = success.value();
        }};

        assertThat("reference stop", mReference.stop(), is(success.value()));
    }

    @Test(dependsOnGroups = "sanity.reference", dependsOnMethods = "stopBeforeStart")
    public final void doubleStop() {
        new StrictExpectations() {{
            mCallback.onStop(mName);
        }};

        assertThat("reference stop", mReference.stop(), is(true));
        assertThat("reference stop", mReference.stop(), is(true));
        assertThat("reference", mReference, is(stopped()));
    }

    @Test(dependsOnGroups = "sanity.reference",
            dataProvider = "success or failure", dataProviderClass = Providers.class)
    public final void receiveStop(final Providers.Boolean success) {
        new StrictExpectations(mReference) {{
            mActor.postStart(mContext, mReference);
            mExecutor.submit(mReference);
            mReference.stop(true);
            result = success.value();
        }};

        assertThat("reference start", mReference.start(mExecutor), is(true));
        final int delivery = success.value() ? SUCCESS : FAILURE_CAN_RETRY;
        assertThat("channel send(STOP)", mChannel.send(Reference.Message.Control.<String>stop()), is(delivery));
    }

    @Test(dependsOnGroups = "sanity.reference",
            expectedExceptions = UnsupportedOperationException.class,
            expectedExceptionsMessageRegExp = "Actor\\(.*\\) is stopped!")
    public final void startWhileStopping() {
        new StrictExpectations() {{
            mActor.postStart(mContext, mReference);
            mExecutor.submit(mReference);
            mMailbox.isAttached();
            result = true;
            Send.withRetries(mMailbox, Reference.Message.Control.<String>stop());
            result = true;
        }};

        assertThat("reference start", mReference.start(mExecutor), is(true));
        assertThat("reference stop", mReference.stop(), is(true));
        mReference.start(mExecutor);
    }

    @Test(dependsOnGroups = "sanity.reference", dependsOnMethods = "stopBeforeStart",
            expectedExceptions = UnsupportedOperationException.class,
            expectedExceptionsMessageRegExp = "Actor\\(.*\\) is stopped!")
    public final void startAfterStop() {
        new StrictExpectations() {{
            mCallback.onStop(mName);
        }};

        assertThat("reference stop", mReference.stop(), is(true));
        mReference.start(mExecutor);
    }

    @Test(dependsOnGroups = "sanity.reference",
            expectedExceptions = UnsupportedOperationException.class,
            expectedExceptionsMessageRegExp = "Actor\\(.*\\) is stopped!")
    public final void attachWhileStopping() {
        new StrictExpectations() {{
            mActor.postStart(mContext, mReference);
            mExecutor.submit(mReference);
            mMailbox.isAttached();
            result = true;
            Send.withRetries(mMailbox, Reference.Message.Control.<String>stop());
            result = true;
        }};

        assertThat("reference start", mReference.start(mExecutor), is(true));
        assertThat("reference stop", mReference.stop(), is(true));
        mReference.attach(mChannelFactory);
    }

    @Test(dependsOnGroups = "sanity.reference", dependsOnMethods = "stopBeforeStart",
            expectedExceptions = UnsupportedOperationException.class,
            expectedExceptionsMessageRegExp = "Actor\\(.*\\) is stopped!")
    public final void attachAfterStop() {
        new StrictExpectations() {{
            mCallback.onStop(mName);
        }};

        assertThat("reference stop", mReference.stop(), is(true));
        mReference.attach(mChannelFactory);
    }

    @Test(dependsOnGroups = "sanity.reference",
            expectedExceptions = UnsupportedOperationException.class,
            expectedExceptionsMessageRegExp = "Actor\\(.*\\) is stopped!")
    public final void tellWhileStopping() {
        new StrictExpectations() {{
            mActor.postStart(mContext, mReference);
            mExecutor.submit(mReference);
            mMailbox.isAttached();
            result = true;
            Send.withRetries(mMailbox, Reference.Message.Control.<String>stop());
            result = true;
        }};

        assertThat("reference start", mReference.start(mExecutor), is(true));
        assertThat("reference stop", mReference.stop(), is(true));
        mReference.tell(a(RandomMessage));
    }

    @Test(dependsOnGroups = "sanity.reference", dependsOnMethods = "stopBeforeStart",
            expectedExceptions = UnsupportedOperationException.class,
            expectedExceptionsMessageRegExp = "Actor\\(.*\\) is stopped!")
    public final void tellAfterStop() {
        new StrictExpectations() {{
            mCallback.onStop(mName);
        }};

        assertThat("reference stop", mReference.stop(), is(true));
        mReference.tell(a(RandomMessage));
    }

    @Test(dependsOnGroups = "sanity.reference", dependsOnMethods = "stopBeforeStart")
    public final void detachAfterStop() {
        new StrictExpectations() {{
            mCallback.onStop(mName);
        }};

        assertThat("reference stop", mReference.stop(), is(true));
        assertThat("reference detach", mReference.detach(), is(true));
    }

    @Test(dependsOnGroups = "sanity.reference", dependsOnMethods = "stopWhenAttached",
            dataProvider = "success or failure", dataProviderClass = Providers.class)
    public final void detachAfterStartAndStop(final Providers.Boolean success) {
        new StrictExpectations() {{
            mActor.postStart(mContext, mReference);
            mExecutor.submit(mReference);
            mMailbox.isAttached();
            result = true;
            Send.withRetries(mMailbox, Reference.Message.Control.<String>stop());
            result = true;
            mMailbox.stop(true);
            result = success.value();
        }};

        assertThat("reference start", mReference.start(mExecutor), is(true));
        assertThat("reference stop", mReference.stop(), is(true));
        assertThat("reference detach", mReference.detach(), is(success.value()));
    }

    @Test(dependsOnGroups = "sanity.reference", dependsOnMethods = "stopBeforeStart",
            expectedExceptions = UnsupportedOperationException.class,
            expectedExceptionsMessageRegExp = "Actor\\(.*\\) is stopped!")
    public final void pauseAfterStop() {
        new StrictExpectations() {{
            mCallback.onStop(mName);
        }};

        assertThat("reference stop", mReference.stop(), is(true));
        mReference.pause();
    }

    @Test(dependsOnGroups = "sanity.reference",
            dataProvider = "success or failure", dataProviderClass = Providers.class)
    public final void nonImmediateStop(final Providers.Boolean success) {
        new StrictExpectations(mReference) {{
            mReference.stop();
            result = success.value();
        }};

        assertThat("reference stop", mReference.stop(false), is(success.value()));
        assertThat("reference", mReference, is(not(stopped())));
    }

    @Test(dependsOnGroups = "sanity.reference",
            dataProvider = "success or failure", dataProviderClass = Providers.class)
    public final void immediateStop(final Providers.Boolean success) {
        new StrictExpectations() {{
            mActor.postStart(mContext, mReference);
            final Executor.Submission submission = mExecutor.submit(mReference);
            notNull(submission).stop();
            result = success.value();
            mActor.preStop();
            times = success.value() ? 1 : 0;
            mCallback.onStop(mName);
            times = success.value() ? 1 : 0;
        }};

        assertThat("reference start", mReference.start(mExecutor), is(true));
        assertThat("reference stop", mReference.stop(true), is(success.value()));
        assertThat("reference", mReference, is(success.value() ? stopped() : not(stopped())));
    }

    @Test(dependsOnGroups = "sanity.reference")
    public final void immediateStopBeforeStart() {
        new StrictExpectations() {{
            mCallback.onStop(mName);
        }};

        assertThat("reference stop", mReference.stop(true), is(true));
        assertThat("reference", mReference, is(stopped()));
    }

    @Test(dependsOnGroups = "sanity.reference")
    public final void actorPreStopFails() {
        new StrictExpectations() {{
            mActor.postStart(mContext, mReference);
            final Executor.Submission submission = mExecutor.submit(mReference);
            notNull(submission).stop();
            result = true;
            mActor.preStop();
            result = new RuntimeException("test failure");
            mCallback.onStop(mName);
        }};

        assertThat("reference start", mReference.start(mExecutor), is(true));
        assertThat("reference stop", mReference.stop(true), is(true));
        assertThat("reference", mReference, is(stopped()));
    }

    @Test(dependsOnGroups = "sanity.reference")
    public final void receiveUnknownControlMessage() {
        new StrictExpectations() {{
            mActor.postStart(mContext, mReference);
            mExecutor.submit(mReference);
        }};

        assertThat("reference start", mReference.start(mExecutor), is(true));
        assertThat("channel send(unknown)", mChannel.send(new Reference.Message.Control<String>(0)), is(FAILURE_NO_RETRY));
    }

    @Test(groups = {"sanity", "sanity.reference"}, dependsOnMethods = "receiveMessage")
    public final void noDirectCallCycles() {
        final String message = isA(RandomMessage);
        final Channel<Reference.Message<String>> channel = Captured.as(Channel.class);
        final Actor<String> actor = new Actor<String>() {
            @Override
            protected void onMessage(@NonNull final String value) {
                final int failure = FAILURE_CAN_RETRY;
                assertThat("channel send", channel.send(new Reference.Message.User<>(message)), is(failure));
            }
        };

        new StrictExpectations() {{
            mContext.getName();
            result = mName;
            new Mailbox<>(withArgThat(is(Captured.into(channel))));
            mExecutor.submit((Executable) any);
        }};

        final Reference<String> reference = new Reference<>(mContext, actor, mCallback);
        assertThat("reference start", reference.start(mExecutor), is(true));
        assertThat("channel send", channel.send(new Reference.Message.User<>(message)),
                is(SUCCESS));
        assertThat("channel send", channel.send(new Reference.Message.User<>(message)), is(SUCCESS));
    }

    private static final RandomDataGenerator<String> RandomName = RandomString;
    private static final RandomDataGenerator<String> RandomMessage = RandomString;
}
