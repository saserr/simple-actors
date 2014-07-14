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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.LinkedList;
import java.util.Queue;

import static android.os.SystemClock.uptimeMillis;

public class BufferedMessenger<M> implements Messenger<M> {

    @NonNull
    private final Messenger.Callback<M> mCallback;
    private final Queue<PendingMessage<M>> mPendingMessages = new LinkedList<>();

    @Nullable
    private Messenger<M> mMessenger;

    public BufferedMessenger(@NonNull final Messenger.Callback<M> callback) {
        super();

        mCallback = callback;
    }

    public final boolean isAttached() {
        return mMessenger != null;
    }

    public final boolean attach(@NonNull final Factory factory) {
        final boolean success = (mMessenger == null) || mMessenger.stop(false);

        if (success) {
            mMessenger = factory.create(mCallback);
            PendingMessage<M> message = mPendingMessages.poll();
            while (message != null) {
                message.send(mMessenger);
                message = mPendingMessages.poll();
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
                ((mPendingMessages.isEmpty() && mCallback.onMessage(message)) ||
                        mPendingMessages.offer(new PendingMessage.System<M>(message))) :
                mMessenger.send(message);
    }

    @Override
    public final boolean send(@NonNull final M message, final long delay) {
        return (mMessenger == null) ?
                mPendingMessages.offer(new PendingMessage.User<>(message, delay)) :
                mMessenger.send(message, delay);
    }

    @Override
    public final boolean stop(final boolean immediately) {
        final boolean success = (mMessenger == null) || mMessenger.stop(immediately);

        if (success) {
            if (immediately) {
                mPendingMessages.clear();
            } else {
                PendingMessage<M> message = mPendingMessages.poll();
                while (message != null) {
                    message.send(mCallback);
                    message = mPendingMessages.poll();
                }
            }
        }

        return success;
    }

    private interface PendingMessage<M> {

        boolean send(@NonNull final Messenger.Callback<M> callback);

        boolean send(@NonNull final Messenger<M> messenger);

        class System<M> implements PendingMessage<M> {

            private final int mMessage;

            public System(final int message) {
                super();

                mMessage = message;
            }

            @Override
            public final boolean send(@NonNull final Messenger.Callback<M> callback) {
                return callback.onMessage(mMessage);
            }

            @Override
            public final boolean send(@NonNull final Messenger<M> messenger) {
                return messenger.send(mMessage);
            }
        }

        class User<M> implements PendingMessage<M> {

            @NonNull
            private final M mMessage;
            private final long mAtTime;

            public User(@NonNull final M message, final long delay) {
                super();

                mMessage = message;
                mAtTime = uptimeMillis() + delay;
            }

            public User(@NonNull final M message) {
                super();

                mMessage = message;
                mAtTime = 0;
            }

            @Override
            public final boolean send(@NonNull final Messenger.Callback<M> callback) {
                return (mAtTime <= uptimeMillis()) && callback.onMessage(mMessage);
            }

            @Override
            public final boolean send(@NonNull final Messenger<M> messenger) {
                final long now = uptimeMillis();
                return (now < mAtTime) ?
                        messenger.send(mMessage, mAtTime - now) :
                        messenger.send(mMessage, 0);
            }
        }
    }
}
