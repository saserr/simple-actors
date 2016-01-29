/*
 * Copyright 2016 the original author or authors
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

package android.actor.util;

import android.support.annotation.NonNull;

import net.jcip.annotations.ThreadSafe;

@ThreadSafe
public class Retry {

    @NonNull
    private final Action mAction;

    public Retry(@NonNull final Action action) {
        super();

        mAction = action;
    }

    public final boolean run(final int tries) {
        if (tries < 1) {
            throw new IllegalArgumentException("Tries must be positive number!");
        }

        boolean success;

        int triesLeft = tries;
        do {
            success = mAction.execute();
            triesLeft--;
            if (!success && (triesLeft > 0)) {
                mAction.onRetry(triesLeft);
            }
        } while (!success && (triesLeft > 0));

        if (!success) {
            mAction.onNoMoreRetries();
        }

        return success;
    }

    public abstract static class Action {

        public abstract boolean execute();

        protected void onRetry(final int triesLeft) {/* do nothing */}

        protected void onNoMoreRetries() {/* do nothing */}
    }
}
