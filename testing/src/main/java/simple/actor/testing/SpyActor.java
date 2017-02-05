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

import simple.actor.Actor;
import simple.actor.Channel;
import simple.actor.Context;

/**
 * An {@link Actor} that remembers all received messages. This {@code Actor} should only be used for
 * testing.
 *
 * @param <M> The type of received messages.
 */
public final class SpyActor<M> extends Actor<M> {

    private final Object mLock = new Object();

    @GuardedBy("mLock")
    private final Queue<M> mReceived = new ArrayDeque<>();

    @GuardedBy("mLock")
    private boolean mStarted = false;

    @GuardedBy("mLock")
    private boolean mStopped = false;

    /**
     * Remembers the received message.
     *
     * @throws IllegalStateException if message has been received before actor has been started.
     */
    @Override
    protected void onMessage(final M message) {
        synchronized (mLock) {
            if (!mStarted) {
                throw new IllegalStateException("message received before actor has been started");
            }
            if (mStopped) {
                throw new IllegalStateException("message received after actor has been stopped");
            }
            mReceived.add(message);
        }
    }

    /**
     * Remembers that actor has been started.
     *
     * @throws IllegalStateException if actor is started more than once.
     */
    @Override
    protected void onStart(final Channel<M> self, final Context context) {
        synchronized (mLock) {
            if (mStarted) {
                throw new IllegalStateException("actor started multiple times");
            }
            mStarted = true;
        }
    }

    /**
     * Remembers that actor has been started.
     *
     * @throws IllegalStateException if actor is started more than once.
     */
    @Override
    protected void onStop() {
        synchronized (mLock) {
            if (!mStarted) {
                throw new IllegalStateException("actor stopped before it has been started");
            }
            if (mStopped) {
                throw new IllegalStateException("actor stopped multiple times");
            }
            mStopped = true;
        }
    }

    /** Returns all received messages. */
    public List<M> getReceivedMessages() {
        synchronized (mLock) {
            return new ArrayList<>(mReceived);
        }
    }

    /** Returns {@code true} if actor has been started. */
    public boolean isStarted() {
        synchronized (mLock) {
            return mStarted;
        }
    }

    /** Returns {@code true} if actor has been stopped. */
    public boolean isStopped() {
        synchronized (mLock) {
            return mStopped;
        }
    }
}
