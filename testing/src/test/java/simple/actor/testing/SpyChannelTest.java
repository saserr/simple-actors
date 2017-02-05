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

import org.junit.runner.RunWith;

import alioli.Scenario;

import static com.google.common.truth.Truth.assertThat;

/** Tests for {@link SpyChannel}. */
@RunWith(Scenario.Runner.class)
public class SpyChannelTest extends Scenario {
    {
        subject("spy channel", () -> {
            final SpyChannel<Message> channel = new SpyChannel<>();

            should("have no sent messages", () -> {
                assertThat(channel.getSentMessages()).isEmpty();
            });

            should("not be stopped", () -> {
                assertThat(channel.isStopped()).isFalse();
            });

            when("a message is sent", () -> {
                final Message message = new Message();
                final boolean success = channel.send(message);

                should("succeed to send it", () -> {
                    assertThat(success).isTrue();
                });

                should("remember it", () -> {
                    assertThat(channel.getSentMessages()).containsExactly(message);
                });
            });

            when("stopped", () -> {
                channel.stop();

                should("be stopped", () -> {
                    assertThat(channel.isStopped()).isTrue();
                });

                and("a message is sent", () -> {
                    final Message message = new Message();
                    final boolean success = channel.send(message);

                    should("fail to send it", () -> {
                        assertThat(success).isFalse();
                    });

                    should("not remember it", () -> {
                        assertThat(channel.getSentMessages()).isEmpty();
                    });
                });
            });
        });
    }

    private static final class Message {}
}
