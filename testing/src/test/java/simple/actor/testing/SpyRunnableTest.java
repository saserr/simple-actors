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

/** Tests for {@link SpyRunnable}. */
@RunWith(Scenario.Runner.class)
public class SpyRunnableTest extends Scenario {
    {
        subject("spy runnable", () -> {
            final SpyRunnable runnable = new SpyRunnable();

            should("not be executed", () -> {
                assertThat(runnable.getExecutedTimes()).isEqualTo(0);
            });

            when("when executed", () -> {
                runnable.run();

                should("remember it", () -> {
                    assertThat(runnable.getExecutedTimes()).isEqualTo(1);
                });
            });

            when("when executed multiple times", () -> {
                runnable.run();
                runnable.run();

                should("remember all of them", () -> {
                    assertThat(runnable.getExecutedTimes()).isEqualTo(2);
                });
            });
        });
    }
}
