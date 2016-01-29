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

package android.actor.channel;

import android.actor.Channel;
import android.actor.util.Retry;
import android.support.annotation.NonNull;
import android.util.Log;

import net.jcip.annotations.ThreadSafe;

import org.jetbrains.annotations.NonNls;

import static android.actor.Configuration.MaximumNumberOfRetries;
import static android.util.Log.DEBUG;

@ThreadSafe
public class Send<M> extends Retry.Action {

    private static final String TAG = Send.class.getSimpleName();

    @NonNull
    private final Channel<? super M> mChannel;
    @NonNls
    @NonNull
    private final M mMessage;

    public Send(@NonNull final Channel<? super M> channel, @NonNls @NonNull final M message) {
        super();

        mChannel = channel;
        mMessage = message;
    }

    @Override
    public final boolean execute() {
        final int delivery = mChannel.send(mMessage);

        if (delivery == Channel.Delivery.SUCCESS) {
            if (Log.isLoggable(TAG, DEBUG)) {
                Log.d(TAG, "Successfully delivered " + mMessage); //NON-NLS
            }
        } else if (delivery == Channel.Delivery.FAILURE_NO_RETRY) {
            Log.e(TAG, "Failed to deliver " + mMessage + "! Cannot retry"); //NON-NLS
        }

        return delivery != Channel.Delivery.FAILURE_CAN_RETRY;
    }

    @Override
    protected final void onRetry(final int triesLeft) {
        Log.w(TAG, "Failed to deliver " + mMessage + "! " + //NON-NLS
                ((triesLeft > 1) ? (triesLeft + " retries left") : "1 retry left")); //NON-NLS
    }

    @Override
    protected final void onNoMoreRetries() {
        Log.e(TAG, "Failed to deliver " + mMessage + "! No more retries"); //NON-NLS
    }

    public static <M> boolean withRetries(@NonNull final Channel<? super M> channel,
                                          @NonNull final M message) {
        return withRetries(channel, message, MaximumNumberOfRetries.get());
    }

    public static <M> boolean withRetries(@NonNull final Channel<? super M> channel,
                                          @NonNull final M message,
                                          final int tries) {
        return new Retry(new Send<>(channel, message)).run(tries);
    }
}
