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

package android.actor.channel;

import android.actor.Channel;
import android.actor.Configuration;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import net.jcip.annotations.NotThreadSafe;
import net.jcip.annotations.ThreadSafe;

import org.jetbrains.annotations.NonNls;

import java.lang.annotation.Retention;

import static android.util.Log.DEBUG;
import static java.lang.Thread.currentThread;
import static java.lang.annotation.RetentionPolicy.SOURCE;

@ThreadSafe
public class LooperChannel<M> implements Channel<M> {

    private static final String TAG = LooperChannel.class.getSimpleName();

    private final Object mToken = new Object();

    @NonNull
    private final Looper mLooper;
    @NonNull
    private final Channel<M> mDestination;
    @NonNull
    private final Handler mHandler;

    public LooperChannel(@NonNull final Looper looper,
                         @NonNull final Channel<M> destination) {
        super();

        mLooper = looper;
        mDestination = destination;
        mHandler = new Handler(looper);
    }

    public final boolean hasUndeliveredMessages() {
        return mHandler.hasMessages(Message.Type.CONTROL, mToken) ||
                mHandler.hasMessages(Message.Type.USER, mToken);
    }

    public final boolean isOnCurrentThread() {
        return mLooper.getThread() == currentThread();
    }

    @Channel.Delivery
    @Override
    public final int send(final int message) {
        @Channel.Delivery int result = (isOnCurrentThread() && !hasUndeliveredMessages()) ?
                mDestination.send(message) :
                Delivery.FAILURE_CAN_RETRY;

        if (result == Delivery.FAILURE_CAN_RETRY) {
            result = control(message).send() ?
                    Delivery.SUCCESS :
                    Delivery.FAILURE_NO_RETRY;
        }

        return result;
    }

    @Channel.Delivery
    @Override
    public final int send(@NonNull final M message) {
        @Channel.Delivery int result = (isOnCurrentThread() && !hasUndeliveredMessages()) ?
                mDestination.send(message) :
                Delivery.FAILURE_CAN_RETRY;

        if (result == Delivery.FAILURE_CAN_RETRY) {
            result = user(message).send() ?
                    Delivery.SUCCESS :
                    Delivery.FAILURE_NO_RETRY;
        }

        return result;
    }

    @Override
    public final boolean stop(final boolean immediately) {
        if (immediately) {
            mHandler.removeMessages(Message.Type.CONTROL, mToken);
            mHandler.removeMessages(Message.Type.USER, mToken);
        }

        return !hasUndeliveredMessages();
    }

    @NonNull
    private Message control(final int message) {
        return new Message.Control(mHandler, mToken, mDestination, message);
    }

    @NonNull
    private Message user(final M message) {
        return new Message.User<>(mHandler, mToken, mDestination, message);
    }

    @ThreadSafe
    @VisibleForTesting
    abstract static class Message implements Runnable {

        @NonNull
        private final Handler mHandler;
        @NonNull
        private final Object mToken;
        @Type
        private final int mType;

        protected Message(@NonNull final Handler handler,
                          @NonNull final Object token,
                          @Type final int type) {
            super();

            mHandler = handler;
            mToken = token;
            mType = type;
        }

        @Channel.Delivery
        protected abstract int deliver();

        @Override
        public final void run() {
            int delivery;

            int triesLeft = Configuration.MaximumNumberOfRetries.get();
            do {
                delivery = deliver();
                triesLeft--;
            } while ((delivery == Channel.Delivery.FAILURE_CAN_RETRY) && (triesLeft > 0));

            if (delivery == Channel.Delivery.SUCCESS) {
                if (Log.isLoggable(TAG, DEBUG)) {
                    Log.d(TAG, "Successfully delivered " + this); //NON-NLS
                }
            } else {
                Log.e(TAG, "Failed to deliver " + this + "! Cannot retry"); //NON-NLS
            }
        }

        public final boolean send() {
            final android.os.Message message = android.os.Message.obtain(mHandler, this);
            message.what = mType;
            message.obj = mToken;
            final boolean success = mHandler.sendMessage(message);

            if (success) {
                if (Log.isLoggable(TAG, DEBUG)) {
                    Log.d(TAG, this + "dispatched"); //NON-NLS
                }
            } else {
                Log.e(TAG, "Failed to dispatch " + this + '!'); //NON-NLS
            }

            return success;
        }

        @NonNls
        @NonNull
        @Override
        public abstract String toString();

        @Retention(SOURCE)
        @IntDef({Type.CONTROL, Type.USER})
        @VisibleForTesting
        @interface Type {
            int CONTROL = 1;
            int USER = 2;
        }

        @ThreadSafe
        public static final class Control extends Message {

            @NonNull
            private final Channel<?> mDestination;
            private final int mMessage;

            public <M> Control(@NonNull final Handler handler,
                               @NonNull final Object token,
                               @NonNull final Channel<M> destination,
                               final int message) {
                super(handler, token, Type.CONTROL);

                mDestination = destination;
                mMessage = message;
            }

            @Override
            protected int deliver() {
                return mDestination.send(mMessage);
            }

            @NonNls
            @NonNull
            @Override
            public String toString() {
                return "Message(type=Control, data=" + mMessage + ')';
            }
        }

        @ThreadSafe
        public static final class User<M> extends Message {

            @NonNull
            private final Channel<M> mDestination;
            @NonNull
            private final M mMessage;

            public User(@NonNull final Handler handler,
                        @NonNull final Object token,
                        @NonNull final Channel<M> destination,
                        @NonNull final M message) {
                super(handler, token, Type.USER);

                mDestination = destination;
                mMessage = message;
            }

            @Override
            protected int deliver() {
                return mDestination.send(mMessage);
            }

            @NonNls
            @NonNull
            @Override
            public String toString() {
                return "Message(type=User, data=" + mMessage + ')';
            }
        }
    }
}
