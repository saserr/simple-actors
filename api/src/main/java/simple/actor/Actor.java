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

/**
 * Base class that all actors have to extend to be able to receive messages. All received messages
 * have to be of the generic type {@code M}.
 *
 * @param <M> The type of received messages.
 */
public abstract class Actor<M> {

    /** Receive and process the given message. This method must be implemented by actors. */
    protected abstract void onMessage(M m);

    /**
     * Callback for when actor is started. Actor will be asynchronously started when registered to a
     * group.
     *
     * @param self    the {@link Channel} for actor to send messages to itself.
     * @param context the view of actor's group.
     */
    protected void onStart(final Channel<M> self, final Context context) {}

    /**
     * Callback for when actor is stopped. Actor will be asynchronously stopped when its {@link
     * Channel} is {@link Channel#stop stopped}.
     */
    protected void onStop() {}
}
