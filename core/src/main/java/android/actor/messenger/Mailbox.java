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

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static android.os.SystemClock.uptimeMillis;

@ThreadSafe
public class Mailbox<M> {

    @GuardedBy("mLock")
    private final Queue<Message<M>> mMessages = new LinkedList<>();
    private final Lock mLock = new ReentrantLock();

    public final boolean isEmpty() {
        final boolean result;

        mLock.lock();
        try {
            result = mMessages.isEmpty();
        } finally {
            mLock.unlock();
        }

        return result;
    }

    public final void clear() {
        mLock.lock();
        try {
            mMessages.clear();
        } finally {
            mLock.unlock();
        }
    }

    public final boolean put(final int message) {
        final boolean result;

        final Message<M> msg = new Message.Control<>(message);
        mLock.lock();
        try {
            result = mMessages.offer(msg);
        } finally {
            mLock.unlock();
        }

        return result;
    }

    public final boolean put(@NonNull final M message, final long delay) {
        final boolean result;

        final Message<M> msg = new Message.User<>(message, delay);
        mLock.lock();
        try {
            result = mMessages.offer(msg);
        } finally {
            mLock.unlock();
        }

        return result;
    }

    @NonNull
    public final Message<M> take() {
        final Message<M> result;

        mLock.lock();
        try {
            result = mMessages.poll();
        } finally {
            mLock.unlock();
        }

        return result;
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
            private final long mAtTime;

            public User(@NonNull final M message, final long delay) {
                super();

                mMessage = message;
                mAtTime = uptimeMillis() + delay;
            }

            @Override
            public boolean send(@NonNull final Messenger.Callback<M> callback) {
                return (mAtTime <= uptimeMillis()) && callback.onMessage(mMessage);
            }

            @Override
            public boolean send(@NonNull final Messenger<M> messenger) {
                final long now = uptimeMillis();
                return (now < mAtTime) ?
                        messenger.send(mMessage, mAtTime - now) :
                        messenger.send(mMessage, 0);
            }
        }
    }
}
