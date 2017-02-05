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

import org.junit.runner.RunWith;

import alioli.Scenario;
import simple.actor.testing.SpyActor;

import static com.google.common.truth.Truth.assertThat;

/** Tests for {@link SameThreadDeliverer}. */
@RunWith(Scenario.Runner.class)
public class SameThreadDelivererTest extends Scenario {
    {
        subject("same thread deliverer", () -> {
            final SpyActor<Message> actor = new SpyActor<>();
            final SameThreadDeliverer<Message> channel = new SameThreadDeliverer<>(actor);

            should("start the actor", () -> {
                assertThat(actor.isStarted()).isTrue();
            });

            should("succeed to send and deliver a message ", () -> {
                final Message message = new Message();
                assertThat(channel.send(message)).isTrue();
                assertThat(actor.getReceivedMessages()).containsExactly(message);
            });

            when("stopped", () -> {
                channel.stop();

                should("stop the actor", () -> {
                    assertThat(actor.isStopped()).isTrue();
                });

                should("fail to send a message", () -> {
                    assertThat(channel.send(new Message())).isFalse();
                    assertThat(actor.getReceivedMessages()).isEmpty();
                });
            });
        });
    }

    private static final class Message {}
}
