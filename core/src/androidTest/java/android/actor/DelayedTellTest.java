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

import android.actor.util.Wait;
import android.support.annotation.NonNull;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static android.actor.TimeMatchers.after;
import static android.os.SystemClock.uptimeMillis;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

public class DelayedTellTest extends TestCase {

    private Executor mExecutor;
    private System mSystem;

    @Override
    public final void setUp() throws Exception {
        super.setUp();

        mExecutor = Executors.singleThread();
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

    public final void testTell() throws InterruptedException {
        final MockActor<Integer> actor = new MockActor<>();
        final Reference<Integer> reference = mSystem.register(a(RandomName), actor);

        final int delay = isA(RandomDelay);
        final long earliestDelivery = uptimeMillis() + delay;
        assertThat("actor tell", reference.tell(a(RandomMessage), delay, MILLISECONDS), is(true));

        final List<Long> onMessages = actor.getOnMessages(1);
        assertThat("number of the actor on message invocations", onMessages.size(), is(1));
        assertThat("actor received message time ", onMessages.get(0), is(after(earliestDelivery)));
    }

    public final void testTellWithZeroDelay() throws InterruptedException {
        final MockActor<Integer> actor = new MockActor<>();
        final Reference<Integer> reference = mSystem.register(a(RandomName), actor);

        final long earliestDelivery = uptimeMillis();
        assertThat("actor tell", reference.tell(a(RandomMessage), 0, MILLISECONDS), is(true));

        final List<Long> onMessages = actor.getOnMessages(1);
        assertThat("number of the actor on message invocations", onMessages.size(), is(1));
        assertThat("actor received message time ", onMessages.get(0), is(after(earliestDelivery)));
    }

    public final void testTellWithNegativeDelay() throws InterruptedException {
        final MockActor<Integer> actor = new MockActor<>();
        final Reference<Integer> reference = mSystem.register(a(RandomName), actor);

        final long earliestDelivery = uptimeMillis();
        assertThat("actor tell", reference.tell(a(RandomMessage), -1, MILLISECONDS), is(true));

        final List<Long> onMessages = actor.getOnMessages(1);
        assertThat("number of the actor on message invocations", onMessages.size(), is(1));
        assertThat("actor received message time ", onMessages.get(0), is(after(earliestDelivery)));
    }

    public final void testTellDuringShortPauseIsReceivedAfterStart() throws InterruptedException {
        final MockActor<Integer> actor = new MockActor<>();
        final Reference<Integer> reference = mSystem.register(a(RandomName), actor);

        final int delay = isA(RandomDelay);
        assertThat("system pause", mSystem.pause(), is(true));
        final long earliestDelivery = uptimeMillis() + delay;
        assertThat("actor tell", reference.tell(a(RandomMessage), delay, MILLISECONDS), is(true));

        assertThat("system start", mSystem.start(), is(true));
        final List<Long> onMessages = actor.getOnMessages(1);
        assertThat("number of the actor on message invocations", onMessages.size(), is(1));
        assertThat("actor received message time ", onMessages.get(0), is(after(earliestDelivery)));
    }

    public final void testTellDuringLongPauseIsReceivedAfterStart() throws InterruptedException {
        final MockActor<Integer> actor = new MockActor<>();
        final Reference<Integer> reference = mSystem.register(a(RandomName), actor);

        final int delay = isA(RandomDelay);
        assertThat("system pause", mSystem.pause(), is(true));
        final long earliestDelivery = uptimeMillis() + delay;
        assertThat("actor tell", reference.tell(a(RandomMessage), delay, MILLISECONDS), is(true));

        sleep(2L * delay, MILLISECONDS);

        assertThat("system start", mSystem.start(), is(true));
        final List<Long> onMessages = actor.getOnMessages(1);
        assertThat("number of the actor on message invocations", onMessages.size(), is(1));
        assertThat("actor received message time ", onMessages.get(0), is(after(earliestDelivery)));
    }

    public final void testTellDuringShortPauseIsNotReceivedBeforeStop() throws InterruptedException {
        final MockActor<Integer> actor = new MockActor<>();
        final Reference<Integer> reference = mSystem.register(a(RandomName), actor);

        final int delay = isA(RandomDelay);
        assertThat("actor pause", reference.pause(), is(true));
        assertThat("actor tell", reference.tell(a(RandomMessage), delay, MILLISECONDS), is(true));
        assertThat("actor stop", reference.stop(), is(true));

        sleep(2L * delay, MILLISECONDS);

        assertThat("actor received messages", actor.getOnMessages(0), is(empty()));
    }

    public final void testTellDuringLongPauseIsReceivedBeforeStop() throws InterruptedException {
        final MockActor<Integer> actor = new MockActor<>();
        final Reference<Integer> reference = mSystem.register(a(RandomName), actor);

        final int delay = isA(RandomDelay);
        assertThat("actor pause", reference.pause(), is(true));
        final long earliestDelivery = uptimeMillis() + delay;
        assertThat("actor tell", reference.tell(a(RandomMessage), delay, MILLISECONDS), is(true));

        sleep(2L * delay, MILLISECONDS);
        assertThat("actor stop", reference.stop(), is(true));

        final List<Long> onMessages = actor.getOnMessages(1);
        assertThat("number of the actor on message invocations", onMessages.size(), is(1));
        assertThat("actor received message time ", onMessages.get(0), is(after(earliestDelivery)));
    }

    public final void testTellAfterStop() {
        final Reference<Integer> reference = mSystem.register(a(RandomName), new DummyActor<Integer>());

        assertThat("actor stop", reference.stop(), is(true));
        try {
            reference.tell(a(RandomMessage), a(RandomDelay), MILLISECONDS);
            fail("sending of message did not throw UnsupportedOperationException");
        } catch (final UnsupportedOperationException ignored) {/* expected */}
    }

    private static final RandomDataGenerator<String> RandomName = RandomString;
    private static final RandomDataGenerator<Integer> RandomMessage = RandomInteger;
    private static final RandomDataGenerator<Integer> RandomDelay =
            new TestCase.RandomDataGenerator<Integer>() {
                @NotNull
                @Override
                public Integer next(@NotNull final Random random) {
                    return 1 + random.nextInt(100);
                }
            };

    private static void sleep(final long delay, @NonNull final TimeUnit unit) throws InterruptedException {
        final long start = uptimeMillis();
        final long end = start + unit.toMillis(delay);

        long now = uptimeMillis();
        while (now < end) {
            Thread.sleep(end - now);
            now = uptimeMillis();
        }
    }

    private static class MockActor<M> extends Actor<M> {

        private final Object mLock = new Object();
        private final List<Long> mOnMessages = new ArrayList<>(1);

        MockActor() {
            super();
        }

        @NonNull
        public final List<Long> getOnMessages(final int expectedSize) throws InterruptedException {
            synchronized (mLock) {
                int waits = 0;
                while ((mOnMessages.size() < expectedSize) && (waits < Wait.MAX_REPEATS)) {
                    mLock.wait(Wait.DELAY);
                    waits++;
                }

                return new ArrayList<>(mOnMessages);
            }
        }

        @Override
        protected final void onMessage(@NonNull final M message) {
            final long now = uptimeMillis();
            synchronized (mLock) {
                mOnMessages.add(now);
                mLock.notifyAll();
            }
        }
    }
}
