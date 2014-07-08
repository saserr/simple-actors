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
import android.actor.ExecutorTestCase;
import android.os.Looper;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

public class FixedSizeExecutorTest extends ExecutorTestCase {

    @NonNull
    @Override
    protected final Executor create() {
        return new FixedSizeExecutor(1);
    }

    public final void testNonPositiveSize() {
        try {
            final FixedSizeExecutor executor = new FixedSizeExecutor(0);
            assertThat("executor stop", executor.stop(), is(true));
            fail("creation of executor did not throw IllegalArgumentException");
        } catch (final IllegalArgumentException ignored) {/* expected */}
    }

    public final void testTasksGetDifferentLoopers() throws InterruptedException {
        final FixedSizeExecutor executor = new FixedSizeExecutor(2);
        final SpyTask task1 = new SpyTask();
        final SpyTask task2 = new SpyTask();
        assertThat("executor submit 1st task", executor.submit(task1), is(notNullValue()));
        assertThat("executor submit 2nd task", executor.submit(task2), is(notNullValue()));

        final List<Looper> attachments1 = task1.getAttachments(1);
        assertThat("number of the 1st task attach invocations", attachments1.size(), is(1));

        final List<Looper> attachments2 = task2.getAttachments(1);
        assertThat("number of the 1st task attach invocations", attachments2.size(), is(1));

        assertThat("1st task looper", attachments1.get(0), is(not(attachments2.get(0))));
    }

    private static class SpyTask implements Executor.Task {

        private static final int MAX_WAITS = 10;

        private final Object mLock = new Object();
        private final List<Looper> mAttachments = new ArrayList<>(1);

        private SpyTask() {
            super();
        }

        @NonNull
        public final List<Looper> getAttachments(final int expectedSize) throws InterruptedException {
            synchronized (mLock) {
                int waits = 0;
                while ((mAttachments.size() < expectedSize) && (waits < MAX_WAITS)) {
                    mLock.wait(100);
                    waits++;
                }

                return new ArrayList<>(mAttachments);
            }
        }

        @Override
        public final boolean attach(@NonNull final Looper looper) {
            synchronized (mLock) {
                mAttachments.add(looper);
                mLock.notifyAll();
            }

            return true;
        }

        @Override
        public final boolean detach() {
            return true;
        }

        @Override
        public final boolean stop() {
            return true;
        }
    }
}
