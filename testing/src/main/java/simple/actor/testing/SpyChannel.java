/*
 * Copyright 2017 Sanjin Sehic
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

package simple.actor.testing;

import net.jcip.annotations.GuardedBy;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import simple.actor.Channel;

/**
 * A {@link Channel} that remembers all sent messages. This {@code Channel} should only be used for
 * testing.
 *
 * @param <M> The type of sent messages.
 */
public final class SpyChannel<M> implements Channel<M> {

    private final Object mLock = new Object();

    @GuardedBy("mLock")
    private final Queue<M> mSent = new ArrayDeque<>();

    @GuardedBy("mLock")
    private boolean mStopped = false;

    /**
     * Remembers the sent message.
     *
     * <p>Note that if {@link Channel} has been stopped, the send request will be ignored and
     * the {@code false} value will be returned.
     *
     * @return {@code true} if {@code Channel} has not been stopped; otherwise {@code false}.
     */
    @Override
    public boolean send(final M message) {
        synchronized (mLock) {
            if (mStopped) {
                return false;
            }

            mSent.add(message);
            return true;
        }
    }

    /**
     * Remembers that {@link Channel} has been stopped. After stop, no more messages can be sent on
     * the {@code Channel}.
     *
     * <p>Stopping {@code Channel} multiple times has same meaning as stopping it once; in other
     * words, all stops after the first one will be ignored.
     */
    @Override
    public void stop() {
        synchronized (mLock) {
            mStopped = true;
        }
    }

    /** Returns all sent messages. */
    public List<M> getSentMessages() {
        synchronized (mLock) {
            return new ArrayList<>(mSent);
        }
    }

    /** Returns {@code true} if {@link Channel} has been stopped. */
    public boolean isStopped() {
        synchronized (mLock) {
            return mStopped;
        }
    }
}
