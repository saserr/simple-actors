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
import simple.actor.SameThreadDeliverer;

/**
 * A {@link Context} that remembers all registered {@link Actor Actors}. This {@code Context} should
 * only be used for testing.
 */
public final class SpyContext implements Context {

    private final Object mLock = new Object();

    @GuardedBy("mLock")
    private final Queue<Actor<?>> mRegistered = new ArrayDeque<>();

    /**
     * Remembers the registered {@link Actor}.
     *
     * @return a {@link SameThreadDeliverer} that will {@link Actor#onMessage deliver} {@link
     * SameThreadDeliverer#send sent} messages on the calling thread.
     */
    @Override
    public <M> Channel<M> register(final Actor<M> actor) {
        synchronized (mLock) {
            mRegistered.add(actor);
        }
        return new SameThreadDeliverer<>(actor);
    }

    /** Returns all registered {@link Actor Actors}. */
    public List<Actor<?>> getRegisteredActors() {
        synchronized (mLock) {
            return new ArrayList<>(mRegistered);
        }
    }
}
