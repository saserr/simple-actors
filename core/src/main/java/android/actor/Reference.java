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

import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.jetbrains.annotations.NonNls;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.Thread.currentThread;

public class Reference<M> {

    protected static final int SYSTEM_MESSAGE = 1;
    protected static final int USER_MESSAGE = 2;

    protected static final int STOP = 1;

    @NonNls
    @NonNull
    private final String mName;
    @NonNls
    @NonNull
    private final String mActorStopped;

    private final Lock mLock = new ReentrantLock();
    private final Queue<PendingMessage<M>> mPendingMessages = new LinkedList<>();

    @Nullable
    private Handler mHandler;
    @Nullable
    private DirectCall<M> mDirectCall;
    private boolean mStopped = false;

    public Reference(@NonNls @NonNull final String name) {
        super();

        mName = name;
        mActorStopped = "Actor " + name + " is stopped";
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

            success = (mHandler == null) ?
                    mPendingMessages.offer(new User.PendingMessage<>(message)) :
                    User.send(mHandler, mDirectCall, message);
        } finally {
            mLock.unlock();
        }

        return success;
    }

    public final boolean stop() {
        final boolean success;

        mLock.lock();
        try {
            success = mStopped || tell(STOP);
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

    @NonNull
    @SuppressWarnings("unchecked")
    public final <N extends M> Reference<N> cast() {
        return (Reference<N>) this;
    }

    protected final void setDirectCall(@Nullable final DirectCall<M> call) {
        mLock.lock();
        try {
            mDirectCall = call;
        } finally {
            mLock.unlock();
        }
    }

    protected final boolean onAttach(@NonNull final Handler handler) {
        final boolean success;

        mLock.lock();
        try {
            success = !mStopped && (mHandler == null);
            if (success) {
                mHandler = handler;
                PendingMessage<M> message = mPendingMessages.poll();
                while (message != null) {
                    message.send(handler, mDirectCall);
                    message = mPendingMessages.poll();
                }
            }
        } finally {
            mLock.unlock();
        }

        return success;
    }

    protected final boolean onDetach() {
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

    protected final void onStop() {
        mLock.lock();
        try {
            mStopped = true;
            if (mHandler != null) {
                mHandler.removeMessages(USER_MESSAGE);
                mHandler.removeMessages(SYSTEM_MESSAGE);
            }
            mPendingMessages.clear();
            mHandler = null;
        } finally {
            mLock.unlock();
        }
    }

    private boolean tell(final int message) {
        final boolean success;

        mLock.lock();
        try {
            if (mStopped) {
                throw new UnsupportedOperationException(mActorStopped);
            }

            if (mHandler == null) {
                success = (mPendingMessages.isEmpty() && (mDirectCall != null)) ?
                        (mDirectCall.handleSystemMessage(message) || mPendingMessages.offer(new System.PendingMessage<M>(message))) :
                        mPendingMessages.offer(new System.PendingMessage<M>(message));
            } else {
                success = System.send(mHandler, mDirectCall, message);
            }
        } finally {
            mLock.unlock();
        }

        return success;
    }

    private static boolean hasUndeliveredMessages(@Nullable final Handler handler) {
        return (handler != null) && (handler.hasMessages(SYSTEM_MESSAGE) || handler.hasMessages(USER_MESSAGE));
    }

    private static boolean isCurrentThread(@NonNull final Handler handler) {
        return handler.getLooper().getThread().equals(currentThread());
    }

    public interface DirectCall<M> {

        boolean handleSystemMessage(final int message);

        boolean handleUserMessage(@NonNull final M message);
    }

    private interface PendingMessage<M> {
        boolean send(@NonNull final Handler handler, @Nullable final DirectCall<M> direct);
    }

    private static final class System {

        private static boolean send(@NonNull final Handler handler, final int message) {
            return handler.sendMessage(handler.obtainMessage(SYSTEM_MESSAGE, message, 0));
        }

        public static <M> boolean send(@NonNull final Handler handler,
                                       @Nullable final DirectCall<M> direct,
                                       final int message) {
            return (!hasUndeliveredMessages(handler) && (direct != null) && isCurrentThread(handler)) ?
                    (direct.handleSystemMessage(message) || send(handler, message)) :
                    send(handler, message);
        }

        public static class PendingMessage<M> implements Reference.PendingMessage<M> {

            private final int mMessage;

            public PendingMessage(final int message) {
                super();

                mMessage = message;
            }

            @Override
            public final boolean send(@NonNull final Handler handler,
                                      @Nullable final DirectCall<M> direct) {
                return System.send(handler, direct, mMessage);
            }
        }

        private System() {
            super();
        }
    }

    private static final class User {

        private static <M> boolean send(@NonNull final Handler handler, @NonNull final M message) {
            return handler.sendMessage(handler.obtainMessage(USER_MESSAGE, message));
        }

        public static <M> boolean send(@NonNull final Handler handler,
                                       @Nullable final DirectCall<M> direct,
                                       @NonNull final M message) {
            return (!hasUndeliveredMessages(handler) && (direct != null) && isCurrentThread(handler)) ?
                    (direct.handleUserMessage(message) || send(handler, message)) :
                    send(handler, message);
        }

        public static class PendingMessage<M> implements Reference.PendingMessage<M> {

            @NonNull
            private final M mMessage;

            public PendingMessage(@NonNull final M message) {
                super();

                mMessage = message;
            }

            @Override
            public final boolean send(@NonNull final Handler handler,
                                      @Nullable final DirectCall<M> direct) {
                return User.send(handler, direct, mMessage);
            }
        }

        private User() {
            super();
        }
    }
}
