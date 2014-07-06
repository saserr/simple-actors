/*
 * Copyright 2014 the original author or authors
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

package android.actor;

import android.support.annotation.NonNull;

import org.jetbrains.annotations.NonNls;

import static junit.framework.Assert.fail;

public class DummyActor<M> extends Actor<M> {

    public DummyActor() {
        super();
    }

    @Override
    protected final void onMessage(@NonNull final System system, @NonNls @NonNull final M message) {
        fail("An unexpected message was received: " + message); //NON-NLS
    }
}
