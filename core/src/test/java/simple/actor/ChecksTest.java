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

import static alioli.Asserts.assertThrows;
import static com.google.common.truth.Truth.assertThat;
import static simple.actor.Checks.checkNotNull;

/** Tests for {@link Checks}. */
@RunWith(Scenario.Runner.class)
public class ChecksTest extends Scenario {
    {
        subject("checkNotNull", () -> {
            when("passed a non-null value", () -> {
                final Object value = new Object();

                should("return the value", () -> {
                    assertThat(checkNotNull(value, "value")).isNotNull();
                });
            });

            when("passed a null value", () -> {
                final Object value = null;

                should("throw an IllegalStateException", () -> {
                    final String name = "value";
                    final Exception failure = assertThrows(() -> checkNotNull(value, name));

                    assertThat(failure).isInstanceOf(IllegalStateException.class);
                    assertThat(failure).hasMessageThat().contains(name);
                });
            });
        });
    }
}
