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

import java.util.LinkedList;
import java.util.Queue;

import static android.os.SystemClock.uptimeMillis;

public class Mailbox<M> {

    private final Queue<Message<M>> mMessages = new LinkedList<>();
    private final Object mLock = new Object();

    public Mailbox() {
        super();
    }

    public final boolean isEmpty() {
        final boolean result;

        synchronized (mLock) {
            result = mMessages.isEmpty();
        }

        return result;
    }

    public final void clear() {
        synchronized (mLock) {
            mMessages.clear();
        }
    }

    public final boolean put(final int message) {
        final boolean result;

        final Message<M> msg = new Message.System<>(message);
        synchronized (mLock) {
            result = mMessages.offer(msg);
        }

        return result;
    }

    public final boolean put(@NonNull final M message, final long delay) {
        final boolean result;

        final Message<M> msg = new Message.User<>(message, delay);
        synchronized (mLock) {
            result = mMessages.offer(msg);
        }

        return result;
    }

    @NonNull
    public final Message<M> take() {
        final Message<M> result;

        synchronized (mLock) {
            result = mMessages.poll();
        }

        return result;
    }

    public interface Message<M> {

        boolean send(@NonNull final Messenger.Callback<M> callback);

        boolean send(@NonNull final Messenger<M> messenger);

        class System<M> implements Message<M> {

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

        class User<M> implements Message<M> {

            @NonNull
            private final M mMessage;
            private final long mAtTime;

            public User(@NonNull final M message, final long delay) {
                super();

                mMessage = message;
                mAtTime = uptimeMillis() + delay;
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
