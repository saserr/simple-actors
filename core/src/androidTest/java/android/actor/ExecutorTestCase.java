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

import android.os.Looper;
import android.support.annotation.NonNull;

import junit.framework.TestCase;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public abstract class ExecutorTestCase extends TestCase {

    private Executor mExecutor;

    @NonNull
    protected abstract Executor create();

    @Override
    public final void setUp() throws Exception {
        super.setUp();

        mExecutor = create();
    }

    @Override
    public final void tearDown() throws Exception {
        mExecutor.stop();
        mExecutor = null;

        super.tearDown();
    }

    public final void testSubmit() throws InterruptedException {
        final SpyTask task = new SpyTask();
        assertThat("executor submit", mExecutor.submit(task), is(notNullValue()));
        assertThat("number of the task attach invocations", task.getAttachments(1), is(1));
        assertThat("number of the task detach invocations", task.getDetachments(0), is(0));
        assertThat("number of the task stop invocations", task.getStops(0), is(0));
    }

    public final void testDoubleSubmitTask() throws InterruptedException {
        final SpyTask task = new SpyTask();
        assertThat("executor submit", mExecutor.submit(task), is(notNullValue()));
        assertThat("executor submit", mExecutor.submit(task), is(nullValue()));
        assertThat("number of the task attach invocations", task.getAttachments(1), is(1));
        assertThat("number of the task detach invocations", task.getDetachments(0), is(0));
        assertThat("number of the task stop invocations", task.getStops(0), is(0));
    }

    public final void testSubmitTaskThatFailsToAttach() {
        final DummyTask task = new DummyTask(false, false);
        assertThat("executor submit", mExecutor.submit(task), is(nullValue()));
    }

    public final void testStopTaskThatFailsToDetach() {
        final DummyTask task = new DummyTask(true, false);
        final Executor.Submission submission = mExecutor.submit(task);
        assertThat("executor submit", submission, is(notNullValue()));

        assert submission != null;
        assertThat("submission stop", submission.stop(), is(false));
    }

    public final void testStopSubmission() throws InterruptedException {
        final SpyTask task = new SpyTask();
        final Executor.Submission submission = mExecutor.submit(task);
        assertThat("executor submit", submission, is(notNullValue()));
        assertThat("number of the task attach invocations", task.getAttachments(1), is(1));

        assert submission != null;
        assertThat("submission stop", submission.stop(), is(true));
        assertThat("number of the task attach invocations", task.getAttachments(1), is(1));
        assertThat("number of the task detach invocations", task.getDetachments(1), is(1));
        assertThat("number of the task stop invocations", task.getStops(0), is(0));
    }

    public final void testDoubleStopSubmission() throws InterruptedException {
        final SpyTask task = new SpyTask();
        final Executor.Submission submission = mExecutor.submit(task);
        assertThat("executor submit", submission, is(notNullValue()));
        assertThat("number of the task attach invocations", task.getAttachments(1), is(1));

        assert submission != null;
        assertThat("submission stop", submission.stop(), is(true));
        assertThat("number of the task detach invocations", task.getDetachments(1), is(1));

        assertThat("submission stop", submission.stop(), is(true));
        assertThat("number of the task attach invocations", task.getAttachments(1), is(1));
        assertThat("number of the task detach invocations", task.getDetachments(1), is(1));
        assertThat("number of the task stop invocations", task.getStops(0), is(0));
    }

    public final void testStopExecutor() throws InterruptedException {
        final SpyTask task = new SpyTask();
        assertThat("executor submit", mExecutor.submit(task), is(notNullValue()));
        assertThat("number of the task attach invocations", task.getAttachments(1), is(1));

        assertThat("executor stop", mExecutor.stop(), is(true));
        assertThat("number of the task attach invocations", task.getAttachments(1), is(1));
        assertThat("number of the task detach invocations", task.getDetachments(0), is(0));
        assertThat("number of the task stop invocations", task.getStops(1), is(1));
    }

    public final void testDoubleStopExecutor() throws InterruptedException {
        final SpyTask task = new SpyTask();
        assertThat("executor submit", mExecutor.submit(task), is(notNullValue()));
        assertThat("number of the task attach invocations", task.getAttachments(1), is(1));

        assertThat("executor stop", mExecutor.stop(), is(true));
        assertThat("number of the task stop invocations", task.getStops(1), is(1));

        assertThat("executor stop", mExecutor.stop(), is(true));
        assertThat("number of the task attach invocations", task.getAttachments(1), is(1));
        assertThat("number of the task detach invocations", task.getDetachments(0), is(0));
        assertThat("number of the task stop invocations", task.getStops(1), is(1));
    }

    public final void testStopSubmissionAndExecutor() throws InterruptedException {
        final SpyTask task = new SpyTask();
        final Executor.Submission submission = mExecutor.submit(task);
        assertThat("executor submit", submission, is(notNullValue()));
        assertThat("number of the task attach invocations", task.getAttachments(1), is(1));

        assert submission != null;
        assertThat("submission stop", submission.stop(), is(true));
        assertThat("number of the task detach invocations", task.getDetachments(1), is(1));

        assertThat("executor stop", mExecutor.stop(), is(true));
        assertThat("number of the task attach invocations", task.getAttachments(1), is(1));
        assertThat("number of the task detach invocations", task.getDetachments(1), is(1));
        assertThat("number of the task stop invocations", task.getStops(0), is(0));
    }

    public final void testSubmitAfterStop() throws InterruptedException {
        assertThat("executor stop", mExecutor.stop(), is(true));
        final SpyTask task = new SpyTask();
        try {
            assertThat("executor submit", mExecutor.submit(task), is(notNullValue()));
            fail("submitting of task did not throw UnsupportedOperationException");
        } catch (final UnsupportedOperationException ignored) {/* expected */}

        assertThat("number of the task attach invocations", task.getAttachments(0), is(0));
        assertThat("number of the task detach invocations", task.getDetachments(0), is(0));
        assertThat("number of the task stop invocations", task.getStops(0), is(0));
    }

    private static class DummyTask implements Executor.Task {

        private final boolean mAttach;
        private final boolean mDetach;

        private DummyTask(final boolean attach, final boolean detach) {
            super();

            mAttach = attach;
            mDetach = detach;
        }

        @Override
        public final boolean attach(@NonNull final Looper looper) {
            return mAttach;
        }

        @Override
        public final boolean detach() {
            return mDetach;
        }

        @Override
        public final boolean stop() {
            return true;
        }
    }

    private static class SpyTask implements Executor.Task {

        private static final int MAX_WAITS = 10;

        private final Object mLock = new Object();

        private int mAttachments = 0;
        private int mDetachments = 0;
        private int mStops = 0;

        private SpyTask() {
            super();
        }

        public final int getAttachments(final int expected) throws InterruptedException {
            synchronized (mLock) {
                int waits = 0;
                while ((mAttachments < expected) && (waits < MAX_WAITS)) {
                    mLock.wait(100);
                    waits++;
                }

                return mAttachments;
            }
        }

        public final int getDetachments(final int expected) throws InterruptedException {
            synchronized (mLock) {
                int waits = 0;
                while ((mDetachments < expected) && (waits < MAX_WAITS)) {
                    mLock.wait(100);
                    waits++;
                }

                return mDetachments;
            }
        }

        public final int getStops(final int expected) throws InterruptedException {
            synchronized (mLock) {
                int waits = 0;
                while ((mStops < expected) && (waits < MAX_WAITS)) {
                    mLock.wait(100);
                    waits++;
                }

                return mStops;
            }
        }

        @Override
        public final boolean attach(@NonNull final Looper looper) {
            synchronized (mLock) {
                mAttachments++;
                mLock.notifyAll();
            }

            return true;
        }

        @Override
        public final boolean detach() {
            synchronized (mLock) {
                mDetachments++;
                mLock.notifyAll();
            }

            return true;
        }

        @Override
        public final boolean stop() {
            synchronized (mLock) {
                mStops++;
                mLock.notifyAll();
            }

            return true;
        }
    }
}
