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

import android.os.Looper;
import android.support.annotation.NonNull;

import junit.framework.TestCase;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class DispatcherTest extends TestCase {

    private SpyCallback mCallback;
    private Dispatcher mDispatcher;

    @Override
    public final void setUp() throws Exception {
        super.setUp();

        mCallback = new SpyCallback();
        mDispatcher = new Dispatcher(mCallback);
    }

    @Override
    public final void tearDown() throws Exception {
        mDispatcher.stop();
        mDispatcher = null;
        mCallback = null;

        super.tearDown();
    }

    public final void testWithoutStartOrStop() throws InterruptedException {
        assertThat("number of the callback start invocations", mCallback.getStarts(0), is(0));
        assertThat("number of the callback stop invocations", mCallback.getStops(0), is(0));
    }

    public final void testStopWithoutStart() throws InterruptedException {
        mDispatcher.stop();

        assertThat("number of the callback start invocations", mCallback.getStarts(0), is(0));
        assertThat("number of the callback stop invocations", mCallback.getStops(0), is(0));
    }

    public final void testStart() throws InterruptedException {
        mDispatcher.start();

        assertThat("number of the callback start invocations", mCallback.getStarts(1), is(1));
        assertThat("number of the callback stop invocations", mCallback.getStops(0), is(0));
    }

    public final void testDoubleStart() throws InterruptedException {
        mDispatcher.start();
        assertThat("number of the callback start invocations", mCallback.getStarts(1), is(1));

        mDispatcher.start();
        assertThat("number of the callback start invocations", mCallback.getStarts(1), is(1));
    }

    public final void testStop() throws InterruptedException {
        mDispatcher.start();
        assertThat("number of the callback start invocations", mCallback.getStarts(1), is(1));

        mDispatcher.stop();
        assertThat("number of the callback start invocations", mCallback.getStarts(1), is(1));
        assertThat("number of the callback stop invocations", mCallback.getStops(1), is(1));
    }

    public final void testDoubleStop() throws InterruptedException {
        mDispatcher.start();
        assertThat("number of the callback start invocations", mCallback.getStarts(1), is(1));

        mDispatcher.stop();
        assertThat("number of the callback stop invocations", mCallback.getStops(1), is(1));

        mDispatcher.stop();
        assertThat("number of the callback start invocations", mCallback.getStarts(1), is(1));
        assertThat("number of the callback stop invocations", mCallback.getStops(1), is(1));
    }

    private static class SpyCallback implements Dispatcher.Callback {

        private static final int MAX_WAITS = 10;

        private final Object mLock = new Object();

        private int mStarts = 0;
        private int mStops = 0;

        private SpyCallback() {
            super();
        }

        public final int getStarts(final int expected) throws InterruptedException {
            synchronized (mLock) {
                int waits = 0;
                while ((mStarts < expected) && (waits < MAX_WAITS)) {
                    mLock.wait(100);
                    waits++;
                }

                return mStarts;
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
        public final void onStart(@NonNull final Looper looper) {
            synchronized (mLock) {
                mStarts++;
                mLock.notifyAll();
            }
        }

        @Override
        public final void onStop() {
            synchronized (mLock) {
                mStops++;
                mLock.notifyAll();
            }
        }
    }
}
