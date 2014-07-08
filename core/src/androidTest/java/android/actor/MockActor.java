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
import android.util.Pair;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.unmodifiableList;

public class MockActor<M> extends Actor<M> {

    private final List<Pair<System, Reference<M>>> mPostStarts = new ArrayList<>(1);
    private final List<System> mPreStops = new ArrayList<>(1);
    private final List<Pair<System, M>> mOnMessages = new ArrayList<>(1);

    public MockActor() {
        super();
    }

    @NonNull
    public final List<Pair<System, Reference<M>>> getPostStarts() {
        return unmodifiableList(mPostStarts);
    }

    @NonNull
    public final List<System> getPreStops() {
        return unmodifiableList(mPreStops);
    }

    @NonNull
    public final List<Pair<System, M>> getOnMessages() {
        return unmodifiableList(mOnMessages);
    }

    @Override
    protected final void postStart(@NonNull final System system, @NonNull final Reference<M> self) {
        mPostStarts.add(Pair.create(system, self));
    }

    @Override
    protected final void preStop(@NonNull final System system) {
        mPreStops.add(system);
    }

    @Override
    protected final void onMessage(@NonNull final System system, @NonNull final M message) {
        mOnMessages.add(Pair.create(system, message));
    }
}
