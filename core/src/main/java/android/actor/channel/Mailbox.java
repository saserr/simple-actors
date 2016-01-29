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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.jcip.annotations.NotThreadSafe;

import java.util.LinkedList;
import java.util.Queue;

@NotThreadSafe
public class Mailbox<M extends Message> implements Channel<M> {

    @NonNull
    private final Channel<M> mDestination;

    private final Queue<M> mMessages = new LinkedList<>();

    @Nullable
    private Channel<M> mCarrier;

    public Mailbox(@NonNull final Channel<M> destination) {
        super();

        mDestination = destination;
    }

    @Override
    @Channel.Delivery
    public final int send(@NonNull final M message) {
        return (mCarrier == null) ?
                ((message.isControl() && mMessages.isEmpty()) ?
                        mDestination.send(message) :
                        sendLater(message)) :
                mCarrier.send(message);
    }

    public final boolean isAttached() {
        return mCarrier != null;
    }

    public final boolean attach(@NonNull final Channel.Factory factory) {
        boolean success = (mCarrier == null) || mCarrier.stop(false);

        if (success) {
            mCarrier = factory.create(mDestination);
            while (success && !mMessages.isEmpty()) {
                if (!Send.withRetries(mCarrier, mMessages.poll())) {
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

    @Override
    public final boolean stop(final boolean immediately) {
        final boolean success = (mCarrier == null) || mCarrier.stop(immediately);

        if (success) {
            mCarrier = null;
            if (immediately) {
                mMessages.clear();
            } else {
                while (!mMessages.isEmpty()) {
                    Send.withRetries(mDestination, mMessages.poll(), 1);
                }
            }
        }

        return success;
    }

    @Delivery
    private int sendLater(@NonNull final M message) {
        return mMessages.offer(message) ?
                Channel.Delivery.SUCCESS :
                Channel.Delivery.FAILURE_CAN_RETRY;
    }
}
