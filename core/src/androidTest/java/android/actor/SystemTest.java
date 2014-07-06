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

import java.util.List;

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
        assertThat("system actor", mSystem.with(isA(RandomString), new DummyActor<>()), is(notNullValue()));
        final List<MockExecutor.Submission> submissions = mExecutor.getSubmissions();
        assertThat("number of the executor submit invocations", submissions.size(), is(1));
        assertThat("submission is stopped", submissions.get(0).isStopped(), is(false));
    }

    public final void testSubmitSameName() {
        final String name = isA(RandomString);
        assertThat("system actor", mSystem.with(name, new DummyActor<>()), is(notNullValue()));
        try {
            mSystem.with(name, new DummyActor<>());
            fail("submitting of actor did not throw IllegalArgumentException");
        } catch (final IllegalArgumentException ignored) {/* expected */}

        final List<MockExecutor.Submission> submissions = mExecutor.getSubmissions();
        assertThat("number of the executor submit invocations", submissions.size(), is(1));
        assertThat("submission is stopped", submissions.get(0).isStopped(), is(false));
    }

    public final void testSubmitSameNameAfterActorStop() {
        final String name = isA(RandomString);
        final Reference<Object> reference = mSystem.with(name, new DummyActor<>());
        assertThat("system actor", reference, is(notNullValue()));

        assertThat("actor stop", reference.stop(), is(true));
        mSystem.with(name, new DummyActor<>());

        final List<MockExecutor.Submission> submissions = mExecutor.getSubmissions();
        assertThat("number of the executor submit invocations", submissions.size(), is(2));
        assertThat("submission is stopped", submissions.get(0).isStopped(), is(true));
        assertThat("submission is stopped", submissions.get(1).isStopped(), is(false));
    }

    public final void testStop() {
        final Reference<Integer> reference = mSystem.with(isA(RandomString), new DummyActor<Integer>());
        assertThat("system actor", reference, is(notNullValue()));

        mSystem.stop(false);

        assertThat("actor is stopped", reference.isStopped(), is(true));
        final List<MockExecutor.Submission> submissions = mExecutor.getSubmissions();
        assertThat("number of the executor submit invocations", submissions.size(), is(1));
        assertThat("submission is stopped", submissions.get(0).isStopped(), is(true));
    }

    public final void testSubmitAfterStop() {
        mSystem.stop(false);
        try {
            mSystem.with(isA(RandomString), new DummyActor<>());
            fail("submitting of actor did not throw UnsupportedOperationException");
        } catch (final UnsupportedOperationException ignored) {/* expected */}

        assertThat("executor submissions", mExecutor.getSubmissions(), is(empty()));
    }

    public final void testImmediateStop() {
        final Reference<Integer> reference = mSystem.with(isA(RandomString), new DummyActor<Integer>());
        assertThat("system actor", reference, is(notNullValue()));

        mSystem.stop(true);

        assertThat("reference is stopped", reference.isStopped(), is(true));
        final List<MockExecutor.Submission> submissions = mExecutor.getSubmissions();
        assertThat("number of the executor submit invocations", submissions.size(), is(1));
        assertThat("submission is stopped", submissions.get(0).isStopped(), is(true));
    }

    public final void testSubmitAfterImmediateStop() {
        mSystem.stop(true);
        try {
            mSystem.with(isA(RandomString), new DummyActor<>());
            fail("submitting of actor did not throw UnsupportedOperationException");
        } catch (final UnsupportedOperationException ignored) {/* expected */}

        assertThat("executor submissions", mExecutor.getSubmissions(), is(empty()));
    }
}
