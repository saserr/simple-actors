/*
 * Copyright 2015 the original author or authors
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

import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

import java.lang.annotation.Retention;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static android.os.SystemClock.uptimeMillis;
import static java.lang.annotation.RetentionPolicy.SOURCE;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

@ThreadSafe
public class Scheduler extends Actor<Scheduler.Delay<?>> {

    @NonNull
    private final ScheduledExecutorService mExecutorService;
    @Owner
    private final int mExecutorServiceOwner;

    private final Lock mLock = new ReentrantLock();

    // TODO use open linked list with O(1) prepend and remove instead of HashSet
    @GuardedBy("mLock")
    private final Collection<Future<?>> mTasks = new ArrayList<>();

    public Scheduler() {
        this(newSingleThreadScheduledExecutor(), Owner.THIS);
    }

    public Scheduler(@NonNull final ScheduledExecutorService service) {
        this(service, Owner.SOMEONE_ELSE);
    }

    private Scheduler(@NonNull final ScheduledExecutorService service,
                      @Owner final int executorServiceOwner) {
        super();

        mExecutorService = service;
        mExecutorServiceOwner = executorServiceOwner;
    }

    @Override
    protected final void onMessage(@NonNull final Delay<?> delay) {
        final Future<?> task = delay.schedule(this, mExecutorService);
        if (task != null) {
            mLock.lock();
            try {
                if (!task.isDone()) {
                    mTasks.add(task);
                }
            } finally {
                mLock.unlock();
            }
        }
    }

    @Override
    protected final void preStop() {
        mLock.lock();
        try {
            for (final Future<?> task : mTasks) {
                task.cancel(true);
            }
            mTasks.clear();
        } finally {
            mLock.unlock();
        }

        if (mExecutorServiceOwner == Owner.THIS) {
            mExecutorService.shutdownNow();
        }
    }

    private void remove(@NonNull final Future<?> task) {
        mLock.lock();
        try {
            mTasks.remove(task);
        } finally {
            mLock.unlock();
        }
    }

    @NonNull
    public static <M> Delay<M> delay(@NonNull final Reference<M> reference,
                                     @NonNull final M message,
                                     final long duration,
                                     @NonNull final TimeUnit unit) {
        return new Delay<>(reference, message, duration, unit);
    }

    public static final class Delay<M> {

        @NonNull
        private final Reference<M> mReference;
        @NonNull
        private final M mMessage;
        private final long mAtTime;

        private Delay(@NonNull final Reference<M> reference,
                      @NonNull final M message,
                      final long duration,
                      @NonNull final TimeUnit unit) {
            super();

            mReference = reference;
            mMessage = message;
            mAtTime = uptimeMillis() + MILLISECONDS.convert(duration, unit);
        }

        @Nullable
        public Future<?> schedule(@NonNull final Scheduler scheduler,
                                  @NonNull final ScheduledExecutorService service) {
            final long remaining = mAtTime - uptimeMillis();
            @org.jetbrains.annotations.Nullable final ScheduledFuture<?> result;
            final Tell<M> tell = new Tell<>(scheduler, mReference, mMessage);

            if (remaining <= 0) {
                tell.run();
                result = null;
            } else {
                result = service.schedule(tell, remaining, MILLISECONDS);
                tell.setResult(result);
            }

            return result;
        }

        private static final class Tell<M> implements Runnable {

            @NonNull
            private final Scheduler mScheduler;
            @NonNull
            private final Reference<M> mReference;
            @NonNull
            private final M mMessage;

            private final Lock mLock = new ReentrantLock();

            @GuardedBy("mLock")
            private boolean mDone = false;
            @Nullable
            @GuardedBy("mLock")
            private Future<?> mResult;

            private Tell(@NonNull final Scheduler scheduler,
                         @NonNull final Reference<M> reference,
                         @NonNull final M message) {
                super();

                mScheduler = scheduler;
                mReference = reference;
                mMessage = message;
            }

            public void setResult(@NonNull final Future<?> result) {
                mLock.lock();
                try {
                    if (mDone) {
                        mScheduler.remove(result);
                    } else {
                        mResult = result;
                    }
                } finally {
                    mLock.unlock();
                }
            }

            @Override
            public void run() {
                try {
                    mReference.tell(mMessage);
                } finally {
                    mLock.lock();
                    try {
                        mDone = true;
                        if (mResult != null) {
                            mScheduler.remove(mResult);
                            mResult = null;
                        }
                    } finally {
                        mLock.unlock();
                    }
                }
            }
        }
    }

    @Retention(SOURCE)
    @IntDef({Owner.THIS, Owner.SOMEONE_ELSE})
    private @interface Owner {
        int THIS = 1;
        int SOMEONE_ELSE = 2;
    }
}
