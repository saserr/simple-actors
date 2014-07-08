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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;

import java.util.ArrayList;
import java.util.List;

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

    public final void testSubmit() {
        assertThat("system is started", mSystem.isStarted(), is(true));
        assertThat("system is paused", mSystem.isPaused(), is(false));
        assertThat("system is stopped", mSystem.isStopped(), is(false));

        assertThat("actor reference", mSystem.with(isA(RandomString), new DummyActor<>()), is(notNullValue()));

        final List<MockExecutor.Submission> submissions = mExecutor.getSubmissions();
        assertThat("number of the executor submit invocations", submissions.size(), is(1));
        assertThat("submission is stopped", submissions.get(0).isStopped(), is(false));
    }

    public final void testSubmitSameName() {
        assertThat("system is started", mSystem.isStarted(), is(true));
        assertThat("system is paused", mSystem.isPaused(), is(false));
        assertThat("system is stopped", mSystem.isStopped(), is(false));

        final String name = isA(RandomString);
        assertThat("actor reference", mSystem.with(name, new DummyActor<>()), is(notNullValue()));
        try {
            mSystem.with(name, new DummyActor<>());
            fail("submitting of actor did not throw IllegalArgumentException");
        } catch (final IllegalArgumentException ignored) {/* expected */}

        final List<MockExecutor.Submission> submissions = mExecutor.getSubmissions();
        assertThat("number of the executor submit invocations", submissions.size(), is(1));
        assertThat("submission is stopped", submissions.get(0).isStopped(), is(false));
    }

    public final void testSubmitSameNameAfterActorStop() {
        assertThat("system is started", mSystem.isStarted(), is(true));
        assertThat("system is paused", mSystem.isPaused(), is(false));
        assertThat("system is stopped", mSystem.isStopped(), is(false));

        final String name = isA(RandomString);
        final Reference<Object> reference = mSystem.with(name, new DummyActor<>());
        assertThat("actor reference", reference, is(notNullValue()));

        assertThat("actor stop", reference.stop(), is(true));
        mSystem.with(name, new DummyActor<>());

        final List<MockExecutor.Submission> submissions = mExecutor.getSubmissions();
        assertThat("number of the executor submit invocations", submissions.size(), is(2));
        assertThat("submission is stopped", submissions.get(0).isStopped(), is(true));
        assertThat("submission is stopped", submissions.get(1).isStopped(), is(false));
    }

    public final void testDoubleStart() {
        assertThat("system is started", mSystem.isStarted(), is(true));
        assertThat("system is paused", mSystem.isPaused(), is(false));
        assertThat("system is stopped", mSystem.isStopped(), is(false));

        final Reference<Integer> reference = mSystem.with(isA(RandomString), new DummyActor<Integer>());
        assertThat("actor reference", reference, is(notNullValue()));

        assertThat("system start", mSystem.start(), is(true));
        assertThat("system is started", mSystem.isStarted(), is(true));
        assertThat("system is paused", mSystem.isPaused(), is(false));
        assertThat("system is stopped", mSystem.isStopped(), is(false));

        assertThat("actor is stopped", reference.isStopped(), is(false));
        final List<MockExecutor.Submission> submissions = mExecutor.getSubmissions();
        assertThat("number of the executor submit invocations", submissions.size(), is(1));
        assertThat("1st submission is stopped", submissions.get(0).isStopped(), is(false));
    }

    public final void testPause() {
        assertThat("system is started", mSystem.isStarted(), is(true));
        assertThat("system is paused", mSystem.isPaused(), is(false));
        assertThat("system is stopped", mSystem.isStopped(), is(false));

        final Reference<Integer> reference = mSystem.with(isA(RandomString), new DummyActor<Integer>());
        assertThat("actor reference", reference, is(notNullValue()));

        assertThat("system pause", mSystem.pause(), is(true));
        assertThat("system is started", mSystem.isStarted(), is(false));
        assertThat("system is paused", mSystem.isPaused(), is(true));
        assertThat("system is stopped", mSystem.isStopped(), is(false));

        assertThat("actor is stopped", reference.isStopped(), is(false));
        final List<MockExecutor.Submission> submissions = mExecutor.getSubmissions();
        assertThat("number of the executor submit invocations", submissions.size(), is(1));
        assertThat("submission is stopped", submissions.get(0).isStopped(), is(true));
    }

    public final void testDoublePause() {
        assertThat("system is started", mSystem.isStarted(), is(true));
        assertThat("system is paused", mSystem.isPaused(), is(false));
        assertThat("system is stopped", mSystem.isStopped(), is(false));

        final Reference<Integer> reference = mSystem.with(isA(RandomString), new DummyActor<Integer>());
        assertThat("actor reference", reference, is(notNullValue()));

        assertThat("system pause", mSystem.pause(), is(true));
        assertThat("system pause", mSystem.pause(), is(true));
        assertThat("system is started", mSystem.isStarted(), is(false));
        assertThat("system is paused", mSystem.isPaused(), is(true));
        assertThat("system is stopped", mSystem.isStopped(), is(false));

        assertThat("actor is stopped", reference.isStopped(), is(false));
        final List<MockExecutor.Submission> submissions = mExecutor.getSubmissions();
        assertThat("number of the executor submit invocations", submissions.size(), is(1));
        assertThat("submission is stopped", submissions.get(0).isStopped(), is(true));
    }

    public final void testSubmitAfterPause() {
        assertThat("system pause", mSystem.pause(), is(true));
        assertThat("system is started", mSystem.isStarted(), is(false));
        assertThat("system is paused", mSystem.isPaused(), is(true));
        assertThat("system is stopped", mSystem.isStopped(), is(false));

        final Reference<Integer> reference = mSystem.with(isA(RandomString), new DummyActor<Integer>());
        assertThat("actor reference", reference, is(notNullValue()));

        assertThat("actor is stopped", reference.isStopped(), is(false));
        assertThat("executor submissions", mExecutor.getSubmissions(), is(empty()));
    }

    public final void testStartAfterPause() {
        assertThat("system is started", mSystem.isStarted(), is(true));
        assertThat("system is paused", mSystem.isPaused(), is(false));
        assertThat("system is stopped", mSystem.isStopped(), is(false));

        final Reference<Integer> reference = mSystem.with(isA(RandomString), new DummyActor<Integer>());
        assertThat("actor reference", reference, is(notNullValue()));

        assertThat("system pause", mSystem.pause(), is(true));
        assertThat("system start", mSystem.start(), is(true));
        assertThat("system is started", mSystem.isStarted(), is(true));
        assertThat("system is paused", mSystem.isPaused(), is(false));
        assertThat("system is stopped", mSystem.isStopped(), is(false));

        assertThat("actor is stopped", reference.isStopped(), is(false));
        final List<MockExecutor.Submission> submissions = mExecutor.getSubmissions();
        assertThat("number of the executor submit invocations", submissions.size(), is(2));
        assertThat("1st submission is stopped", submissions.get(0).isStopped(), is(true));
        assertThat("2nd submission is stopped", submissions.get(1).isStopped(), is(false));
    }

    public final void testStop() {
        assertThat("system is started", mSystem.isStarted(), is(true));
        assertThat("system is paused", mSystem.isPaused(), is(false));
        assertThat("system is stopped", mSystem.isStopped(), is(false));

        final Reference<Integer> reference = mSystem.with(isA(RandomString), new DummyActor<Integer>());
        assertThat("actor reference", reference, is(notNullValue()));

        assertThat("system stop", mSystem.stop(false), is(true));
        assertThat("system is started", mSystem.isStarted(), is(false));
        assertThat("system is paused", mSystem.isPaused(), is(false));
        assertThat("system is stopped", mSystem.isStopped(), is(true));

        assertThat("actor is stopped", reference.isStopped(), is(true));
        final List<MockExecutor.Submission> submissions = mExecutor.getSubmissions();
        assertThat("number of the executor submit invocations", submissions.size(), is(1));
        assertThat("submission is stopped", submissions.get(0).isStopped(), is(true));
    }

    public final void testDoubleStop() {
        assertThat("system is started", mSystem.isStarted(), is(true));
        assertThat("system is paused", mSystem.isPaused(), is(false));
        assertThat("system is stopped", mSystem.isStopped(), is(false));

        final Reference<Integer> reference = mSystem.with(isA(RandomString), new DummyActor<Integer>());
        assertThat("actor reference", reference, is(notNullValue()));

        assertThat("system stop", mSystem.stop(false), is(true));
        assertThat("system stop", mSystem.stop(false), is(true));
        assertThat("system is started", mSystem.isStarted(), is(false));
        assertThat("system is paused", mSystem.isPaused(), is(false));
        assertThat("system is stopped", mSystem.isStopped(), is(true));

        assertThat("actor is stopped", reference.isStopped(), is(true));
        final List<MockExecutor.Submission> submissions = mExecutor.getSubmissions();
        assertThat("number of the executor submit invocations", submissions.size(), is(1));
        assertThat("submission is stopped", submissions.get(0).isStopped(), is(true));
    }

    public final void testSubmitAfterStop() {
        assertThat("system is started", mSystem.isStarted(), is(true));
        assertThat("system is paused", mSystem.isPaused(), is(false));
        assertThat("system is stopped", mSystem.isStopped(), is(false));

        assertThat("system stop", mSystem.stop(false), is(true));
        try {
            mSystem.with(isA(RandomString), new DummyActor<>());
            fail("submitting of actor did not throw UnsupportedOperationException");
        } catch (final UnsupportedOperationException ignored) {/* expected */}

        assertThat("executor submissions", mExecutor.getSubmissions(), is(empty()));
    }

    public final void testStartAfterStop() {
        assertThat("system is started", mSystem.isStarted(), is(true));
        assertThat("system is paused", mSystem.isPaused(), is(false));
        assertThat("system is stopped", mSystem.isStopped(), is(false));

        assertThat("system stop", mSystem.stop(false), is(true));
        try {
            mSystem.start();
            fail("starting of system did not throw UnsupportedOperationException");
        } catch (final UnsupportedOperationException ignored) {/* expected */}
    }

    public final void testPauseAfterStop() {
        assertThat("system is started", mSystem.isStarted(), is(true));
        assertThat("system is paused", mSystem.isPaused(), is(false));
        assertThat("system is stopped", mSystem.isStopped(), is(false));

        assertThat("system stop", mSystem.stop(false), is(true));
        try {
            mSystem.pause();
            fail("pausing of system did not throw UnsupportedOperationException");
        } catch (final UnsupportedOperationException ignored) {/* expected */}
    }

    public final void testStopAfterPauseWithPendingMessages() {
        assertThat("system is started", mSystem.isStarted(), is(true));
        assertThat("system is paused", mSystem.isPaused(), is(false));
        assertThat("system is stopped", mSystem.isStopped(), is(false));

        final MockActor<Integer> actor = new MockActor<>();
        final Reference<Integer> reference = mSystem.with(isA(RandomString), actor);
        assertThat("actor reference", reference, is(notNullValue()));

        assertThat("system pause", mSystem.pause(), is(true));
        final int expected = isA(RandomInteger);
        assertThat("actor tell", reference.tell(expected), is(true));
        assertThat("system stop", mSystem.stop(false), is(true));
        assertThat("system is started", mSystem.isStarted(), is(false));
        assertThat("system is paused", mSystem.isPaused(), is(false));
        assertThat("system is stopped", mSystem.isStopped(), is(true));

        assertThat("actor is stopped", reference.isStopped(), is(true));
        final List<Pair<System, Integer>> onMessages = actor.getOnMessages();
        assertThat("number of actor on message invocations", onMessages.size(), is(1));

        final Pair<System, Integer> onMessage = onMessages.get(0);
        assertThat("actor received system", onMessage.first, is(mSystem));
        assertThat("actor received message", onMessage.second, is(expected));
    }

    public final void testImmediateStop() {
        assertThat("system is started", mSystem.isStarted(), is(true));
        assertThat("system is paused", mSystem.isPaused(), is(false));
        assertThat("system is stopped", mSystem.isStopped(), is(false));

        final Reference<Integer> reference = mSystem.with(isA(RandomString), new DummyActor<Integer>());
        assertThat("actor reference", reference, is(notNullValue()));

        assertThat("system stop", mSystem.stop(true), is(true));
        assertThat("system is started", mSystem.isStarted(), is(false));
        assertThat("system is paused", mSystem.isPaused(), is(false));
        assertThat("system is stopped", mSystem.isStopped(), is(true));

        assertThat("reference is stopped", reference.isStopped(), is(true));
        final List<MockExecutor.Submission> submissions = mExecutor.getSubmissions();
        assertThat("number of the executor submit invocations", submissions.size(), is(1));
        assertThat("submission is stopped", submissions.get(0).isStopped(), is(true));
    }

    public final void testDoubleImmediateStop() {
        assertThat("system is started", mSystem.isStarted(), is(true));
        assertThat("system is paused", mSystem.isPaused(), is(false));
        assertThat("system is stopped", mSystem.isStopped(), is(false));

        final Reference<Integer> reference = mSystem.with(isA(RandomString), new DummyActor<Integer>());
        assertThat("actor reference", reference, is(notNullValue()));

        assertThat("system stop", mSystem.stop(true), is(true));
        assertThat("system stop", mSystem.stop(true), is(true));
        assertThat("system is started", mSystem.isStarted(), is(false));
        assertThat("system is paused", mSystem.isPaused(), is(false));
        assertThat("system is stopped", mSystem.isStopped(), is(true));

        assertThat("reference is stopped", reference.isStopped(), is(true));
        final List<MockExecutor.Submission> submissions = mExecutor.getSubmissions();
        assertThat("number of the executor submit invocations", submissions.size(), is(1));
        assertThat("submission is stopped", submissions.get(0).isStopped(), is(true));
    }

    public final void testSubmitAfterImmediateStop() {
        assertThat("system is started", mSystem.isStarted(), is(true));
        assertThat("system is paused", mSystem.isPaused(), is(false));
        assertThat("system is stopped", mSystem.isStopped(), is(false));

        assertThat("system stop", mSystem.stop(true), is(true));
        try {
            mSystem.with(isA(RandomString), new DummyActor<>());
            fail("submitting of actor did not throw UnsupportedOperationException");
        } catch (final UnsupportedOperationException ignored) {/* expected */}

        assertThat("executor submissions", mExecutor.getSubmissions(), is(empty()));
    }

    public final void testStartAfterImmediateStop() {
        assertThat("system is started", mSystem.isStarted(), is(true));
        assertThat("system is paused", mSystem.isPaused(), is(false));
        assertThat("system is stopped", mSystem.isStopped(), is(false));

        assertThat("system stop", mSystem.stop(true), is(true));
        try {
            mSystem.start();
            fail("starting of system did not throw UnsupportedOperationException");
        } catch (final UnsupportedOperationException ignored) {/* expected */}
    }

    public final void testImmediateStopAfterPauseWithPendingMessages() {
        assertThat("system is started", mSystem.isStarted(), is(true));
        assertThat("system is paused", mSystem.isPaused(), is(false));
        assertThat("system is stopped", mSystem.isStopped(), is(false));

        final MockActor<Integer> actor = new MockActor<>();
        final Reference<Integer> reference = mSystem.with(isA(RandomString), actor);
        assertThat("actor reference", reference, is(notNullValue()));

        assertThat("system pause", mSystem.pause(), is(true));
        final int expected = isA(RandomInteger);
        assertThat("actor tell", reference.tell(expected), is(true));
        assertThat("system stop", mSystem.stop(true), is(true));
        assertThat("system is started", mSystem.isStarted(), is(false));
        assertThat("system is paused", mSystem.isPaused(), is(false));
        assertThat("system is stopped", mSystem.isStopped(), is(true));

        assertThat("actor is stopped", reference.isStopped(), is(true));
        assertThat("actor received messages", actor.getOnMessages(), is(empty()));
    }

    private static class MockExecutor implements Executor {

        private final List<Submission> mSubmissions = new ArrayList<>(1);

        private boolean mStopped = false;

        public MockExecutor() {
            super();
        }

        public final boolean isStopped() {
            return mStopped;
        }

        @NonNull
        public final List<Submission> getSubmissions() {
            return unmodifiableList(mSubmissions);
        }

        @Nullable
        @Override
        public final Submission submit(@NonNull final Executor.Task task) {
            final Submission submission = new Submission(task);
            mSubmissions.add(submission);
            return submission;
        }

        @Override
        public final boolean stop() {
            mStopped = true;
            return true;
        }

        public static class Submission implements Executor.Submission {

            @NonNull
            private final Executor.Task mTask;

            private boolean mStopped = false;

            public Submission(@NonNull final Executor.Task task) {
                super();

                mTask = task;
                mTask.attach(myLooper());
            }

            public final boolean isStopped() {
                return mStopped;
            }

            @NonNull
            public final Executor.Task getTask() {
                return mTask;
            }

            @Override
            public final boolean stop() {
                mTask.detach();
                mStopped = true;
                return true;
            }
        }
    }
}
