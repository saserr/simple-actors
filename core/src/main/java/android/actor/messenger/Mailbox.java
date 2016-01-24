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

package android.actor.messenger;

import android.actor.Messenger;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.jcip.annotations.NotThreadSafe;
import net.jcip.annotations.ThreadSafe;

import java.util.LinkedList;
import java.util.Queue;

@NotThreadSafe
public class Mailbox<M> implements Messenger<M> {

    @NonNull
    private final Messenger.Callback<M> mCallback;

    private final Queue<Message<M>> mMessages = new LinkedList<>();

    @Nullable
    private Messenger<M> mMessenger;

    public Mailbox(@NonNull final Messenger.Callback<M> callback) {
        super();

        mCallback = callback;
    }

    public final boolean isAttached() {
        return mMessenger != null;
    }

    public final boolean attach(@NonNull final Factory factory) {
        boolean success = (mMessenger == null) || mMessenger.stop(false);

        if (success) {
            mMessenger = factory.create(mCallback);
            while (success && !mMessages.isEmpty()) {
                if (!mMessages.poll().send(mMessenger)) {
                    success = false;
                    mMessenger = null;
                }
            }
        }

        return success;
    }

    public final void detach() {
        mMessenger = null;
    }

    @Override
    public final boolean send(final int message) {
        return (mMessenger == null) ?
                (mMessages.isEmpty() ?
                        mCallback.onMessage(message) :
                        mMessages.offer(new Message.Control<M>(message))) :
                mMessenger.send(message);
    }

    @Override
    public final boolean send(@NonNull final M message) {
        return (mMessenger == null) ?
                mMessages.offer(new Message.User<>(message)) :
                mMessenger.send(message);
    }

    @Override
    public final boolean stop(final boolean immediately) {
        final boolean success = (mMessenger == null) || mMessenger.stop(immediately);

        if (success) {
            mMessenger = null;
            if (immediately) {
                mMessages.clear();
            } else {
                while (!mMessages.isEmpty()) {
                    mMessages.poll().send(mCallback);
                }
            }
        }

        return success;
    }

    public interface Message<M> {

        boolean send(@NonNull final Messenger.Callback<M> callback);

        boolean send(@NonNull final Messenger<M> messenger);

        @ThreadSafe
        final class Control<M> implements Message<M> {

            private final int mMessage;

            public Control(final int message) {
                super();

                mMessage = message;
            }

            @Override
            public boolean send(@NonNull final Messenger.Callback<M> callback) {
                return callback.onMessage(mMessage);
            }

            @Override
            public boolean send(@NonNull final Messenger<M> messenger) {
                return messenger.send(mMessage);
            }
        }

        @ThreadSafe
        final class User<M> implements Message<M> {

            @NonNull
            private final M mMessage;

            public User(@NonNull final M message) {
                super();

                mMessage = message;
            }

            @Override
            public boolean send(@NonNull final Messenger.Callback<M> callback) {
                return callback.onMessage(mMessage) == Messenger.Delivery.SUCCESS;
            }

            @Override
            public boolean send(@NonNull final Messenger<M> messenger) {
                return messenger.send(mMessage);
            }
        }
    }
}
