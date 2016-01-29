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

import android.actor.Captured;
import android.actor.Channel;
import android.actor.Channels;
import android.actor.Providers;
import android.actor.TestCase;
import android.os.Looper;
import android.support.annotation.NonNull;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import mockit.Injectable;
import mockit.Mocked;
import mockit.StrictExpectations;

import static android.actor.Providers.booleans;
import static android.actor.executor.DispatcherMatchers.empty;
import static android.actor.executor.DispatcherMatchers.hasSize;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

public class DispatcherTest extends TestCase {

    @Mocked
    private Thread mAllThreads;
    @Mocked
    private Looper mAllLoopers;
    @Mocked
    private Channels mChannels;
    @Mocked
    private SimpleExecutor mAllSimpleExecutors;

    @Injectable
    private Executable mExecutable;

    private Looper mLooper;
    private Dispatcher mDispatcher;
    private SimpleExecutor mExecutor;

    @BeforeMethod
    public final void setUp() {
        new StrictExpectations() {{
            mExecutor = new SimpleExecutor();
        }};

        mDispatcher = new Dispatcher();
    }

    @AfterMethod
    public final void tearDown() {
        verifyNoMoreInteractions(mAllLoopers, mChannels, mAllSimpleExecutors);
        verifyNoMoreInteractions(mExecutor, mExecutable);
    }

    @Test(dependsOnGroups = "sanity.dispatcher", dataProvider = "emptiness")
    public final void isEmpty(final Providers.Boolean empty) {
        new StrictExpectations() {{
            mExecutor.isEmpty();
            result = empty.value();
        }};

        assertThat("dispatcher", mDispatcher, is(empty.value() ? empty() : not(empty())));
    }

    @Test(dependsOnGroups = "sanity.dispatcher")
    public final void size() {
        final int size = isA(RandomSize);

        new StrictExpectations() {{
            mExecutor.size();
            result = size;
        }};

        assertThat("dispatcher", mDispatcher, hasSize(size));
    }

    @Test(groups = {"sanity", "sanity.dispatcher"})
    public final void submit() {
        new StrictExpectations() {{
            mExecutor.submit(mExecutable);
        }};

        mDispatcher.submit(mExecutable);
    }

    @Test(groups = {"sanity", "sanity.dispatcher"},
            dataProvider = "success or failure", dataProviderClass = Providers.class)
    public final void start(final Providers.Boolean success) {
        final Runnable runnable = Captured.as(Runnable.class);

        new StrictExpectations() {{
            final Thread thread = new Thread(withArgThat(is(Captured.into(runnable))));
            thread.start();
            Looper.prepare();
            mLooper = Looper.myLooper();
            final Channel.Factory factory = Channels.from(mLooper);
            mExecutor.start(factory);
            result = success.value();
        }};

        if (success.value()) {
            new StrictExpectations() {{
                Looper.loop();
                mExecutor.stop();
            }};
        } else {
            new StrictExpectations() {{
                mLooper.quit();
            }};
        }

        mDispatcher.start();
        runnable.run();
    }

    @Test(dependsOnGroups = "sanity.dispatcher",
            expectedExceptions = IllegalStateException.class,
            expectedExceptionsMessageRegExp = "No looper associated with this thread")
    public final void noLooper() {
        final Runnable runnable = Captured.as(Runnable.class);

        new StrictExpectations() {{
            final Thread thread = new Thread(withArgThat(is(Captured.into(runnable))));
            thread.start();
            Looper.prepare();
            Looper.myLooper();
            result = null;
        }};

        mDispatcher.start();
        runnable.run();
    }

    private static final RandomDataGenerator<Integer> RandomSize =
            RandomInteger.thatIs(greaterThan(0));

    @NonNull
    @DataProvider(name = "emptiness")
    private static Object[][] emptiness() {
        return booleans("not empty", "empty");
    }
}
