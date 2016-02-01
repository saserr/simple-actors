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

import android.support.annotation.IntDef;
import android.support.annotation.NonNull;

import net.jcip.annotations.ThreadSafe;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.SOURCE;

@ThreadSafe
public class Retry {

    public static final int SUCCESS = 1;
    public static final int AGAIN = 2;
    public static final int FAILURE = 3;

    @NonNull
    private final Action mAction;

    public Retry(@NonNull final Action action) {
        super();

        mAction = action;
    }

    @Result
    public final int run(final int tries) {
        if (tries < 1) {
            throw new IllegalArgumentException("Tries must be positive number!");
        }

        @Result int result;

        int triesLeft = tries;
        do {
            result = mAction.execute();
            triesLeft--;
            if ((result == AGAIN) && (triesLeft > 0)) {
                mAction.onRetry(triesLeft);
            }
        } while ((result == AGAIN) && (triesLeft > 0));

        if (result == AGAIN) {
            mAction.onNoMoreRetries();
        }

        return result;
    }

    @Retention(SOURCE)
    @IntDef({SUCCESS, AGAIN, FAILURE})
    public @interface Result {}

    public abstract static class Action {

        @Result
        public abstract int execute();

        protected void onRetry(final int triesLeft) {/* do nothing */}

        protected void onNoMoreRetries() {/* do nothing */}
    }
}
