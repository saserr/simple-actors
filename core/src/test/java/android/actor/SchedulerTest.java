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

package android.actor;

import android.os.SystemClock;

import org.hamcrest.Matchers;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.regex.Matcher;

import mockit.Injectable;
import mockit.Mocked;
import mockit.StrictExpectations;

import static android.actor.Scheduler.delay;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

public class SchedulerTest extends TestCase {

    @Mocked
    private SystemClock mSystemClock;

    @Injectable
    private Reference<String> mReference;
    @Injectable
    private ScheduledExecutorService mExecutorService;

    private Scheduler mScheduler;

    @BeforeMethod
    public final void setUp() {
        mScheduler = new Scheduler(mExecutorService);
    }

    @AfterMethod
    public final void tearDown() {
        verifyNoMoreInteractions(mSystemClock);
        verifyNoMoreInteractions(mReference, mExecutorService);
    }

    @Test(groups = {"sanity", "sanity.scheduler"})
    public final void tellImmediately() {
        final String message = isA(RandomMessage);
        final long delay = isA(RandomDelay);

        new StrictExpectations() {{
            mReference.isStopped();
            result = false;
            SystemClock.uptimeMillis();
            result = new long[]{0, delay};
            mReference.tell(message);
        }};

        mScheduler.onMessage(delay(mReference, message, delay, MILLISECONDS));
    }

    @Test(groups = {"sanity", "sanity.scheduler"})
    public final void tellAfterDelay() {
        final String message = isA(RandomMessage);
        final long delay = isA(RandomDelay);
        final Runnable runnable = Captured.as(Runnable.class);

        new StrictExpectations() {{
            mReference.isStopped();
            result = false;
            SystemClock.uptimeMillis();
            result = new long[]{0, 0};
            mExecutorService.schedule(withArgThat(is(Captured.into(runnable))), delay, MILLISECONDS);
            mReference.tell(message);
        }};

        mScheduler.onMessage(delay(mReference, message, delay, MILLISECONDS));
        runnable.run();
    }

    @Test(dependsOnGroups = "sanity.scheduler")
    public final void preStopWithPendingTell() {
        final long delay = isA(RandomDelay);

        new StrictExpectations() {{
            mReference.isStopped();
            result = false;
            SystemClock.uptimeMillis();
            result = new long[]{0, 0};
            final Future<?> future = mExecutorService.schedule((Runnable) any, delay, MILLISECONDS);
            future.isDone();
            result = false;
            future.cancel(true);
        }};

        mScheduler.onMessage(delay(mReference, a(RandomMessage), delay, MILLISECONDS));
        mScheduler.preStop();
    }

    private static final RandomDataGenerator<Long> RandomDelay = RandomLong.thatIs(greaterThan(0L));
    private static final RandomDataGenerator<String> RandomMessage = RandomString;
}
