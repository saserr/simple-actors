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
import simple.actor.Actor;

import static com.google.common.truth.Truth.assertThat;

/** Tests for {@link SpyContext}. */
@RunWith(Scenario.Runner.class)
public class SpyContextTest extends Scenario {
    {
        subject("spy context", () -> {
            final SpyContext context = new SpyContext();

            should("have no registered actors", () -> {
                assertThat(context.getRegisteredActors()).isEmpty();
            });

            when("actor is registered", () -> {
                final Actor<Object> actor = new SpyActor<>();
                context.register(actor);

                should("remember it", () -> {
                    assertThat(context.getRegisteredActors()).containsExactly(actor);
                });
            });
        });
    }
}
