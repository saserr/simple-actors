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

package android.actor.channel;

import android.actor.Channel;
import android.actor.Configuration;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import net.jcip.annotations.NotThreadSafe;
import net.jcip.annotations.ThreadSafe;

import org.jetbrains.annotations.NonNls;

import java.util.LinkedList;
import java.util.Queue;

import static android.util.Log.DEBUG;

@NotThreadSafe
public class Mailbox<M> {

    private static final String TAG = Mailbox.class.getSimpleName();

    @NonNull
    private final Channel<M> mDestination;

    private final Queue<Message<M>> mMessages = new LinkedList<>();

    @Nullable
    private Channel<M> mCarrier;

    public Mailbox(@NonNull final Channel<M> destination) {
        super();

        mDestination = destination;
    }

    public final boolean isAttached() {
        return mCarrier != null;
    }

    public final boolean attach(@NonNull final Channel.Factory factory) {
        boolean success = (mCarrier == null) || mCarrier.stop(false);

        if (success) {
            mCarrier = factory.create(mDestination);
            while (success && !mMessages.isEmpty()) {
                if (!mMessages.poll().sendTo(mCarrier)) {
                    success = false;
                    mCarrier = null;
                }
            }
        }

        return success;
    }

    public final void detach() {
        mCarrier = null;
    }

    public boolean send(@NonNull final Message.Control<M> message) {
        return (mCarrier == null) ?
                (mMessages.isEmpty() ? message.sendTo(mDestination) : sendLater(message)) :
                message.sendTo(mCarrier);
    }

    public boolean send(@NonNull final Message.User<M> message) {
        return (mCarrier == null) ?
                sendLater(message) :
                message.sendTo(mCarrier);
    }

    public final boolean stop(final boolean immediately) {
        final boolean success = (mCarrier == null) || mCarrier.stop(immediately);

        if (success) {
            mCarrier = null;
            if (immediately) {
                mMessages.clear();
            } else {
                while (!mMessages.isEmpty()) {
                    final Message<M> message = mMessages.poll();
                    message.log(message.sendOnceTo(mDestination));
                }
            }
        }

        return success;
    }

    private boolean sendLater(@NonNull final Message<M> message) {
        return mMessages.offer(message);
    }

    @ThreadSafe
    public abstract static class Message<M> {

        @Channel.Delivery
        protected abstract int sendOnceTo(@NonNull final Channel<M> channel);

        public boolean sendTo(@NonNull final Channel<M> channel) {
            int delivery;

            int triesLeft = Configuration.MaximumNumberOfRetries.get();
            do {
                delivery = sendOnceTo(channel);
                triesLeft--;
                log(delivery, triesLeft);
            } while ((delivery == Channel.Delivery.FAILURE_CAN_RETRY) && (triesLeft > 0));

            return delivery == Channel.Delivery.SUCCESS;
        }

        @NonNls
        @NonNull
        @Override
        public abstract String toString();

        private void log(@Channel.Delivery final int delivery) {
            log(delivery, 0);
        }

        private void log(@Channel.Delivery final int delivery,
                         final int triesLeft) {
            if (delivery == Channel.Delivery.FAILURE_CAN_RETRY) {
                if (triesLeft > 0) {
                    Log.w(TAG, "Failed to deliver " + this + "! Retrying"); //NON-NLS
                } else {
                    Log.e(TAG, "Failed to deliver " + this + "! No retries left"); //NON-NLS
                }
            } else if (delivery == Channel.Delivery.SUCCESS) {
                if (Log.isLoggable(TAG, DEBUG)) {
                    Log.d(TAG, "Successfully delivered " + this); //NON-NLS
                }
            } else {
                Log.e(TAG, "Failed to deliver " + this + "! Cannot retry"); //NON-NLS
            }
        }

        @ThreadSafe
        public static final class Control<M> extends Message<M> {

            private final int mMessage;

            public Control(final int message) {
                super();

                mMessage = message;
            }

            @Channel.Delivery
            @Override
            protected int sendOnceTo(@NonNull final Channel<M> channel) {
                return channel.send(mMessage);
            }

            @Override
            public int hashCode() {
                return mMessage;
            }

            @Override
            public boolean equals(@Nullable final Object object) {
                boolean result = this == object;

                if (!result && (object instanceof Control)) {
                    final Control<?> other = (Control<?>) object;
                    result = mMessage == other.mMessage;
                }

                return result;
            }

            @NonNls
            @NonNull
            @Override
            public String toString() {
                return "Message(type=Control, data=" + mMessage + ')';
            }
        }

        @ThreadSafe
        public static final class User<M> extends Message<M> {

            @NonNull
            private final M mMessage;

            public User(@NonNull final M message) {
                super();

                mMessage = message;
            }

            @Channel.Delivery
            @Override
            protected int sendOnceTo(@NonNull final Channel<M> channel) {
                return channel.send(mMessage);
            }

            @Override
            public int hashCode() {
                return mMessage.hashCode();
            }

            @Override
            public boolean equals(@Nullable final Object object) {
                boolean result = this == object;

                if (!result && (object instanceof User)) {
                    final User<?> other = (User<?>) object;
                    result = mMessage == other.mMessage;
                }

                return result;
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
