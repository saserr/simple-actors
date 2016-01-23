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

import static android.actor.SystemMatchers.paused;
import static android.actor.SystemMatchers.started;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class ReferenceTest extends TestCase {

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
        final String name = isA(RandomName);
        final Reference<Integer> reference = mSystem.register(name, actor);

        final List<Pair<Context, Reference<Integer>>> postStarts = actor.getPostStarts();
        assertThat("number of actor post start invocations", postStarts.size(), is(1));

        final Pair<Context, Reference<Integer>> postStart = postStarts.get(0);
        assertThat("actor post start context", postStart.first, is(notNullValue()));
        assertThat("actor post start system", postStart.first.getSystem(), is(mSystem));
        assertThat("actor post start name", postStart.first.getName(), is(new Actor.Name(null, name)));
        assertThat("actor post start reference", postStart.second, is(reference));
    }

    public final void testTell() {
        final MockActor<Integer> actor = new MockActor<>();
        final Reference<Integer> reference = mSystem.register(a(RandomName), actor);

        final int message = isA(RandomMessage);
        assertThat("actor tell", reference.tell(message), is(true));

        final List<Integer> messages = actor.getMessages();
        assertThat("number of the actor received messages", messages.size(), is(1));
        assertThat("actor received message", messages.get(0), is(message));
    }

    public final void testTellBeforeStart() {
        assertThat("system pause", mSystem.pause(), is(true));
        assertThat("system", mSystem, is(paused()));

        final MockActor<Integer> actor = new MockActor<>();
        final Reference<Integer> reference = mSystem.register(a(RandomName), actor);

        final int message = isA(RandomMessage);
        assertThat("actor tell", reference.tell(message), is(true));
        assertThat("actor received messages", actor.getMessages(), is(empty()));

        assertThat("system start", mSystem.start(), is(true));
        assertThat("system", mSystem, is(started()));

        final List<Integer> messages = actor.getMessages();
        assertThat("number of the actor received messages", messages.size(), is(1));
        assertThat("actor received message", messages.get(0), is(message));
    }

    public final void testPause() {
        final MockActor<Integer> actor = new MockActor<>();
        final Reference<Integer> reference = mSystem.register(a(RandomName), actor);

        assertThat("actor pause", reference.pause(), is(true));
        assertThat("actor is stopped", reference.isStopped(), is(false));

        assertThat("number of actor post start invocations", actor.getPostStarts().size(), is(1));
        assertThat("actor pre stops", actor.getPreStops(), is(0));
    }

    public final void testPauseBeforeStart() {
        assertThat("system pause", mSystem.pause(), is(true));
        assertThat("system", mSystem, is(paused()));

        final MockActor<Integer> actor = new MockActor<>();
        final Reference<Integer> reference = mSystem.register(a(RandomName), actor);

        assertThat("actor pause", reference.pause(), is(true));
        assertThat("actor is stopped", reference.isStopped(), is(false));

        assertThat("number of actor post start invocations", actor.getPostStarts().size(), is(0));
        assertThat("actor pre stops", actor.getPreStops(), is(0));
    }

    public final void testTellAfterPause() {
        final MockActor<Integer> actor = new MockActor<>();
        final Reference<Integer> reference = mSystem.register(a(RandomName), actor);

        assertThat("actor pause", reference.pause(), is(true));
        assertThat("actor tell", reference.tell(a(RandomMessage)), is(true));
        assertThat("actor received messages", actor.getMessages(), is(empty()));
    }

    public final void testTellAfterStart() {
        final MockActor<Integer> actor = new MockActor<>();
        final Reference<Integer> reference = mSystem.register(a(RandomName), actor);

        assertThat("system pause", mSystem.pause(), is(true));
        assertThat("system start", mSystem.start(), is(true));
        final int message = isA(RandomMessage);
        assertThat("actor tell", reference.tell(message), is(true));

        final List<Integer> messages = actor.getMessages();
        assertThat("number of the actor received messages", messages.size(), is(1));
        assertThat("actor received message", messages.get(0), is(message));
    }

    public final void testTellDuringPauseIsReceivedAfterStart() {
        final MockActor<Integer> actor = new MockActor<>();
        final Reference<Integer> reference = mSystem.register(a(RandomName), actor);

        assertThat("system pause", mSystem.pause(), is(true));
        final int message = isA(RandomMessage);
        assertThat("actor tell", reference.tell(message), is(true));
        assertThat("actor received messages", actor.getMessages(), is(empty()));

        assertThat("system start", mSystem.start(), is(true));
        final List<Integer> messages = actor.getMessages();
        assertThat("number of the actor received messages", messages.size(), is(1));
        assertThat("actor received message", messages.get(0), is(message));
    }

    public final void testStop() {
        final MockActor<Integer> actor = new MockActor<>();
        final Reference<Integer> reference = mSystem.register(a(RandomName), actor);

        assertThat("actor stop", reference.stop(), is(true));
        assertThat("actor is stopped", reference.isStopped(), is(true));

        assertThat("number of actor post start invocations", actor.getPostStarts().size(), is(1));
        assertThat("number of actor pre stop invocations", actor.getPreStops(), is(1));
    }

    public final void testDoubleStop() {
        final MockActor<Integer> actor = new MockActor<>();
        final Reference<Integer> reference = mSystem.register(a(RandomName), actor);

        assertThat("actor 1st stop", reference.stop(), is(true));
        assertThat("actor 2nd stop", reference.stop(), is(true));
        assertThat("actor is stopped", reference.isStopped(), is(true));

        assertThat("number of actor post start invocations", actor.getPostStarts().size(), is(1));
        assertThat("number of actor pre stop invocations", actor.getPreStops(), is(1));
    }

    public final void testStopExecutor() {
        final MockActor<Integer> actor = new MockActor<>();
        final Reference<Integer> reference = mSystem.register(a(RandomName), actor);

        mExecutor.stop();
        assertThat("actor is stopped", reference.isStopped(), is(false));
    }

    public final void testPauseAfterStop() {
        final MockActor<Integer> actor = new MockActor<>();
        final Reference<Integer> reference = mSystem.register(a(RandomName), actor);

        assertThat("actor stop", reference.stop(), is(true));
        assertThat("actor is stopped", reference.isStopped(), is(true));
        try {
            reference.pause();
            fail("pausing of actor did not throw UnsupportedOperationException");
        } catch (final UnsupportedOperationException ignored) {/* expected */}
    }

    public final void testStopAfterPause() {
        final MockActor<Integer> actor = new MockActor<>();
        final Reference<Integer> reference = mSystem.register(a(RandomName), actor);

        assertThat("actor pause", reference.pause(), is(true));
        assertThat("actor stop", reference.stop(), is(true));
        assertThat("actor is stopped", reference.isStopped(), is(true));
    }

    public final void testTellDuringPauseIsReceivedBeforeStop() {
        final MockActor<Integer> actor = new MockActor<>();
        final Reference<Integer> reference = mSystem.register(a(RandomName), actor);

        assertThat("actor pause", reference.pause(), is(true));
        final int message = isA(RandomMessage);
        assertThat("actor tell", reference.tell(message), is(true));
        assertThat("actor stop", reference.stop(), is(true));
        assertThat("actor is stopped", reference.isStopped(), is(true));

        final List<Integer> messages = actor.getMessages();
        assertThat("number of the actor received messages", messages.size(), is(1));
        assertThat("actor received message", messages.get(0), is(message));
    }

    public final void testTellAfterStop() {
        final Reference<Object> reference = mSystem.register(a(RandomName), new DummyActor<>());

        assertThat("actor stop", reference.stop(), is(true));
        try {
            reference.tell(a(RandomMessage));
            fail("sending of message did not throw UnsupportedOperationException");
        } catch (final UnsupportedOperationException ignored) {/* expected */}
    }

    public final void testNoDirectCallCycles() {
        final AtomicInteger last = new AtomicInteger();
        final Reference<Integer> reference = mSystem.register(a(RandomName), new Actor<Integer>() {

            private Reference<Integer> mSelf;

            @Override
            protected void postStart(@NonNull final Context context,
                                     @NonNull final Reference<Integer> self) {
                mSelf = self;
            }

            @Override
            protected void onMessage(@NonNull final Integer value) {
                last.set(value);
                if (value > 0) {
                    mSelf.tell(value - 1);
                }
            }
        });

        assertThat("actor tell", reference.tell(1), is(true));
        assertThat("actor last received message", last.get(), is(1));
    }

    private static final RandomDataGenerator<String> RandomName = RandomString;
    private static final RandomDataGenerator<Integer> RandomMessage = RandomInteger;
}
