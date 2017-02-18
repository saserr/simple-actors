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

/** The view of {@link Actor}'s group from the perspective of an {@code Actor}. */
public interface Context {

    /**
     * Registers the given {@link Actor} with the group. The returned {@link Channel} has be used
     * to send messages to the {@code Actor}.
     *
     * @param actor the {@code Actor} that should be registered with the group.
     * @param <M>   the type of messages that {@code Actor} receives.
     *
     * @return a {@code Channel} to send messages to the {@code Actor}.
     */
    <M> Channel<M> register(Actor<M> actor);
}
