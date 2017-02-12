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
import simple.actor.testing.SpyChannel;

import static com.google.common.truth.Truth.assertThat;

/** Tests for {@link Mailbox}. */
@RunWith(Scenario.Runner.class)
public class MailboxTest extends Scenario {
    {
        subject("connected mailbox", () -> {
            final SpyChannel<Message> channel = new SpyChannel<>();
            final Mailbox<Message> mailbox = new Mailbox<>(channel);

            should("delegate sending of message to the connected channel", () -> {
                final Message message = new Message();
                mailbox.send(message);

                assertThat(channel.getSentMessages()).containsExactly(message);
            });

            should("fail to send a message if the connected channel fails to send", () -> {
                channel.stop();

                assertThat(mailbox.send(new Message())).isFalse();
            });

            when("disconnected from the connected channel", () -> {
                mailbox.disconnect();

                should("not delegate sending of message to the channel anymore", () -> {
                    assertThat(mailbox.send(new Message())).isTrue();
                    assertThat(channel.getSentMessages()).isEmpty();
                });
            });

            when("stopped", () -> {
                mailbox.stop();

                should("stop the connected channel", () -> {
                    assertThat(channel.isStopped()).isTrue();
                });
            });
        });

        subject("disconnected mailbox", () -> {
            final Mailbox<Message> mailbox = new Mailbox<>();

            should("succeed to send a message", () -> {
                assertThat(mailbox.send(new Message())).isTrue();
            });

            when("a message is sent", () -> {
                final Message message = new Message();
                mailbox.send(message);

                and("a channel is connected", () -> {
                    final SpyChannel<Message> channel = new SpyChannel<>();
                    mailbox.connect(channel);

                    should("resend all messages that were sent while being disconnected", () -> {
                        assertThat(channel.getSentMessages()).containsExactly(message);
                    });
                });
            });

            when("stopped", () -> {
                final Message message = new Message();
                mailbox.send(message);
                mailbox.stop();

                should("fail to send a message", () -> {
                    assertThat(mailbox.send(new Message())).isFalse();
                });

                and("a channel is connected", () -> {
                    mailbox.send(new Message());
                    final SpyChannel<Message> channel = new SpyChannel<>();
                    mailbox.connect(channel);

                    should("stop the channel", () -> {
                        assertThat(channel.isStopped()).isTrue();
                    });

                    should("resend only messages that were sent before being stopped", () -> {
                        assertThat(channel.getSentMessages()).containsExactly(message);
                    });
                });
            });
        });
    }

    private static final class Message {}
}
