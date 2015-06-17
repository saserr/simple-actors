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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static android.os.SystemClock.uptimeMillis;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;

public class DelayedTellTest extends TestCase {

    private static final int DELAY = 50;
    private static final TimeUnit DELAY_UNIT = TimeUnit.MILLISECONDS;

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
        final Reference<Integer> reference = mSystem.with(isA(RandomString), actor);

        final long expected = uptimeMillis() + DELAY_UNIT.toMillis(DELAY);
        assertThat("actor tell", reference.tell(a(RandomInteger), DELAY, DELAY_UNIT), is(true));

        final List<Long> onMessages = actor.getOnMessages(1);
        assertThat("number of the actor on message invocations", onMessages.size(), is(1));
        assertThat("actor received message time ", onMessages.get(0), is(greaterThanOrEqualTo(expected)));
    }

    public final void testTellWithZeroDelay() throws InterruptedException {
        final MockActor<Integer> actor = new MockActor<>();
        final Reference<Integer> reference = mSystem.with(isA(RandomString), actor);

        final long expected = uptimeMillis();
        assertThat("actor tell", reference.tell(a(RandomInteger), 0, DELAY_UNIT), is(true));

        final List<Long> onMessages = actor.getOnMessages(1);
        assertThat("number of the actor on message invocations", onMessages.size(), is(1));
        assertThat("actor received message time ", onMessages.get(0), is(greaterThanOrEqualTo(expected)));
    }

    public final void testTellWithNegativeDelay() throws InterruptedException {
        final MockActor<Integer> actor = new MockActor<>();
        final Reference<Integer> reference = mSystem.with(isA(RandomString), actor);

        final long expected = uptimeMillis();
        assertThat("actor tell", reference.tell(a(RandomInteger), -1, DELAY_UNIT), is(true));

        final List<Long> onMessages = actor.getOnMessages(1);
        assertThat("number of the actor on message invocations", onMessages.size(), is(1));
        assertThat("actor received message time ", onMessages.get(0), is(greaterThanOrEqualTo(expected)));
    }

    public final void testTellDuringShortPauseIsReceivedAfterStart() throws InterruptedException {
        final MockActor<Integer> actor = new MockActor<>();
        final Reference<Integer> reference = mSystem.with(isA(RandomString), actor);

        assertThat("system pause", mSystem.pause(), is(true));
        final long expected = uptimeMillis() + DELAY_UNIT.toMillis(DELAY);
        assertThat("actor tell", reference.tell(a(RandomInteger), DELAY, DELAY_UNIT), is(true));

        assertThat("system start", mSystem.start(), is(true));
        final List<Long> onMessages = actor.getOnMessages(1);
        assertThat("number of the actor on message invocations", onMessages.size(), is(1));
        assertThat("actor received message time ", onMessages.get(0), is(greaterThanOrEqualTo(expected)));
    }

    public final void testTellDuringLongPauseIsReceivedAfterStart() throws InterruptedException {
        final MockActor<Integer> actor = new MockActor<>();
        final Reference<Integer> reference = mSystem.with(isA(RandomString), actor);

        assertThat("system pause", mSystem.pause(), is(true));
        final long expected = uptimeMillis() + DELAY_UNIT.toMillis(DELAY);
        assertThat("actor tell", reference.tell(a(RandomInteger), DELAY, DELAY_UNIT), is(true));

        sleep(2 * DELAY, DELAY_UNIT);

        assertThat("system start", mSystem.start(), is(true));
        final List<Long> onMessages = actor.getOnMessages(1);
        assertThat("number of the actor on message invocations", onMessages.size(), is(1));
        assertThat("actor received message time ", onMessages.get(0), is(greaterThanOrEqualTo(expected)));
    }

    public final void testTellDuringShortPauseIsNotReceivedBeforeStop() throws InterruptedException {
        final MockActor<Integer> actor = new MockActor<>();
        final Reference<Integer> reference = mSystem.with(isA(RandomString), actor);

        assertThat("actor pause", reference.pause(), is(true));
        assertThat("actor tell", reference.tell(a(RandomInteger), DELAY, DELAY_UNIT), is(true));
        assertThat("actor stop", reference.stop(), is(true));

        sleep(2 * DELAY, DELAY_UNIT);

        assertThat("actor received messages", actor.getOnMessages(0), is(empty()));
    }

    public final void testTellDuringLongPauseIsReceivedBeforeStop() throws InterruptedException {
        final MockActor<Integer> actor = new MockActor<>();
        final Reference<Integer> reference = mSystem.with(isA(RandomString), actor);

        assertThat("actor pause", reference.pause(), is(true));
        final long expected = uptimeMillis() + DELAY_UNIT.toMillis(DELAY);
        assertThat("actor tell", reference.tell(a(RandomInteger), DELAY, DELAY_UNIT), is(true));

        sleep(2 * DELAY, DELAY_UNIT);
        assertThat("actor stop", reference.stop(), is(true));

        final List<Long> onMessages = actor.getOnMessages(1);
        assertThat("number of the actor on message invocations", onMessages.size(), is(1));
        assertThat("actor received message time ", onMessages.get(0), is(greaterThanOrEqualTo(expected)));
    }

    public final void testTellAfterStop() {
        final Reference<Integer> reference = mSystem.with(isA(RandomString), new DummyActor<Integer>());

        assertThat("actor stop", reference.stop(), is(true));
        try {
            reference.tell(a(RandomInteger), DELAY, DELAY_UNIT);
            fail("sending of message did not throw UnsupportedOperationException");
        } catch (final UnsupportedOperationException ignored) {/* expected */}
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

    private static void sleep(final long delay, @NonNull final TimeUnit unit) throws InterruptedException {
        final long start = uptimeMillis();
        final long end = start + unit.toMillis(delay);

        long now = uptimeMillis();
        while (now < end) {
            Thread.sleep(end - now);
            now = uptimeMillis();
        }
    }
}
