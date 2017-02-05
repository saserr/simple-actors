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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility methods to check if class is in correct state.
 */
public final class Checks {

    /**
     * Ensures that the given value is not {@code null}.
     *
     * @param value the value to be checked.
     * @param name  the human-readable name for the value that will be used in the exception message
     *              if the value is null.
     * @param <T>   the type of the value.
     *
     * @return the non-{@code null} value.
     *
     * @throws IllegalStateException if value is {@code null}.
     */
    @NotNull
    public static <T> T checkNotNull(@Nullable final T value, final String name) {
        if (value == null) {
            throw new IllegalStateException(name + " should not have been null");
        }
        return value;
    }

    private Checks() {}
}
