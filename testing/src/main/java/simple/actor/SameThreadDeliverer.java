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

package simple.actor;

import java.util.concurrent.Semaphore;

import simple.actor.testing.SpyContext;

/**
 * A {@link Channel} that {@link Actor#onMessage delivers} messages to an {@link
 * Actor} on the calling thread. This {@code Channel} should only be used for testing.
 *
 * @param <M> The type of sent messages.
 */
public class SameThreadDeliverer<M> implements Channel<M> {

    private final Semaphore mSemaphore = new Semaphore(1 /*permits*/);

    private final Actor<M> mActor;

    private boolean mStopped = false;

    /**
     * Creates a {@link Channel} that will {@link Actor#onMessage deliver} messages to given {@link
     * Actor} on the calling thread.
     *
     * @param actor the {@code Actor} that will receive all sent messages.
     */
    public SameThreadDeliverer(final Actor<M> actor) {
        this(actor, new SpyContext());
    }

    /**
     * Creates a {@link Channel} that will {@link Actor#onMessage deliver} messages to given {@link
     * Actor} on the calling thread.
     *
     * @param actor   the {@code Actor} that will receive all sent messages.
     * @param context the view of actor's group.
     */
    public SameThreadDeliverer(final Actor<M> actor, final Context context) {
        mActor = actor;
        mSemaphore.acquireUninterruptibly();
        try {
            mActor.onStart(this, context);
        } finally {
            mSemaphore.release();
        }
    }

    @Override
    public boolean send(final M message) {
        mSemaphore.acquireUninterruptibly();
        try {
            if (mStopped) {
                return false;
            }

            mActor.onMessage(message);
            return true;
        } finally {
            mSemaphore.release();
        }
    }

    @Override
    public void stop() {
        mSemaphore.acquireUninterruptibly();
        try {
            if (!mStopped) {
                mStopped = true;
                mActor.onStop();
            }
        } finally {
            mSemaphore.release();
        }
    }
}
