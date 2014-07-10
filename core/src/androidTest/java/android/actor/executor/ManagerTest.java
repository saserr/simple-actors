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

package android.actor.executor;

import android.actor.Executor;
import android.os.Looper;
import android.support.annotation.NonNull;

import junit.framework.TestCase;

import static android.os.Looper.myLooper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class ManagerTest extends TestCase {

    private Manager mManager;

    @Override
    public final void setUp() throws Exception {
        super.setUp();

        mManager = new Manager();
        mManager.onStart(myLooper());
    }

    @Override
    public final void tearDown() throws Exception {
        mManager.onStop();
        mManager = null;

        super.tearDown();
    }

    public final void testSubmit() {
        final SpyExecutable executable = new SpyExecutable();
        assertThat("manager submit", mManager.submit(executable), is(notNullValue()));
        assertThat("number of the executable attach invocations", executable.getAttachments(), is(1));
        assertThat("number of the executable detach invocations", executable.getDetachments(), is(0));
    }

    public final void testDoubleSubmitExecutable() {
        final SpyExecutable executable = new SpyExecutable();
        assertThat("manager submit", mManager.submit(executable), is(notNullValue()));
        assertThat("manager submit", mManager.submit(executable), is(nullValue()));
        assertThat("number of the executable attach invocations", executable.getAttachments(), is(1));
        assertThat("number of the executable detach invocations", executable.getDetachments(), is(0));
    }

    public final void testSubmitExecutableThatFailsToAttach() {
        assertThat("manager submit", mManager.submit(new DummyExecutable(false, false)), is(nullValue()));
    }

    public final void testStopExecutableThatFailsToDetach() {
        final DummyExecutable executable = new DummyExecutable(true, false);
        final Executor.Submission submission = mManager.submit(executable);
        assertThat("manager submit", submission, is(notNullValue()));

        assert submission != null;
        assertThat("submission stop", submission.stop(), is(false));
    }

    public final void testStopSubmission() {
        final SpyExecutable executable = new SpyExecutable();
        final Executor.Submission submission = mManager.submit(executable);
        assertThat("manager submit", submission, is(notNullValue()));

        assert submission != null;
        assertThat("submission stop", submission.stop(), is(true));
        assertThat("number of the executable attach invocations", executable.getAttachments(), is(1));
        assertThat("number of the executable detach invocations", executable.getDetachments(), is(1));
    }

    public final void testDoubleStopSubmission() {
        final SpyExecutable executable = new SpyExecutable();
        final Executor.Submission submission = mManager.submit(executable);
        assertThat("manager submit", submission, is(notNullValue()));

        assert submission != null;
        assertThat("submission stop", submission.stop(), is(true));
        assertThat("submission stop", submission.stop(), is(true));
        assertThat("number of the executable attach invocations", executable.getAttachments(), is(1));
        assertThat("number of the executable detach invocations", executable.getDetachments(), is(1));
    }

    public final void testOnStop() {
        final SpyExecutable executable = new SpyExecutable();
        assertThat("manager submit", mManager.submit(executable), is(notNullValue()));

        assertThat("manager stop", mManager.onStop(), is(true));
        assertThat("number of the executable attach invocations", executable.getAttachments(), is(1));
        assertThat("number of the executable detach invocations", executable.getDetachments(), is(1));
    }

    public final void testSubmitAfterOnStop() {
        assertThat("manager stop", mManager.onStop(), is(true));

        final SpyExecutable executable = new SpyExecutable();
        assertThat("manager submit", mManager.submit(executable), is(notNullValue()));
        assertThat("number of the executable attach invocations", executable.getAttachments(), is(0));
        assertThat("number of the executable detach invocations", executable.getDetachments(), is(0));
    }

    public final void testStopSubmissionAfterOnStop() {
        final SpyExecutable executable = new SpyExecutable();
        final Executor.Submission submission = mManager.submit(executable);
        assertThat("manager submit", submission, is(notNullValue()));

        assert submission != null;
        assertThat("manager stop", mManager.onStop(), is(true));
        assertThat("submission stop", submission.stop(), is(true));
        assertThat("number of the executable attach invocations", executable.getAttachments(), is(1));
        assertThat("number of the executable detach invocations", executable.getDetachments(), is(1));
    }

    public final void testSubmitAfterOnStopAndBeforeOnStart() {
        assertThat("manager stop", mManager.onStop(), is(true));

        final SpyExecutable executable = new SpyExecutable();
        assertThat("manager submit", mManager.submit(executable), is(notNullValue()));

        assertThat("manager start", mManager.onStart(myLooper()), is(true));
        assertThat("number of the executable attach invocations", executable.getAttachments(), is(1));
        assertThat("number of the executable detach invocations", executable.getDetachments(), is(0));
    }

    public final void testSubmitExecutableThatFailsToAttachAfterOnStopAndBeforeOnStart() {
        assertThat("manager stop", mManager.onStop(), is(true));
        assertThat("manager submit", mManager.submit(new DummyExecutable(false, false)), is(notNullValue()));
        assertThat("manager start", mManager.onStart(myLooper()), is(false));
    }

    private static class DummyExecutable implements Executable {

        private final boolean mAttach;
        private final boolean mDetach;

        private DummyExecutable(final boolean attach, final boolean detach) {
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
    }

    private static class SpyExecutable implements Executable {

        private int mAttachments = 0;
        private int mDetachments = 0;

        private SpyExecutable() {
            super();
        }

        public final int getAttachments() {
            return mAttachments;
        }

        public final int getDetachments() {
            return mDetachments;
        }

        @Override
        public final boolean attach(@NonNull final Looper looper) {
            mAttachments++;
            return true;
        }

        @Override
        public final boolean detach() {
            mDetachments++;
            return true;
        }
    }
}
