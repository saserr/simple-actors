/*
 * Copyright 2014 the original author or authors
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

import android.actor.util.CurrentThreadExecutor;
import android.support.annotation.NonNull;
import android.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Collections.unmodifiableList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

public class ActorTest extends TestCase {

    private Executor mExecutor;
    private System mSystem;

    @Override
    public final void setUp() throws Exception {
        super.setUp();

        mExecutor = new CurrentThreadExecutor();
        mSystem = new System(mExecutor);
    }

    @Override
    public final void tearDown() throws Exception {
        mSystem.stop(true);
        mSystem = null;

        mExecutor.stop();
        mExecutor = null;

        super.tearDown();
    }

    public final void testStart() {
        final MockActor<Integer> actor = new MockActor<>();
        final Reference<Integer> reference = mSystem.with(isA(RandomString), actor);

        final List<Pair<System, Reference<Integer>>> postStarts = actor.getPostStarts();
        assertThat("number of actor post start invocations", postStarts.size(), is(1));

        final Pair<System, Reference<Integer>> postStart = postStarts.get(0);
        assertThat("actor post start system", postStart.first, is(mSystem));
        assertThat("actor post start reference", postStart.second, is(reference));
    }

    public final void testTell() {
        final MockActor<Integer> actor = new MockActor<>();
        final Reference<Integer> reference = mSystem.with(isA(RandomString), actor);

        final int expected = isA(RandomInteger);
        assertThat("actor tell", reference.tell(expected), is(true));

        final List<Pair<System, Integer>> onMessages = actor.getOnMessages();
        assertThat("number of the actor on message invocations", onMessages.size(), is(1));

        final Pair<System, Integer> onMessage = onMessages.get(0);
        assertThat("actor received system", onMessage.first, is(mSystem));
        assertThat("actor received message", onMessage.second, is(expected));
    }

    public final void testPause() {
        final MockActor<Integer> actor = new MockActor<>();
        final Reference<Integer> reference = mSystem.with(isA(RandomString), actor);

        assertThat("actor pause", reference.pause(), is(true));
        assertThat("actor is stopped", reference.isStopped(), is(false));

        final List<Pair<System, Reference<Integer>>> postStarts = actor.getPostStarts();
        assertThat("number of actor post start invocations", postStarts.size(), is(1));

        final Pair<System, Reference<Integer>> postStart = postStarts.get(0);
        assertThat("actor post start system", postStart.first, is(mSystem));
        assertThat("actor post start reference", postStart.second, is(reference));

        assertThat("actor pre stops", actor.getPreStops(), is(empty()));
    }

    public final void testTellAfterPause() {
        final MockActor<Integer> actor = new MockActor<>();
        final Reference<Integer> reference = mSystem.with(isA(RandomString), actor);

        assertThat("actor pause", reference.pause(), is(true));
        assertThat("actor tell", reference.tell(a(RandomInteger)), is(true));
        assertThat("actor received messages", actor.getOnMessages(), is(empty()));
    }

    public final void testTellAfterStart() {
        final MockActor<Integer> actor = new MockActor<>();
        final Reference<Integer> reference = mSystem.with(isA(RandomString), actor);

        assertThat("system pause", mSystem.pause(), is(true));
        assertThat("system start", mSystem.start(), is(true));
        final int expected = isA(RandomInteger);
        assertThat("actor tell", reference.tell(expected), is(true));

        final List<Pair<System, Integer>> onMessages = actor.getOnMessages();
        assertThat("number of the actor on message invocations", onMessages.size(), is(1));

        final Pair<System, Integer> onMessage = onMessages.get(0);
        assertThat("actor received system", onMessage.first, is(mSystem));
        assertThat("actor received message", onMessage.second, is(expected));
    }

    public final void testTellDuringPauseIsReceivedAfterStart() {
        final MockActor<Integer> actor = new MockActor<>();
        final Reference<Integer> reference = mSystem.with(isA(RandomString), actor);

        assertThat("system pause", mSystem.pause(), is(true));
        final int expected = isA(RandomInteger);
        assertThat("actor tell", reference.tell(expected), is(true));
        assertThat("actor received messages", actor.getOnMessages(), is(empty()));

        assertThat("system start", mSystem.start(), is(true));
        final List<Pair<System, Integer>> onMessages = actor.getOnMessages();
        assertThat("number of the actor on message invocations", onMessages.size(), is(1));

        final Pair<System, Integer> onMessage = onMessages.get(0);
        assertThat("actor received system", onMessage.first, is(mSystem));
        assertThat("actor received message", onMessage.second, is(expected));
    }

    public final void testStop() {
        final MockActor<Integer> actor = new MockActor<>();
        final Reference<Integer> reference = mSystem.with(isA(RandomString), actor);

        assertThat("actor stop", reference.stop(), is(true));
        assertThat("actor is stopped", reference.isStopped(), is(true));

        final List<Pair<System, Reference<Integer>>> postStarts = actor.getPostStarts();
        assertThat("number of actor post start invocations", postStarts.size(), is(1));

        final Pair<System, Reference<Integer>> postStart = postStarts.get(0);
        assertThat("actor post start system", postStart.first, is(mSystem));
        assertThat("actor post start reference", postStart.second, is(reference));

        final List<System> preStops = actor.getPreStops();
        assertThat("number of actor pre stop invocations", preStops.size(), is(1));
        assertThat("actor pre stop system", preStops.get(0), is(mSystem));
    }

    public final void testDoubleStop() {
        final MockActor<Integer> actor = new MockActor<>();
        final Reference<Integer> reference = mSystem.with(isA(RandomString), actor);

        assertThat("actor 1st stop", reference.stop(), is(true));
        assertThat("actor 2nd stop", reference.stop(), is(true));
        assertThat("actor is stopped", reference.isStopped(), is(true));

        final List<Pair<System, Reference<Integer>>> postStarts = actor.getPostStarts();
        assertThat("number of actor post start invocations", postStarts.size(), is(1));

        final Pair<System, Reference<Integer>> postStart = postStarts.get(0);
        assertThat("actor post start system", postStart.first, is(mSystem));
        assertThat("actor post start reference", postStart.second, is(reference));

        final List<System> preStops = actor.getPreStops();
        assertThat("number of actor pre stop invocations", preStops.size(), is(1));
        assertThat("actor pre stop system", preStops.get(0), is(mSystem));
    }

    public final void testExecutorStop() {
        final MockActor<Integer> actor = new MockActor<>();
        final Reference<Integer> reference = mSystem.with(isA(RandomString), actor);

        assertThat("executor stop", mExecutor.stop(), is(true));
        assertThat("actor is stopped", reference.isStopped(), is(true));

        final List<Pair<System, Reference<Integer>>> postStarts = actor.getPostStarts();
        assertThat("number of actor post start invocations", postStarts.size(), is(1));

        final Pair<System, Reference<Integer>> postStart = postStarts.get(0);
        assertThat("actor post start system", postStart.first, is(mSystem));
        assertThat("actor post start reference", postStart.second, is(reference));

        final List<System> preStops = actor.getPreStops();
        assertThat("number of actor pre stop invocations", preStops.size(), is(1));
        assertThat("actor pre stop system", preStops.get(0), is(mSystem));
    }

    public final void testPauseAfterStop() {
        final MockActor<Integer> actor = new MockActor<>();
        final Reference<Integer> reference = mSystem.with(isA(RandomString), actor);

        assertThat("actor stop", reference.stop(), is(true));
        assertThat("actor is stopped", reference.isStopped(), is(true));
        try {
            reference.pause();
            fail("pausing of actor did not throw UnsupportedOperationException");
        } catch (final UnsupportedOperationException ignored) {/* expected */}
    }

    public final void testStopAfterPause() {
        final MockActor<Integer> actor = new MockActor<>();
        final Reference<Integer> reference = mSystem.with(isA(RandomString), actor);

        assertThat("actor pause", reference.pause(), is(true));
        assertThat("actor stop", reference.stop(), is(true));
        assertThat("actor is stopped", reference.isStopped(), is(true));
    }

    public final void testTellDuringPauseIsReceivedBeforeStop() {
        final MockActor<Integer> actor = new MockActor<>();
        final Reference<Integer> reference = mSystem.with(isA(RandomString), actor);

        assertThat("actor pause", reference.pause(), is(true));
        final int expected = isA(RandomInteger);
        assertThat("actor tell", reference.tell(expected), is(true));
        assertThat("actor stop", reference.stop(), is(true));
        assertThat("actor is stopped", reference.isStopped(), is(true));

        final List<Pair<System, Integer>> onMessages = actor.getOnMessages();
        assertThat("number of the actor on message invocations", onMessages.size(), is(1));

        final Pair<System, Integer> onMessage = onMessages.get(0);
        assertThat("actor received system", onMessage.first, is(mSystem));
        assertThat("actor received message", onMessage.second, is(expected));
    }

    public final void testTellAfterStop() {
        final Reference<Integer> reference = mSystem.with(isA(RandomString), new DummyActor<Integer>());

        assertThat("actor stop", reference.stop(), is(true));
        try {
            reference.tell(a(RandomInteger));
            fail("sending of message did not throw UnsupportedOperationException");
        } catch (final UnsupportedOperationException ignored) {/* expected */}
    }

    public final void testNoDirectCallCycles() {
        final AtomicInteger last = new AtomicInteger();
        final Reference<Integer> reference = mSystem.with(isA(RandomString), new Actor<Integer>() {

            private Reference<Integer> mSelf;

            @Override
            protected void postStart(@NonNull final System system,
                                     @NonNull final Reference<Integer> self) {
                mSelf = self;
            }

            @Override
            protected void onMessage(@NonNull final System system, @NonNull final Integer value) {
                last.set(value);
                if (value > 0) {
                    mSelf.tell(value - 1);
                }
            }
        });

        assertThat("actor tell", reference.tell(1), is(true));
        assertThat("actor last received message", last.get(), is(1));
    }

    public final void testDirectCallSystemMessage() {
        final Reference<Integer> reference = mSystem.with(isA(RandomString), new DummyActor<Integer>());
        final MockDirectCall<Integer> direct = new MockDirectCall<>();
        reference.setDirectCall(direct);

        assertThat("actor stop", reference.stop(), is(true));

        final List<Integer> messages = direct.getSystemMessages();
        assertThat("number of the direct call system messages", messages.size(), is(1));
        assertThat("direct call system message", messages.get(0), is(Reference.STOP));
    }

    public final void testDirectCallSystemMessageAfterPauseWithoutPendingMessages() {
        final Reference<Integer> reference = mSystem.with(isA(RandomString), new DummyActor<Integer>());
        assertThat("actor pause", reference.pause(), is(true));

        final MockDirectCall<Integer> direct = new MockDirectCall<>();
        reference.setDirectCall(direct);
        final Integer expected = isA(RandomInteger);
        assertThat("actor send", reference.send(expected), is(true));

        final List<Integer> messages = direct.getSystemMessages();
        assertThat("number of the direct call system messages", messages.size(), is(1));
        assertThat("direct call system message", messages.get(0), is(expected));
    }

    public final void testDirectCallSystemMessageAfterPauseWithPendingMessages() {
        final Reference<Integer> reference = mSystem.with(isA(RandomString), new DummyActor<Integer>());
        assertThat("actor pause", reference.pause(), is(true));

        final MockDirectCall<Integer> direct = new MockDirectCall<>();
        reference.setDirectCall(direct);
        assertThat("actor tell", reference.tell(a(RandomInteger)), is(true));
        assertThat("actor send", reference.send(a(RandomInteger)), is(true));

        assertThat("direct call user messages", direct.getUserMessages(), is(empty()));
        assertThat("direct call system messages", direct.getSystemMessages(), is(empty()));
    }

    public final void testDirectCallUserMessage() {
        final Reference<Integer> reference = mSystem.with(isA(RandomString), new DummyActor<Integer>());
        final MockDirectCall<Integer> direct = new MockDirectCall<>();
        reference.setDirectCall(direct);

        final int expected = isA(RandomInteger);
        assertThat("actor tell", reference.tell(expected), is(true));

        final List<Integer> messages = direct.getUserMessages();
        assertThat("number of the direct call user messages", messages.size(), is(1));
        assertThat("direct call user message", messages.get(0), is(expected));
    }

    public final void testDirectCallUserMessageAfterPause() {
        final Reference<Integer> reference = mSystem.with(isA(RandomString), new DummyActor<Integer>());
        assertThat("actor pause", reference.pause(), is(true));

        final MockDirectCall<Integer> direct = new MockDirectCall<>();
        reference.setDirectCall(direct);
        final int expected = isA(RandomInteger);
        assertThat("actor tell", reference.tell(expected), is(true));

        assertThat("direct call user messages", direct.getUserMessages(), is(empty()));
    }

    private static class MockDirectCall<M> implements Reference.DirectCall<M> {

        private final List<Integer> mSystemMessages = new ArrayList<>(1);
        private final List<M> mUserMessages = new ArrayList<>(1);

        public MockDirectCall() {
            super();
        }

        @NonNull
        public final List<Integer> getSystemMessages() {
            return unmodifiableList(mSystemMessages);
        }

        @NonNull
        public final List<M> getUserMessages() {
            return unmodifiableList(mUserMessages);
        }

        @Override
        public final boolean handleSystemMessage(final int message) {
            mSystemMessages.add(message);
            return true;
        }

        @Override
        public final boolean handleUserMessage(@NonNull final M message) {
            mUserMessages.add(message);
            return true;
        }
    }
}
