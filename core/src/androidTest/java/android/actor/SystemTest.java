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

import android.actor.executor.Executable;
import android.actor.messenger.Messengers;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import static android.actor.SystemMatchers.paused;
import static android.actor.SystemMatchers.started;
import static android.actor.SystemMatchers.stopped;
import static android.os.Looper.myLooper;
import static java.util.Collections.unmodifiableList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class SystemTest extends TestCase {

    private MockExecutor mExecutor;
    private System mSystem;

    @Override
    public final void setUp() throws Exception {
        super.setUp();

        mExecutor = new MockExecutor();
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

    public final void testRegister() {
        assertThat("system", mSystem, is(started()));

        final Reference<?> reference = mSystem.register(a(RandomName), new DummyActor<>());
        assertThat("actor reference", reference, is(notNullValue()));

        final List<MockExecutor.Submission> submissions = mExecutor.getSubmissions();
        assertThat("number of the executor submit invocations", submissions.size(), is(1));
        assertThat("submission is stopped", submissions.get(0).isStopped(), is(false));
    }

    public final void testRegisterAgainWithSameName() {
        assertThat("system", mSystem, is(started()));

        final String name = isA(RandomName);
        final Reference<?> reference = mSystem.register(name, new DummyActor<>());
        assertThat("actor reference", reference, is(notNullValue()));

        try {
            mSystem.register(name, new DummyActor<>());
            fail("registering actor did not throw IllegalArgumentException");
        } catch (final IllegalArgumentException ignored) {/* expected */}

        final List<MockExecutor.Submission> submissions = mExecutor.getSubmissions();
        assertThat("number of the executor submit invocations", submissions.size(), is(1));
        assertThat("submission is stopped", submissions.get(0).isStopped(), is(false));
    }

    public final void testRegisterSameNameAfterActorStop() {
        assertThat("system", mSystem, is(started()));

        final String name = isA(RandomName);
        final Reference<?> reference = mSystem.register(name, new DummyActor<>());
        assertThat("actor reference", reference, is(notNullValue()));

        assertThat("actor stop", reference.stop(), is(true));
        mSystem.register(name, new DummyActor<>());

        final List<MockExecutor.Submission> submissions = mExecutor.getSubmissions();
        assertThat("number of the executor submit invocations", submissions.size(), is(2));
        assertThat("submission is stopped", submissions.get(0).isStopped(), is(true));
        assertThat("submission is stopped", submissions.get(1).isStopped(), is(false));
    }

    public final void testDoubleStart() {
        assertThat("system", mSystem, is(started()));

        final Reference<?> reference = mSystem.register(a(RandomName), new DummyActor<>());
        assertThat("actor reference", reference, is(notNullValue()));

        assertThat("system start", mSystem.start(), is(true));
        assertThat("system", mSystem, is(started()));

        assertThat("actor is stopped", reference.isStopped(), is(false));
        final List<MockExecutor.Submission> submissions = mExecutor.getSubmissions();
        assertThat("number of the executor submit invocations", submissions.size(), is(1));
        assertThat("1st submission is stopped", submissions.get(0).isStopped(), is(false));
    }

    public final void testPause() {
        assertThat("system", mSystem, is(started()));

        final Reference<?> reference = mSystem.register(a(RandomName), new DummyActor<>());
        assertThat("actor reference", reference, is(notNullValue()));

        assertThat("system pause", mSystem.pause(), is(true));
        assertThat("system", mSystem, is(paused()));

        assertThat("actor is stopped", reference.isStopped(), is(false));
        final List<MockExecutor.Submission> submissions = mExecutor.getSubmissions();
        assertThat("number of the executor submit invocations", submissions.size(), is(1));
        assertThat("submission is stopped", submissions.get(0).isStopped(), is(true));
    }

    public final void testDoublePause() {
        assertThat("system", mSystem, is(started()));

        final Reference<?> reference = mSystem.register(a(RandomName), new DummyActor<>());
        assertThat("actor reference", reference, is(notNullValue()));

        assertThat("system pause", mSystem.pause(), is(true));
        assertThat("system pause", mSystem.pause(), is(true));
        assertThat("system", mSystem, is(paused()));

        assertThat("actor is stopped", reference.isStopped(), is(false));
        final List<MockExecutor.Submission> submissions = mExecutor.getSubmissions();
        assertThat("number of the executor submit invocations", submissions.size(), is(1));
        assertThat("submission is stopped", submissions.get(0).isStopped(), is(true));
    }

    public final void testRegisterAfterPause() {
        assertThat("system pause", mSystem.pause(), is(true));
        assertThat("system", mSystem, is(paused()));

        final Reference<?> reference = mSystem.register(a(RandomName), new DummyActor<>());
        assertThat("actor reference", reference, is(notNullValue()));

        assertThat("actor is stopped", reference.isStopped(), is(false));
        assertThat("executor submissions", mExecutor.getSubmissions(), is(empty()));
    }

    public final void testStartAfterPause() {
        assertThat("system", mSystem, is(started()));

        final Reference<?> reference = mSystem.register(a(RandomName), new DummyActor<>());
        assertThat("actor reference", reference, is(notNullValue()));

        assertThat("system pause", mSystem.pause(), is(true));
        assertThat("system start", mSystem.start(), is(true));
        assertThat("system", mSystem, is(started()));

        assertThat("actor is stopped", reference.isStopped(), is(false));
        final List<MockExecutor.Submission> submissions = mExecutor.getSubmissions();
        assertThat("number of the executor submit invocations", submissions.size(), is(2));
        assertThat("1st submission is stopped", submissions.get(0).isStopped(), is(true));
        assertThat("2nd submission is stopped", submissions.get(1).isStopped(), is(false));
    }

    public final void testStop() {
        assertThat("system", mSystem, is(started()));

        final Reference<?> reference = mSystem.register(a(RandomName), new DummyActor<>());
        assertThat("actor reference", reference, is(notNullValue()));

        assertThat("system stop", mSystem.stop(false), is(true));
        assertThat("system", mSystem, is(stopped()));

        assertThat("actor is stopped", reference.isStopped(), is(true));
        final List<MockExecutor.Submission> submissions = mExecutor.getSubmissions();
        assertThat("number of the executor submit invocations", submissions.size(), is(1));
        assertThat("submission is stopped", submissions.get(0).isStopped(), is(true));
    }

    public final void testDoubleStop() {
        assertThat("system", mSystem, is(started()));

        final Reference<?> reference = mSystem.register(a(RandomName), new DummyActor<>());
        assertThat("actor reference", reference, is(notNullValue()));

        assertThat("system stop", mSystem.stop(false), is(true));
        assertThat("system stop", mSystem.stop(false), is(true));
        assertThat("system", mSystem, is(stopped()));

        assertThat("actor is stopped", reference.isStopped(), is(true));
        final List<MockExecutor.Submission> submissions = mExecutor.getSubmissions();
        assertThat("number of the executor submit invocations", submissions.size(), is(1));
        assertThat("submission is stopped", submissions.get(0).isStopped(), is(true));
    }

    public final void testRegisterAfterStop() {
        assertThat("system", mSystem, is(started()));

        assertThat("system stop", mSystem.stop(false), is(true));
        try {
            mSystem.register(a(RandomName), new DummyActor<>());
            fail("registering actor did not throw UnsupportedOperationException");
        } catch (final UnsupportedOperationException ignored) {/* expected */}

        assertThat("executor submissions", mExecutor.getSubmissions(), is(empty()));
    }

    public final void testStartAfterStop() {
        assertThat("system", mSystem, is(started()));

        assertThat("system stop", mSystem.stop(false), is(true));
        try {
            mSystem.start();
            fail("starting of system did not throw UnsupportedOperationException");
        } catch (final UnsupportedOperationException ignored) {/* expected */}
    }

    public final void testPauseAfterStop() {
        assertThat("system", mSystem, is(started()));

        assertThat("system stop", mSystem.stop(false), is(true));
        try {
            mSystem.pause();
            fail("pausing of system did not throw UnsupportedOperationException");
        } catch (final UnsupportedOperationException ignored) {/* expected */}
    }

    public final void testStopAfterPauseWithPendingMessages() {
        assertThat("system", mSystem, is(started()));

        final MockActor<Integer> actor = new MockActor<>();
        final Reference<Integer> reference = mSystem.register(a(RandomName), actor);
        assertThat("actor reference", reference, is(notNullValue()));

        assertThat("system pause", mSystem.pause(), is(true));
        final int message = isA(RandomMessage);
        assertThat("actor tell", reference.tell(message), is(true));
        assertThat("system stop", mSystem.stop(false), is(true));
        assertThat("system", mSystem, is(stopped()));

        assertThat("actor is stopped", reference.isStopped(), is(true));
        final List<Integer> messages = actor.getMessages();
        assertThat("number of actor received messages", messages.size(), is(1));
        assertThat("actor received message", messages.get(0), is(message));
    }

    public final void testImmediateStop() {
        assertThat("system", mSystem, is(started()));

        final Reference<?> reference = mSystem.register(a(RandomName), new DummyActor<>());
        assertThat("actor reference", reference, is(notNullValue()));

        assertThat("system stop", mSystem.stop(true), is(true));
        assertThat("system", mSystem, is(stopped()));

        assertThat("reference is stopped", reference.isStopped(), is(true));
        final List<MockExecutor.Submission> submissions = mExecutor.getSubmissions();
        assertThat("number of the executor submit invocations", submissions.size(), is(1));
        assertThat("submission is stopped", submissions.get(0).isStopped(), is(true));
    }

    public final void testDoubleImmediateStop() {
        assertThat("system", mSystem, is(started()));

        final Reference<?> reference = mSystem.register(a(RandomName), new DummyActor<>());
        assertThat("actor reference", reference, is(notNullValue()));

        assertThat("system stop", mSystem.stop(true), is(true));
        assertThat("system stop", mSystem.stop(true), is(true));
        assertThat("system", mSystem, is(stopped()));

        assertThat("reference is stopped", reference.isStopped(), is(true));
        final List<MockExecutor.Submission> submissions = mExecutor.getSubmissions();
        assertThat("number of the executor submit invocations", submissions.size(), is(1));
        assertThat("submission is stopped", submissions.get(0).isStopped(), is(true));
    }

    public final void testRegisterAfterImmediateStop() {
        assertThat("system", mSystem, is(started()));

        assertThat("system stop", mSystem.stop(true), is(true));
        try {
            mSystem.register(a(RandomName), new DummyActor<>());
            fail("registering actor did not throw UnsupportedOperationException");
        } catch (final UnsupportedOperationException ignored) {/* expected */}

        assertThat("executor submissions", mExecutor.getSubmissions(), is(empty()));
    }

    public final void testStartAfterImmediateStop() {
        assertThat("system", mSystem, is(started()));

        assertThat("system stop", mSystem.stop(true), is(true));
        try {
            mSystem.start();
            fail("starting of system did not throw UnsupportedOperationException");
        } catch (final UnsupportedOperationException ignored) {/* expected */}
    }

    public final void testImmediateStopAfterPauseWithPendingMessages() {
        assertThat("system", mSystem, is(started()));

        final MockActor<Integer> actor = new MockActor<>();
        final Reference<Integer> reference = mSystem.register(a(RandomName), actor);
        assertThat("actor reference", reference, is(notNullValue()));

        assertThat("system pause", mSystem.pause(), is(true));
        final int message = isA(RandomMessage);
        assertThat("actor tell", reference.tell(message), is(true));
        assertThat("system stop", mSystem.stop(true), is(true));
        assertThat("system", mSystem, is(stopped()));

        assertThat("actor is stopped", reference.isStopped(), is(true));
        assertThat("actor received messages", actor.getMessages(), is(empty()));
    }

    private static final RandomDataGenerator<String> RandomName = RandomString;
    private static final RandomDataGenerator<Integer> RandomMessage = RandomInteger;

    private static class MockExecutor implements Executor {

        private final List<Submission> mSubmissions = new ArrayList<>(1);

        MockExecutor() {
            super();
        }

        @NonNull
        public final List<Submission> getSubmissions() {
            return unmodifiableList(mSubmissions);
        }

        @Nullable
        @Override
        public final Submission submit(@NonNull final Executable executable) {
            final Submission submission = new Submission(executable);
            mSubmissions.add(submission);
            return submission;
        }

        @Override
        public final void stop() {/* do nothing */}

        public static class Submission implements Executor.Submission {

            @NonNull
            private final Executable mExecutable;

            private boolean mStopped = false;

            public Submission(@NonNull final Executable executable) {
                super();

                mExecutable = executable;
                final Looper looper = myLooper();
                assertThat("looper", looper, is(notNullValue()));
                mExecutable.attach(Messengers.from(looper));
            }

            public final boolean isStopped() {
                return mStopped;
            }

            @NonNull
            public final Executable getExecutable() {
                return mExecutable;
            }

            @Override
            public final boolean stop() {
                mExecutable.detach();
                mStopped = true;
                return true;
            }
        }
    }
}
