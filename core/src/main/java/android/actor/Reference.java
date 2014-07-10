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

import android.actor.executor.Executable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.jetbrains.annotations.NonNls;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static android.os.SystemClock.uptimeMillis;
import static java.lang.Thread.currentThread;

public class Reference<M> implements Executable {

    private static final String TAG = Reference.class.getSimpleName();

    private static final long NO_DELAY = 0;

    private static final int SYSTEM_MESSAGE = 1;
    private static final int USER_MESSAGE = 2;

    private static final int PAUSE = 1;
    private static final int STOP = 2;

    @NonNull
    private final System mSystem;
    @NonNls
    @NonNull
    private final String mName;
    @NonNull
    private final Actor<M> mActor;
    @NonNls
    @NonNull
    private final String mActorStopped;

    private final Lock mLock = new ReentrantLock();
    private final Queue<PendingMessage<M>> mPendingMessages = new LinkedList<>();

    @Nullable
    private Task<M> mTask;
    @Nullable
    private Handler mHandler;
    private boolean mStopped = false;

    public Reference(@NonNull final System system,
                     @NonNls @NonNull final String name,
                     @NonNull final Actor<M> actor) {
        super();

        mSystem = system;
        mName = name;
        mActor = actor;
        mActorStopped = this + " is stopped";
    }

    @NonNls
    @NonNull
    public final String getName() {
        return mName;
    }

    public final boolean isStopped() {
        final boolean result;

        mLock.lock();
        try {
            result = mStopped;
        } finally {
            mLock.unlock();
        }

        return result;
    }

    public final boolean tell(@NonNull final M message) {
        final boolean success;

        mLock.lock();
        try {
            if (mStopped) {
                throw new UnsupportedOperationException(mActorStopped);
            }

            if (mHandler == null) {
                success = mPendingMessages.offer(new PendingMessage.User<>(message));
            } else {
                success = (mTask == null) ?
                        send(mHandler, message, NO_DELAY) :
                        mTask.send(mHandler, message) || send(mHandler, message, NO_DELAY);
            }
        } finally {
            mLock.unlock();
        }

        return success;
    }

    public final boolean tell(@NonNull final M message,
                              final int delay,
                              @NonNull final TimeUnit unit) {
        final boolean success;

        if (delay > 0) {
            mLock.lock();
            try {
                if (mStopped) {
                    throw new UnsupportedOperationException(mActorStopped);
                }

                success = (mHandler == null) ?
                        mPendingMessages.offer(new PendingMessage.User<>(message, delay, unit)) :
                        send(mHandler, message, unit.toMillis(delay));
            } finally {
                mLock.unlock();
            }
        } else {
            success = tell(message);
        }

        return success;
    }

    public final boolean stop() {
        final boolean success;

        mLock.lock();
        try {
            if (!mStopped && (mHandler == null) && (mTask != null)) {
                PendingMessage<M> message = mPendingMessages.poll();
                while (message != null) {
                    message.send(mTask);
                    message = mPendingMessages.poll();
                }
            }
            success = mStopped || send(STOP);
            mStopped = true;
        } finally {
            mLock.unlock();
        }

        return success;
    }

    @Override
    public final boolean attach(@NonNull final Looper looper) {
        final boolean success;

        mLock.lock();
        try {
            success = (mTask != null) && ((mHandler == null) || !hasUndeliveredMessages(mHandler));
            if (success) {
                mHandler = new Handler(looper, mTask);
                PendingMessage<M> message = mPendingMessages.poll();
                while (message != null) {
                    message.send(mHandler, mTask);
                    message = mPendingMessages.poll();
                }
            }
        } finally {
            mLock.unlock();
        }

        return success;
    }

    @Override
    public final boolean detach() {
        final boolean success;

        mLock.lock();
        try {
            if (mStopped && (mHandler != null)) {
                mHandler.removeMessages(USER_MESSAGE);
                mHandler.removeMessages(SYSTEM_MESSAGE);
            }
            success = !hasUndeliveredMessages(mHandler);
            if (success) {
                mHandler = null;
            }
        } finally {
            mLock.unlock();
        }

        return success;
    }

    @Override
    public final int hashCode() {
        return mName.hashCode();
    }

    @Override
    public final boolean equals(@Nullable final Object other) {
        boolean result = this == other;

        if (!result && (other instanceof Reference<?>)) {
            result = mName.equals(((Reference<?>) other).mName);
        }

        return result;
    }

    @NonNls
    @NonNull
    @Override
    public final String toString() {
        return "Actor(" + mName + ')';
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public final <N extends M> Reference<N> cast() {
        return (Reference<N>) this;
    }

    protected final boolean send(final int message) {
        final boolean success;

        mLock.lock();
        try {
            if (mStopped) {
                throw new UnsupportedOperationException(mActorStopped);
            }

            if (mHandler == null) {
                success = (mPendingMessages.isEmpty() && (mTask != null)) ?
                        (mTask.handleSystemMessage(message) || mPendingMessages.offer(new PendingMessage.System<M>(message))) :
                        mPendingMessages.offer(new PendingMessage.System<M>(message));
            } else {
                success = (mTask == null) ?
                        send(mHandler, message) :
                        (mTask.send(mHandler, message) || send(mHandler, message));
            }
        } finally {
            mLock.unlock();
        }

        return success;
    }

    protected final boolean start(@NonNull final Executor executor) {
        boolean success = false;

        mLock.lock();
        try {
            if (mTask == null) {
                mTask = new Task<>(this, mActor);
                mActor.postStart(mSystem, this);
            }

            success = mTask.start(executor);
        } catch (final Throwable error) {
            Log.e(TAG, this + " failed to start ", error); // NON-NLS
        } finally {
            mLock.unlock();
        }

        return success;
    }

    protected final boolean pause() {
        final boolean success;

        mLock.lock();
        try {
            if (mStopped) {
                throw new UnsupportedOperationException(mActorStopped);
            }

            success = (mHandler == null) || send(PAUSE);
            if (success) {
                mHandler = null;
            }
        } finally {
            mLock.unlock();
        }

        return success;
    }

    protected final boolean stop(final boolean immediately) {
        boolean success = false;

        if (immediately) {
            mSystem.onStop(this);

            mLock.lock();
            try {
                mStopped = true;

                if (mTask == null) {
                    success = true;
                } else {
                    mActor.preStop();

                    if (mHandler != null) {
                        mHandler.removeMessages(USER_MESSAGE);
                        mHandler.removeMessages(SYSTEM_MESSAGE);
                    }
                    mPendingMessages.clear();
                    mHandler = null;

                    success = mTask.stop();
                }
            } catch (final Throwable error) {
                Log.e(TAG, this + " failed to stop ", error); // NON-NLS
            } finally {
                mLock.unlock();
            }
        } else {
            success = stop();
        }

        return success;
    }

    private static boolean hasUndeliveredMessages(@Nullable final Handler handler) {
        return (handler != null) && (handler.hasMessages(SYSTEM_MESSAGE) || handler.hasMessages(USER_MESSAGE));
    }

    private static boolean send(@NonNull final Handler handler, final int message) {
        return handler.sendMessage(handler.obtainMessage(SYSTEM_MESSAGE, message, 0));
    }

    private static <M> boolean send(@NonNull final Handler handler,
                                    @NonNull final M message,
                                    final long delay) {
        final Message msg = handler.obtainMessage(USER_MESSAGE, message);
        return (delay > 0) ? handler.sendMessageDelayed(msg, delay) : handler.sendMessage(msg);
    }

    private interface PendingMessage<M> {

        boolean send(@NonNull final Task<M> task);

        boolean send(@NonNull final Handler handler, @Nullable final Task<M> task);

        class System<M> implements PendingMessage<M> {

            private final int mMessage;

            public System(final int message) {
                super();

                mMessage = message;
            }

            @Override
            public final boolean send(@NonNull final Task<M> task) {
                return task.handleSystemMessage(mMessage);
            }

            @Override
            public final boolean send(@NonNull final Handler handler, @Nullable final Task<M> task) {
                return (task == null) ?
                        Reference.send(handler, mMessage) :
                        (task.send(handler, mMessage) || Reference.send(handler, mMessage));
            }
        }

        class User<M> implements PendingMessage<M> {

            @NonNull
            private final M mMessage;
            private final long mAtTime;

            public User(@NonNull final M message, final long delay, @NonNull final TimeUnit unit) {
                super();

                mMessage = message;
                mAtTime = uptimeMillis() + unit.toMillis(delay);
            }

            public User(@NonNull final M message) {
                super();

                mMessage = message;
                mAtTime = 0;
            }

            @Override
            public final boolean send(@NonNull final Task<M> task) {
                return (mAtTime <= uptimeMillis()) && task.handleUserMessage(mMessage);
            }

            @Override
            public final boolean send(@NonNull final Handler handler, @Nullable final Task<M> task) {
                final long now = uptimeMillis();
                final boolean success;

                if (now < mAtTime) {
                    success = Reference.send(handler, mMessage, mAtTime - now);
                } else {
                    success = (task == null) ?
                            Reference.send(handler, mMessage, NO_DELAY) :
                            (task.send(handler, mMessage) || Reference.send(handler, mMessage, NO_DELAY));
                }

                return success;
            }
        }
    }

    private static class Task<M> implements Handler.Callback {

        @NonNull
        private final Reference<M> mReference;
        @NonNull
        private final Actor<M> mActor;

        private final Semaphore mDirectCall = new Semaphore(1);

        @Nullable
        private Executor.Submission mSubmission;

        private Task(@NonNull final Reference<M> reference, @NonNull final Actor<M> actor) {
            super();

            mReference = reference;
            mActor = actor;
        }

        public final boolean start(@NonNull final Executor executor) {
            final boolean stopped = stop();
            if (stopped) {
                mSubmission = executor.submit(mReference);
            }
            return stopped && (mSubmission != null);
        }

        public final boolean stop() {
            final boolean success = (mSubmission == null) || mSubmission.stop();
            if (success) {
                mSubmission = null;
            }
            return success;
        }

        @Override
        @SuppressWarnings("unchecked")
        public final boolean handleMessage(@NonNull final Message message) {
            final boolean processed;

            switch (message.what) {
                case SYSTEM_MESSAGE:
                    processed = handleSystemMessage(message.arg1);
                    break;
                case USER_MESSAGE:
                    processed = handleUserMessage((M) message.obj);
                    break;
                default:
                    processed = false;
            }

            if (processed) {
                message.recycle();
            }

            return processed;
        }

        public final boolean handleUserMessage(@NonNls @NonNull final M message) {
            final boolean success = mDirectCall.tryAcquire();

            if (success) {
                try {
                    mActor.onMessage(message);
                } catch (final Throwable error) {
                    Log.e(TAG, mReference + " failed to receive message " + message, error); // NON-NLS
                } finally {
                    mDirectCall.release();
                }
            }

            return success;
        }

        public final boolean handleSystemMessage(final int message) {
            final boolean processed;

            switch (message) {
                case PAUSE:
                    processed = (mSubmission == null) || mSubmission.stop();
                    mSubmission = null;
                    break;
                case STOP:
                    processed = mReference.stop(true);
                    break;
                default:
                    processed = false;
            }

            return processed;
        }

        public final boolean send(@NonNull final Handler handler, final int message) {
            return !hasUndeliveredMessages(handler) && isCurrentThread(handler) && handleSystemMessage(message);
        }

        public final boolean send(@NonNull final Handler handler, @NonNull final M message) {
            return !hasUndeliveredMessages(handler) && isCurrentThread(handler) && handleUserMessage(message);
        }

        private static boolean isCurrentThread(@NonNull final Handler handler) {
            return handler.getLooper().getThread().equals(currentThread());
        }
    }
}
