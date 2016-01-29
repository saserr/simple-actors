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

import android.actor.channel.Mailbox;
import android.actor.channel.Send;
import android.actor.executor.Executable;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.NotThreadSafe;
import net.jcip.annotations.ThreadSafe;

import org.jetbrains.annotations.NonNls;

import java.lang.annotation.Retention;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static android.util.Log.DEBUG;
import static android.util.Log.INFO;
import static java.lang.annotation.RetentionPolicy.SOURCE;

@ThreadSafe
public class Reference<M> implements Executable {

    // TODO make it immutable class by using Mailbox + Channel to hold the state

    private static final String TAG = Reference.class.getSimpleName();

    @NonNull
    private final Context mContext;
    @NonNull
    private final Actor.Name mName;
    @NonNull
    private final Actor<M> mActor;
    @NonNull
    private final Callback mCallback;
    @NonNull
    private final Task<M> mTask;
    @NonNull
    @GuardedBy("mLock")
    private final Mailbox<Message<M>> mMailbox;

    private final Lock mLock = new ReentrantLock();

    @State
    @GuardedBy("mLock")
    private int mState = State.INITIALIZED;

    public Reference(@NonNull final Context context,
                     @NonNull final Actor<M> actor,
                     @NonNull final Callback callback) {
        super();

        mContext = context;
        mName = context.getName();
        mActor = actor;
        mCallback = callback;

        mTask = new Task<>(this, actor);
        mMailbox = new Mailbox<>(mTask);
    }

    @NonNull
    public final Actor.Name getName() {
        return mName;
    }

    public final boolean isStopped() {
        final boolean result;

        mLock.lock();
        try {
            result = mState == State.STOPPED;
        } finally {
            mLock.unlock();
        }

        return result;
    }

    public final boolean tell(@NonNull final M message) {
        final boolean success;

        mLock.lock();
        try {
            if ((mState != State.INITIALIZED) && (mState != State.STARTED)) {
                throw new UnsupportedOperationException(this + " is stopped!");
            }

            success = Send.withRetries(mMailbox, new Message.User<>(message));
        } finally {
            mLock.unlock();
        }

        return success;
    }

    public final boolean stop() {
        final boolean success;

        mLock.lock();
        try {
            switch (mState) {
                case State.INITIALIZED:
                    success = stop(true);
                    break;
                case State.STARTED:
                    // TODO don't use isAttached(); Mailbox should check that somehow
                    success = mMailbox.isAttached() ?
                            Send.withRetries(mMailbox, Message.Control.<M>stop()) :
                            (mMailbox.stop(false) && stop(true));

                    if (success && (mState == State.STARTED)) {
                        mState = State.STOPPING;
                    }
                    break;
                case State.STOPPING:
                case State.STOPPED:
                    success = true;
                    break;
                default:
                    throw new IllegalStateException("Unknown reference state: " + mState);
            }
        } finally {
            mLock.unlock();
        }

        return success;
    }

    @Override
    public final boolean attach(@NonNull final Channel.Factory factory) {
        final boolean success;

        mLock.lock();
        try {
            if (mState == State.INITIALIZED) {
                throw new UnsupportedOperationException(this + " hasn't been started!");
            }
            if (mState != State.STARTED) {
                throw new UnsupportedOperationException(this + " is stopped!");
            }

            success = mMailbox.attach(factory);
        } finally {
            mLock.unlock();
        }

        if (success && Log.isLoggable(TAG, DEBUG)) {
            Log.d(TAG, this + " attached to new channel"); //NON-NLS
        }

        return success;
    }

    @Override
    public final boolean detach() {
        final boolean success;

        mLock.lock();
        try {
            switch (mState) {
                case State.INITIALIZED:
                case State.STOPPED:
                    success = true;
                    break;
                case State.STARTED:
                    mMailbox.detach();
                    success = true;
                    break;
                case State.STOPPING:
                    if (Log.isLoggable(TAG, DEBUG)) {
                        Log.d(TAG, this + " stopping the mailbox"); //NON-NLS
                    }
                    success = mMailbox.stop(true);
                    break;
                default:
                    throw new IllegalStateException("Unknown reference state: " + mState);
            }
        } finally {
            mLock.unlock();
        }

        if (success && Log.isLoggable(TAG, DEBUG)) {
            Log.d(TAG, this + " detached from the channel"); //NON-NLS
        }

        return success;
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

    protected final boolean start(@NonNull final Executor executor) {
        boolean success = false;

        mLock.lock();
        try {
            if (mState == State.STARTED) {
                success = true;
            } else {
                if (mState != State.INITIALIZED) {
                    throw new UnsupportedOperationException(this + " is stopped!");
                }

                try {
                    mActor.postStart(mContext, this);
                    mState = State.STARTED;
                    success = true;
                } catch (final Throwable error) {
                    Log.e(TAG, this + " failed to start! Stopping", error); // NON-NLS
                }
            }

            if (success) {
                success = mTask.start(executor);
            } else {
                stop(true);
            }
        } finally {
            mLock.unlock();
        }

        if (success && Log.isLoggable(TAG, INFO)) {
            Log.i(TAG, this + " started"); //NON-NLS
        }

        return success;
    }

    protected final boolean pause() {
        final boolean success;

        mLock.lock();
        try {
            if (mState == State.INITIALIZED) {
                success = true;
            } else {
                if (mState != State.STARTED) {
                    throw new UnsupportedOperationException(this + " is stopped!");
                }

                success = Send.withRetries(mMailbox, Message.Control.<M>pause());
            }
        } finally {
            mLock.unlock();
        }

        if (success && Log.isLoggable(TAG, INFO)) {
            Log.i(TAG, this + " paused"); //NON-NLS
        }

        return success;
    }

    protected final boolean stop(final boolean immediately) {
        boolean success = false;

        if (immediately) {
            mLock.lock();
            try {
                if (mState == State.INITIALIZED) {
                    success = true;
                } else {
                    success = mTask.stop(true);
                    if (success) {
                        mActor.preStop();
                    }
                }

                if (success) {
                    mState = State.STOPPED;
                }
            } catch (final Throwable error) {
                Log.w(TAG, this + " failed during stop! Ignoring", error); // NON-NLS
                mState = State.STOPPED;
            } finally {
                mLock.unlock();
                if (success) {
                    mCallback.onStop(mName);
                }
            }
        } else {
            success = stop();
        }

        if (success && Log.isLoggable(TAG, INFO)) {
            Log.i(TAG, this + " stopped"); //NON-NLS
        }

        return success;
    }

    public interface Callback {
        void onStop(@NonNull final Actor.Name name);
    }

    @Retention(SOURCE)
    @IntDef({State.INITIALIZED, State.STARTED, State.STOPPING, State.STOPPED})
    private @interface State {
        int INITIALIZED = 1;
        int STARTED = 2;
        int STOPPING = 3;
        int STOPPED = 4;
    }

    @VisibleForTesting
    abstract static class Message<M> implements android.actor.channel.Message {

        @Channel.Delivery
        public abstract int deliverTo(@NonNull final Callback<M> callback);

        public interface Callback<M> {

            @Channel.Delivery
            int receive(final int message);

            @Channel.Delivery
            int receive(@NonNull final M message);
        }

        public static final class Control<M> extends Message<M> {

            private static final Control<Object> Pause = new Control<>(Type.PAUSE);
            private static final Control<Object> Stop = new Control<>(Type.STOP);

            @Type
            private final int mType;

            public Control(@Type final int type) {
                super();

                mType = type;
            }

            @Override
            public boolean isControl() {
                return true;
            }

            @Channel.Delivery
            @Override
            public int deliverTo(@NonNull final Callback<M> callback) {
                return callback.receive(mType);
            }

            @Override
            public int hashCode() {
                return mType;
            }

            @Override
            public boolean equals(@Nullable final Object object) {
                boolean result = this == object;

                if (!result && (object instanceof Control)) {
                    final Control<?> other = (Control<?>) object;
                    result = mType == other.mType;
                }

                return result;
            }

            @NonNls
            @NonNull
            @Override
            public String toString() {
                return "Control(type=" + toString(mType) + ')';
            }

            @SuppressWarnings("unchecked")
            public static <M> Control<M> pause() {
                return (Control<M>) Pause;
            }

            @SuppressWarnings("unchecked")
            public static <M> Control<M> stop() {
                return (Control<M>) Stop;
            }

            @NonNls
            @NonNull
            private static String toString(@Type final int type) {
                @NonNls final String result;

                switch (type) {
                    case Type.PAUSE:
                        result = "pause";
                        break;
                    case Type.STOP:
                        result = "stop";
                        break;
                    default:
                        result = "unknown(" + type + ')';
                }

                return result;
            }

            @Retention(SOURCE)
            @IntDef({Type.PAUSE, Type.STOP})
            @VisibleForTesting
            @interface Type {
                int PAUSE = 1;
                int STOP = 2;
            }
        }

        public static final class User<M> extends Message<M> {

            @NonNull
            private final M mMessage;

            public User(@NonNull final M message) {
                super();

                mMessage = message;
            }

            @Override
            public boolean isControl() {
                return false;
            }

            @Override
            public int deliverTo(@NonNull final Callback<M> callback) {
                return callback.receive(mMessage);
            }

            @Override
            public int hashCode() {
                return mMessage.hashCode();
            }

            @Override
            public boolean equals(@Nullable final Object object) {
                boolean result = this == object;

                if (!result && (object instanceof User<?>)) {
                    final User<?> other = (User<?>) object;
                    result = mMessage == other.mMessage;
                }

                return result;
            }

            @NonNls
            @NonNull
            @Override
            public String toString() {
                return "User(message=" + mMessage + ')';
            }
        }
    }

    @NotThreadSafe
    private static final class Task<M> implements Channel<Message<M>>, Message.Callback<M> {

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

        public boolean start(@NonNull final Executor executor) {
            final boolean stopped = stop(true);
            if (stopped) {
                mSubmission = executor.submit(mReference);
            }
            return stopped && (mSubmission != null);
        }

        @Override
        public boolean stop(final boolean immediately) {
            final boolean success = (mSubmission == null) || mSubmission.stop();
            if (success) {
                mSubmission = null;
            }
            return success;
        }

        @Channel.Delivery
        @Override
        public int send(@NonNull final Message<M> message) {
            return message.deliverTo(this);
        }

        @Channel.Delivery
        @Override
        public int receive(@NonNls final int message) {
            final int result;

            switch (message) {
                case Message.Control.Type.PAUSE:
                    result = ((mSubmission == null) || mSubmission.stop()) ?
                            Channel.Delivery.SUCCESS :
                            Channel.Delivery.FAILURE_CAN_RETRY;
                    mSubmission = null;
                    break;
                case Message.Control.Type.STOP:
                    result = mReference.stop(true) ?
                            Channel.Delivery.SUCCESS :
                            Channel.Delivery.FAILURE_CAN_RETRY;
                    break;
                default:
                    result = Delivery.FAILURE_NO_RETRY;
            }

            if ((result == Channel.Delivery.SUCCESS) && Log.isLoggable(TAG, DEBUG)) {
                Log.d(TAG, mReference + " handled system message " + message); //NON-NLS
            }

            return result;
        }

        @Channel.Delivery
        @Override
        public int receive(@NonNls @NonNull final M message) {
            int result = Channel.Delivery.SUCCESS;

            if (mDirectCall.tryAcquire()) {
                try {
                    mActor.onMessage(message);
                } catch (final Throwable error) {
                    Log.e(TAG, mReference + " failed to receive message " + message + "! Stopping", error); // NON-NLS
                    if (!mReference.stop(true)) {
                        Log.e(TAG, mReference + " couldn't be stopped after the failure! Ignoring the failure"); // NON-NLS
                    }
                    result = Channel.Delivery.FAILURE_NO_RETRY;
                } finally {
                    mDirectCall.release();
                }
            } else {
                result = Channel.Delivery.FAILURE_CAN_RETRY;
            }

            if ((result == Channel.Delivery.SUCCESS) && Log.isLoggable(TAG, DEBUG)) {
                Log.d(TAG, mReference + " handled user message " + message); //NON-NLS
            }

            return result;
        }
    }
}
