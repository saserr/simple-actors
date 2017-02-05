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
import simple.actor.testing.SameThreadRunner;
import simple.actor.testing.SpyActor;

import static alioli.Asserts.assertThrows;
import static com.google.common.truth.Truth.assertThat;

/** Tests for {@link System}. */
@RunWith(Scenario.Runner.class)
public class SystemTest extends Scenario {
    {
        subject("an actor is registered", () -> {
            final System system = new System(new SameThreadRunner());
            final SpyActor<Message> actor = new SpyActor<>();
            final Channel<Message> channel = system.register(actor);

            should("be started", () -> {
                assertThat(actor.isStarted()).isTrue();
            });

            should("succeed to send and deliver a message ", () -> {
                final Message message = new Message();
                assertThat(channel.send(message)).isTrue();
                assertThat(actor.getReceivedMessages()).containsExactly(message);
            });

            and("channel is stopped", () -> {
                channel.stop();

                should("fail to send a message", () -> {
                    assertThat(channel.send(new Message())).isFalse();
                    assertThat(actor.getReceivedMessages()).isEmpty();
                });
            });

            and("system is stopped", () -> {
                system.stop();

                should("fail to send a message", () -> {
                    assertThat(channel.send(new Message())).isFalse();
                    assertThat(actor.getReceivedMessages()).isEmpty();
                });
            });

            and("system is paused", () -> {
                system.pause();

                should("succeed to send a message but not deliver it", () -> {
                    assertThat(channel.send(new Message())).isTrue();
                    assertThat(actor.getReceivedMessages()).isEmpty();
                });

                should("deliver the message after being resumed", () -> {
                    final Message message = new Message();
                    channel.send(message);
                    system.resume();

                    assertThat(actor.getReceivedMessages()).containsExactly(message);
                });

                should("deliver the message after system is resumed " +
                        "even if actor's channel was previously stopped", () -> {
                    final Message message = new Message();
                    assertThat(channel.send(message)).isTrue();
                    channel.stop();
                    system.resume();

                    assertThat(actor.getReceivedMessages()).containsExactly(message);
                });

                should("deliver messages after system is resumed " +
                        "even if system was previously stopped", () -> {
                    final Message message = new Message();
                    assertThat(channel.send(message)).isTrue();
                    system.stop();
                    system.resume();

                    assertThat(actor.getReceivedMessages()).containsExactly(message);
                });
            });
        });

        subject("system", () -> {
            final System system = new System(new SameThreadRunner());

            when("paused", () -> {
                system.pause();

                and("actor is registered", () -> {
                    final SpyActor<Object> actor = new SpyActor<>();
                    final Channel<Object> channel = system.register(actor);

                    should("not be started", () -> {
                        assertThat(actor.isStarted()).isFalse();
                    });

                    should("succeed to send a message but not deliver it", () -> {
                        assertThat(channel.send(new Object())).isTrue();
                        assertThat(actor.getReceivedMessages()).isEmpty();
                    });

                    should("be started after being resumed", () -> {
                        system.resume();

                        assertThat(actor.isStarted()).isTrue();
                    });

                    and("actor's channel is stopped", () -> {
                        channel.stop();

                        should("start the actor after system is resumed", () -> {
                            system.resume();

                            assertThat(actor.isStarted()).isTrue();
                        });
                    });

                    and("system is stopped", () -> {
                        system.stop();

                        should("start the actor after system is resumed", () -> {
                            system.resume();

                            assertThat(actor.isStarted()).isTrue();
                        });
                    });

                    should("deliver the message after being resumed", () -> {
                        final Object message = new Object();
                        channel.send(message);
                        system.resume();

                        assertThat(actor.getReceivedMessages()).containsExactly(message);
                    });
                });
            });

            when("stopped", () -> {
                system.stop();

                should("fail to register an actor", () -> {
                    final SpyActor<Object> actor = new SpyActor<>();

                    final Exception failure = assertThrows(() -> system.register(actor));
                    assertThat(failure).isInstanceOf(IllegalStateException.class);
                });
            });
        });
    }

    private static final class Message {}
}
