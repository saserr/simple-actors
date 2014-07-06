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

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class ActorTest extends TestCase {

    private System mSystem;

    @Override
    public final void setUp() throws Exception {
        super.setUp();

        mSystem = new System(new CurrentThreadExecutor());
    }

    @Override
    public final void tearDown() throws Exception {
        mSystem.stop(true);
        mSystem = null;

        super.tearDown();
    }

    public final void testPostStart() {
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

        final List<Pair<System, Integer>> tells = actor.getTells();
        assertThat("number of actor on message invocations", tells.size(), is(1));

        final Pair<System, Integer> tell = tells.get(0);
        assertThat("actor received system", tell.first, is(mSystem));
        assertThat("actor received message", tell.second, is(expected));
    }

    public final void testStop() {
        final Reference<Integer> reference = mSystem.with(isA(RandomString), new DummyActor<Integer>());

        assertThat("actor stop", reference.stop(), is(true));
        assertThat("actor is stopped", reference.isStopped(), is(true));
    }

    public final void testPreStop() {
        final MockActor<Integer> actor = new MockActor<>();
        final Reference<Integer> reference = mSystem.with(isA(RandomString), actor);

        assertThat("actor stop", reference.stop(), is(true));

        final List<System> preStops = actor.getPreStops();
        assertThat("number of actor pre stop invocations", preStops.size(), is(1));
        assertThat("actor pre stop system", preStops.get(0), is(mSystem));
    }

    public final void testDoubleStop() {
        final Reference<Integer> reference = mSystem.with(isA(RandomString), new DummyActor<Integer>());

        assertThat("1st actor stop", reference.stop(), is(true));
        assertThat("2nd actor stop", reference.stop(), is(true));
        assertThat("actor is stopped", reference.isStopped(), is(true));
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
        final MockDirectCall<Integer> directCall = new MockDirectCall<>();
        reference.setDirectCall(directCall);

        assertThat("actor stop", reference.stop(), is(true));
        final List<Integer> messages = directCall.getSystemMessages();
        assertThat("number of the direct call system messages", messages.size(), is(1));
        assertThat("direct call system message", messages.get(0), is(Reference.STOP));
    }

    public final void testDirectCallUserMessage() {
        final Reference<Integer> reference = mSystem.with(isA(RandomString), new DummyActor<Integer>());
        final MockDirectCall<Integer> directCall = new MockDirectCall<>();
        reference.setDirectCall(directCall);

        final int expected = isA(RandomInteger);
        assertThat("actor tell", reference.tell(expected), is(true));
        final List<Integer> messages = directCall.getUserMessages();
        assertThat("number of the direct call user messages", messages.size(), is(1));
        assertThat("direct call user message", messages.get(0), is(expected));
    }
}
