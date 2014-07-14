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
import android.actor.executor.Messenger;
import android.actor.util.Wait;
import android.support.annotation.NonNull;

import junit.framework.TestCase;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public abstract class ExecutorTestCase extends TestCase {

    private Executor mExecutor;

    @NonNull
    protected abstract Executor create() throws InterruptedException;

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
        final SpyExecutable executable = new SpyExecutable();
        assertThat("executor submit", mExecutor.submit(executable), is(notNullValue()));
        assertThat("number of the executable attach invocations", executable.getAttachments(1), is(1));
        assertThat("number of the executable detach invocations", executable.getDetachments(0), is(0));
    }

    public final void testDoubleSubmitExecutable() throws InterruptedException {
        final SpyExecutable executable = new SpyExecutable();
        assertThat("executor submit", mExecutor.submit(executable), is(notNullValue()));
        assertThat("executor submit", mExecutor.submit(executable), is(nullValue()));
        assertThat("number of the executable attach invocations", executable.getAttachments(1), is(1));
        assertThat("number of the executable detach invocations", executable.getDetachments(0), is(0));
    }

    public final void testStopSubmission() throws InterruptedException {
        final SpyExecutable executable = new SpyExecutable();
        final Executor.Submission submission = mExecutor.submit(executable);
        assertThat("executor submit", submission, is(notNullValue()));

        assert submission != null;
        assertThat("submission stop", submission.stop(), is(true));
        assertThat("number of the executable attach invocations", executable.getAttachments(1), is(1));
        assertThat("number of the executable detach invocations", executable.getDetachments(1), is(1));
    }

    public final void testDoubleStopSubmission() throws InterruptedException {
        final SpyExecutable executable = new SpyExecutable();
        final Executor.Submission submission = mExecutor.submit(executable);
        assertThat("executor submit", submission, is(notNullValue()));

        assert submission != null;
        assertThat("submission stop", submission.stop(), is(true));
        assertThat("submission stop", submission.stop(), is(true));
        assertThat("number of the executable attach invocations", executable.getAttachments(1), is(1));
        assertThat("number of the executable detach invocations", executable.getDetachments(1), is(1));
    }

    public final void testStopExecutor() throws InterruptedException {
        final SpyExecutable executable = new SpyExecutable();
        assertThat("executor submit", mExecutor.submit(executable), is(notNullValue()));

        mExecutor.stop();
        assertThat("number of the executable attach invocations", executable.getAttachments(1), is(1));
        assertThat("number of the executable detach invocations", executable.getDetachments(1), is(1));
    }

    public final void testDoubleStopExecutor() throws InterruptedException {
        final SpyExecutable executable = new SpyExecutable();
        assertThat("executor submit", mExecutor.submit(executable), is(notNullValue()));

        mExecutor.stop();
        mExecutor.stop();
        assertThat("number of the executable attach invocations", executable.getAttachments(1), is(1));
        assertThat("number of the executable detach invocations", executable.getDetachments(1), is(1));
    }

    public final void testStopSubmissionAndExecutor() throws InterruptedException {
        final SpyExecutable executable = new SpyExecutable();
        final Executor.Submission submission = mExecutor.submit(executable);
        assertThat("executor submit", submission, is(notNullValue()));

        assert submission != null;
        assertThat("submission stop", submission.stop(), is(true));
        mExecutor.stop();
        assertThat("number of the executable attach invocations", executable.getAttachments(1), is(1));
        assertThat("number of the executable detach invocations", executable.getDetachments(1), is(1));
    }

    public final void testSubmitAfterStop() throws InterruptedException {
        mExecutor.stop();
        final SpyExecutable executable = new SpyExecutable();
        try {
            assertThat("executor submit", mExecutor.submit(executable), is(notNullValue()));
            fail("submitting executable did not throw UnsupportedOperationException");
        } catch (final UnsupportedOperationException ignored) {/* expected */}

        assertThat("number of the executable attach invocations", executable.getAttachments(0), is(0));
        assertThat("number of the executable detach invocations", executable.getDetachments(0), is(0));
    }

    private static class SpyExecutable implements Executable {

        private final Object mLock = new Object();

        private int mAttachments = 0;
        private int mDetachments = 0;

        private SpyExecutable() {
            super();
        }

        public final int getAttachments(final int expected) throws InterruptedException {
            synchronized (mLock) {
                int waits = 0;
                while ((mAttachments < expected) && (waits < Wait.MAX_REPEATS)) {
                    mLock.wait(Wait.DELAY);
                    waits++;
                }

                return mAttachments;
            }
        }

        public final int getDetachments(final int expected) throws InterruptedException {
            synchronized (mLock) {
                int waits = 0;
                while ((mDetachments < expected) && (waits < Wait.MAX_REPEATS)) {
                    mLock.wait(Wait.DELAY);
                    waits++;
                }

                return mDetachments;
            }
        }

        @Override
        public final boolean attach(@NonNull final Messenger.Factory factory) {
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
    }
}
