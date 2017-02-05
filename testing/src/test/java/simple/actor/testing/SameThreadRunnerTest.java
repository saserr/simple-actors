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
import simple.actor.Channel;

import static com.google.common.truth.Truth.assertThat;

/** Tests for {@link SameThreadRunner}. */
@RunWith(Scenario.Runner.class)
public class SameThreadRunnerTest extends Scenario {
    {
        subject("created channel", () -> {
            final SameThreadRunner runner = new SameThreadRunner();
            final Channel<Runnable> channel = runner.create();

            when("a task is sent", () -> {
                final SpyRunnable task = new SpyRunnable();
                final boolean success = channel.send(task);

                should("succeed to send it", () -> {
                    assertThat(success).isTrue();
                });

                should("run it", () -> {
                    assertThat(task.getExecutedTimes()).isEqualTo(1);
                });
            });

            when("stopped", () -> {
                channel.stop();

                and("a task is sent", () -> {
                    final SpyRunnable task = new SpyRunnable();
                    final boolean success = channel.send(task);

                    should("fail to send it", () -> {
                        assertThat(success).isFalse();
                    });

                    should("not run it", () -> {
                        assertThat(task.getExecutedTimes()).isEqualTo(0);
                    });
                });
            });
        });
    }
}
