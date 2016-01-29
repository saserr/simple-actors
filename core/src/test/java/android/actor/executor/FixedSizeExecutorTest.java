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

import android.actor.TestCase;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import mockit.Injectable;
import mockit.Mocked;
import mockit.StrictExpectations;

import static android.actor.executor.SimpleExecutor.EXECUTOR_STOPPED;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

public class FixedSizeExecutorTest extends TestCase {

    @Mocked
    private Dispatcher mAllDispatchers;

    @Injectable
    private Executable mExecutable;

    private FixedSizeExecutor mExecutor;
    private Dispatcher mDispatcher1;
    private Dispatcher mDispatcher2;

    @BeforeMethod
    public final void setUp() {
        new StrictExpectations() {{
            mDispatcher1 = new Dispatcher();
            mDispatcher1.start();
            mDispatcher2 = new Dispatcher();
            mDispatcher2.start();
        }};

        mExecutor = new FixedSizeExecutor(2);
    }

    @AfterMethod
    public final void tearDown() {
        verifyNoMoreInteractions(mAllDispatchers);
        verifyNoMoreInteractions(mExecutable, mDispatcher1, mDispatcher2);
    }

    @Test(dependsOnGroups = "sanity.executor.fixed",
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = "Size must be grater than zero")
    public final void sizeMustBePositive() {
        final int invalidSize = a(RandomInteger.thatIs(lessThanOrEqualTo(0)));
        new FixedSizeExecutor(invalidSize);
    }

    @Test(groups = {"sanity", "sanity.executor", "sanity.executor.fixed"})
    public final void submitOnEmptyDispatcher() {
        new StrictExpectations() {{
            mDispatcher1.isEmpty();
            result = true;
            mDispatcher1.submit(mExecutable);
        }};

        mExecutor.submit(mExecutable);
    }

    @Test(groups = {"sanity", "sanity.executor", "sanity.executor.fixed"})
    public final void submitOnLeastUsedDispatcher() {
        new StrictExpectations() {{
            mDispatcher1.isEmpty();
            result = false;
            mDispatcher2.size();
            result = 1;
            mDispatcher1.size();
            result = 2;
            mDispatcher2.submit(mExecutable);
        }};

        mExecutor.submit(mExecutable);
    }

    @Test(dependsOnGroups = "sanity.executor.fixed")
    public final void stop() {
        new StrictExpectations() {{
            mDispatcher1.stop();
            mDispatcher2.stop();
        }};

        mExecutor.stop();
    }

    @Test(dependsOnGroups = "sanity.executor.fixed", dependsOnMethods = "stop")
    public final void doubleStop() {
        new StrictExpectations() {{
            mDispatcher1.stop();
            mDispatcher2.stop();
        }};

        mExecutor.stop();
        mExecutor.stop();
    }

    @Test(dependsOnGroups = "sanity.executor.fixed", dependsOnMethods = "stop",
            expectedExceptions = UnsupportedOperationException.class,
            expectedExceptionsMessageRegExp = EXECUTOR_STOPPED)
    public final void submitAfterStop() {
        new StrictExpectations() {{
            mDispatcher1.stop();
            mDispatcher2.stop();
        }};

        mExecutor.stop();
        mExecutor.submit(mExecutable);
    }
}
