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
 * Common interface for sending messages. All sent messages have to be of of the generic type {@code
 * M}.
 *
 * @param <M> the type of sent messages.
 */
public interface Channel<M> {

    /**
     * Sends the given message.
     *
     * <p>Note that if channel has been stopped, the send request will be ignored and the {@code
     * false} value will be returned.
     *
     * @return {@code true} if channel has not been stopped; otherwise {@code false}.
     */
    boolean send(M message);

    /**
     * Stops the channel. After stop, no more messages can be sent on the channel. However,
     * all previously sent messages that are still not delivered will be delivered.
     *
     * <p>Stopping channel multiple times has same meaning as stopping it once; in other words, all
     * stops after the first one will be ignored.
     */
    void stop();
}
