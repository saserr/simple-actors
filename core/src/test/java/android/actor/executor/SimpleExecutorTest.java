/*
 * Copyright 2016 the original author or authors
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

import android.actor.Channel;
import android.actor.Executor;
import android.actor.Providers;
import android.actor.TestCase;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import mockit.Injectable;
import mockit.StrictExpectations;

import static android.actor.executor.SimpleExecutor.EXECUTOR_STOPPED;
import static android.actor.executor.SimpleExecutorMatchers.empty;
import static android.actor.executor.SimpleExecutorMatchers.hasSize;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class SimpleExecutorTest extends TestCase {

    @Injectable
    private Executable mExecutable;
    @Injectable
    private Channel.Factory mFactory;

    private SimpleExecutor mExecutor;

    @BeforeMethod
    public final void setUp() {
        mExecutor = new SimpleExecutor();
    }

    @AfterMethod
    public final void tearDown() {
        verifyNoMoreInteractions(mExecutable, mFactory);
    }

    @Test(groups = {"sanity", "sanity.executor", "sanity.executor.simple"})
    public final void newlyCreatedSimpleExecutorIsEmpty() {
        assertThat("executor", mExecutor, is(empty()));
        assertThat("executor", mExecutor, hasSize(0));
    }

    @Test(groups = {"sanity", "sanity.executor", "sanity.executor.simple"})
    public final void submitBeforeStart() {
        assertThat("submission", mExecutor.submit(mExecutable), is(notNullValue()));
        assertThat("executor", mExecutor, is(not(empty())));
        assertThat("executor", mExecutor, hasSize(1));
    }

    @Test(dependsOnGroups = "sanity.executor.simple")
    public final void submitSameExecutableTwiceBeforeStart() {
        assertThat("first submission", mExecutor.submit(mExecutable), is(notNullValue()));
        assertThat("second submission", mExecutor.submit(mExecutable), is(nullValue()));
        assertThat("executor", mExecutor, is(not(empty())));
        assertThat("executor", mExecutor, hasSize(1));
    }

    @Test(dependsOnGroups = "sanity.executor.simple")
    public final void stopSubmissionBeforeStart() {
        final Executor.Submission submission = mExecutor.submit(mExecutable);
        assertThat("submission", submission, is(notNullValue()));

        assertThat("submission stop", notNull(submission).stop(), is(true));
        assertThat("executor", mExecutor, is(empty()));
        assertThat("executor", mExecutor, hasSize(0));
    }

    @Test(dependsOnGroups = "sanity.executor.simple",
            dependsOnMethods = "stopSubmissionBeforeStart")
    public final void stopSameSubmissionTwiceBeforeStart() {
        final Executor.Submission submission = mExecutor.submit(mExecutable);
        assertThat("submission", submission, is(notNullValue()));

        assertThat("first submission stop", notNull(submission).stop(), is(true));
        assertThat("second submission stop", notNull(submission).stop(), is(true));
    }

    @Test(groups = {"sanity", "sanity.executor", "sanity.executor.simple"},
            dependsOnMethods = "submitBeforeStart",
            dataProvider = "success or failure", dataProviderClass = Providers.class)
    public final void start(final Providers.Boolean success) {
        new StrictExpectations() {{
            mExecutable.attach(mFactory);
            result = success.value();
        }};

        assertThat("submission", mExecutor.submit(mExecutable), is(notNullValue()));
        assertThat("executor start", mExecutor.start(mFactory), is(success.value()));
    }

    @Test(groups = {"sanity", "sanity.executor", "sanity.executor.simple"},
            dataProvider = "success or failure", dataProviderClass = Providers.class)
    public final void submitAfterStart(final Providers.Boolean success) {
        new StrictExpectations() {{
            mExecutable.attach(mFactory);
            result = success.value();
        }};

        assertThat("executor start", mExecutor.start(mFactory), is(true));
        final Executor.Submission submission = mExecutor.submit(mExecutable);
        assertThat("submission", submission, is(success.value() ? notNullValue() : nullValue()));
        assertThat("executor", mExecutor, is(success.value() ? not(empty()) : empty()));
        assertThat("executor", mExecutor, hasSize(success.value() ? 1 : 0));
    }

    @Test(dependsOnGroups = "sanity.executor.simple")
    public final void submitSameExecutableTwiceAfterStart() {
        new StrictExpectations() {{
            mExecutable.attach(mFactory);
            result = true;
        }};

        assertThat("executor start", mExecutor.start(mFactory), is(true));
        assertThat("first submission", mExecutor.submit(mExecutable), is(notNullValue()));
        assertThat("second submission", mExecutor.submit(mExecutable), is(nullValue()));
        assertThat("executor", mExecutor, is(not(empty())));
        assertThat("executor", mExecutor, hasSize(1));
    }

    @Test(dependsOnGroups = "sanity.executor.simple",
            dataProvider = "success or failure", dataProviderClass = Providers.class)
    public final void stopSubmissionAfterStart(final Providers.Boolean success) {
        new StrictExpectations() {{
            mExecutable.attach(mFactory);
            result = true;
            mExecutable.detach();
            result = success.value();
        }};

        final Executor.Submission submission = mExecutor.submit(mExecutable);
        assertThat("submission", submission, is(notNullValue()));
        assertThat("executor start", mExecutor.start(mFactory), is(true));

        assertThat("submission stop", notNull(submission).stop(), is(success.value()));
        assertThat("executor", mExecutor, is(success.value() ? empty() : not(empty())));
        assertThat("executor", mExecutor, hasSize(success.value() ? 0 : 1));
    }

    @Test(dependsOnGroups = "sanity.executor.simple", dependsOnMethods = "stopSubmissionAfterStart")
    public final void stopSameSubmissionTwiceAfterStart() {
        new StrictExpectations() {{
            mExecutable.attach(mFactory);
            result = true;
            mExecutable.detach();
            result = true;
        }};

        final Executor.Submission submission = mExecutor.submit(mExecutable);
        assertThat("submission", submission, is(notNullValue()));
        assertThat("executor start", mExecutor.start(mFactory), is(true));

        assertThat("first submission stop", notNull(submission).stop(), is(true));
        assertThat("second submission stop", notNull(submission).stop(), is(true));
    }

    @Test(dependsOnGroups = "sanity.executor.simple")
    public final void stopBeforeStart() {
        assertThat("submission", mExecutor.submit(mExecutable), is(notNullValue()));
        mExecutor.stop();
    }

    @Test(dependsOnGroups = "sanity.executor.simple",
            dataProvider = "success or failure", dataProviderClass = Providers.class)
    public final void stopAfterStart(final Providers.Boolean success) {
        new StrictExpectations() {{
            mExecutable.attach(mFactory);
            result = true;
            mExecutable.detach();
            result = success.value();
        }};

        assertThat("submission", mExecutor.submit(mExecutable), is(notNullValue()));
        assertThat("executor start", mExecutor.start(mFactory), is(true));
        mExecutor.stop();
    }

    @Test(dependsOnGroups = "sanity.executor.simple", dependsOnMethods = "stopAfterStart")
    public final void doubleStopAfterStart() {
        new StrictExpectations() {{
            mExecutable.attach(mFactory);
            result = true;
            mExecutable.detach();
            result = true;
        }};

        assertThat("submission", mExecutor.submit(mExecutable), is(notNullValue()));
        assertThat("executor start", mExecutor.start(mFactory), is(true));
        mExecutor.stop();
        mExecutor.stop();
    }

    @Test(dependsOnGroups = "sanity.executor.simple", dependsOnMethods = "stopBeforeStart",
            expectedExceptions = UnsupportedOperationException.class,
            expectedExceptionsMessageRegExp = EXECUTOR_STOPPED)
    public final void submitAfterStop() {
        mExecutor.stop();
        mExecutor.submit(mExecutable);
    }

    @Test(dependsOnGroups = "sanity.executor.simple", dependsOnMethods = "stopBeforeStart",
            expectedExceptions = UnsupportedOperationException.class,
            expectedExceptionsMessageRegExp = EXECUTOR_STOPPED)
    public final void startAfterStop() {
        mExecutor.stop();
        mExecutor.start(mFactory);
    }
}
