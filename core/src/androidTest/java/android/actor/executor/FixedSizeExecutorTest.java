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
import android.actor.Executors;
import android.actor.Messenger;
import android.actor.util.Wait;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

public class FixedSizeExecutorTest extends ExecutorTestCase {

    private final Object mLock = new Object();
    private boolean mInitializing = true;

    @NonNull
    @Override
    protected final Executor create() throws InterruptedException {
        final Executor executor = Executors.fixedSize(1);

        final Executor.Submission submission = executor.submit(new Executable() {

            @Override
            public boolean attach(@NonNull final Messenger.Factory factory) {
                synchronized (mLock) {
                    mInitializing = false;
                    mLock.notifyAll();
                }
                return true;
            }

            @Override
            public boolean detach() {
                return true;
            }
        });

        if (submission == null) {
            fail("Couldn't start a fixed size executor");
        } else {
            try {
                synchronized (mLock) {
                    int waits = 0;
                    while (mInitializing && (waits < Wait.MAX_REPEATS)) {
                        mLock.wait(Wait.DELAY);
                        waits++;
                    }
                }
            } finally {
                submission.stop();
            }
        }

        return executor;
    }

    public final void testNonPositiveSize() {
        try {
            final Executor executor = Executors.fixedSize(0);
            executor.stop();
            fail("creation of executor did not throw IllegalArgumentException");
        } catch (final IllegalArgumentException ignored) {/* expected */}
    }

    public final void testExecutablesGetDifferentMessengerFactories() throws InterruptedException {
        final Executor executor = Executors.fixedSize(2);
        final SpyExecutable executable1 = new SpyExecutable();
        final SpyExecutable executable2 = new SpyExecutable();
        assertThat("executor submit 1st executable", executor.submit(executable1), is(notNullValue()));
        assertThat("executor submit 2nd executable", executor.submit(executable2), is(notNullValue()));

        final List<Messenger.Factory> attachments1 = executable1.getAttachments(1);
        assertThat("number of the 1st executable attach invocations", attachments1.size(), is(1));

        final List<Messenger.Factory> attachments2 = executable2.getAttachments(1);
        assertThat("number of the 1st executable attach invocations", attachments2.size(), is(1));

        assertThat("1st executable looper", attachments1.get(0), is(not(attachments2.get(0))));
    }

    private static class SpyExecutable implements Executable {

        private final Object mLock = new Object();
        private final List<Messenger.Factory> mAttachments = new ArrayList<>(1);

        private SpyExecutable() {
            super();
        }

        @NonNull
        public final List<Messenger.Factory> getAttachments(final int expectedSize) throws InterruptedException {
            synchronized (mLock) {
                int waits = 0;
                while ((mAttachments.size() < expectedSize) && (waits < Wait.MAX_REPEATS)) {
                    mLock.wait(Wait.DELAY);
                    waits++;
                }

                return new ArrayList<>(mAttachments);
            }
        }

        @Override
        public final boolean attach(@NonNull final Messenger.Factory factory) {
            synchronized (mLock) {
                mAttachments.add(factory);
                mLock.notifyAll();
            }

            return true;
        }

        @Override
        public final boolean detach() {
            return true;
        }
    }
}
