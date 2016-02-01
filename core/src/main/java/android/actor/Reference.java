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

import android.actor.channel.Send;
import android.actor.util.Retry;
import android.support.annotation.NonNull;

import net.jcip.annotations.ThreadSafe;

import org.jetbrains.annotations.NonNls;

@ThreadSafe
public class Reference<M> {

    @NonNull
    private final System mSystem;
    @NonNull
    private final Actor.Name mName;
    @NonNull
    private final Channel<M> mChannel;

    public Reference(@NonNull final System system,
                     @NonNull final Actor.Name name,
                     @NonNull final Channel<M> channel) {
        super();

        mSystem = system;
        mName = name;
        mChannel = channel;
    }

    @NonNull
    public final Actor.Name getName() {
        return mName;
    }

    public final boolean tell(@NonNull final M message) {
        return Send.withRetries(mChannel, message) == Retry.SUCCESS;
    }

    public final boolean start() {
        return mSystem.start(mName);
    }

    public final boolean pause() {
        return mSystem.pause(mName);
    }

    public final boolean stop() {
        return mSystem.stop(mName);
    }

    @NonNls
    @NonNull
    @Override
    public final String toString() {
        return "Actor(" + mName + ')';
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public final <N extends M> Reference<N> cast() {
        return (Reference<N>) this;
    }
}
