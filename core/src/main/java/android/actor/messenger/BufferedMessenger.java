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

package android.actor.messenger;

import android.actor.Messenger;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.jcip.annotations.NotThreadSafe;

@NotThreadSafe
public class BufferedMessenger<M> implements Messenger<M> {

    @NonNull
    private final Messenger.Callback<M> mCallback;
    private final Mailbox<M> mMailbox = new Mailbox<>();

    @Nullable
    private Messenger<M> mMessenger;

    public BufferedMessenger(@NonNull final Messenger.Callback<M> callback) {
        super();

        mCallback = callback;
    }

    public final boolean isAttached() {
        return mMessenger != null;
    }

    public final boolean attach(@NonNull final Factory factory) {
        boolean success = (mMessenger == null) || mMessenger.stop(false);

        if (success) {
            mMessenger = factory.create(mCallback);
            while (success && !mMailbox.isEmpty()) {
                if (!mMailbox.take().send(mMessenger)) {
                    success = false;
                    mMessenger = null;
                }
            }
        }

        return success;
    }

    public final void detach() {
        mMessenger = null;
    }

    @Override
    public final boolean send(final int message) {
        return (mMessenger == null) ?
                (mMailbox.isEmpty() ? mCallback.onMessage(message) : mMailbox.put(message)) :
                mMessenger.send(message);
    }

    @Override
    public final boolean send(@NonNull final M message, final long delay) {
        return (mMessenger == null) ?
                mMailbox.put(message, delay) :
                mMessenger.send(message, delay);
    }

    @Override
    public final boolean stop(final boolean immediately) {
        final boolean success = (mMessenger == null) || mMessenger.stop(immediately);

        if (success) {
            mMessenger = null;
            if (immediately) {
                mMailbox.clear();
            } else {
                while (!mMailbox.isEmpty()) {
                    mMailbox.take().send(mCallback);
                }
            }
        }

        return success;
    }
}
