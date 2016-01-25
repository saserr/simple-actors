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

package android.actor;

import android.support.annotation.IntDef;
import android.support.annotation.NonNull;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.SOURCE;

public interface Channel<M> {

    @Delivery
    int send(final int message);

    @Delivery
    int send(@NonNull final M message);

    boolean stop(final boolean immediately);

    @Retention(SOURCE)
    @IntDef({Delivery.SUCCESS, Delivery.FAILURE_CAN_RETRY, Delivery.FAILURE_NO_RETRY})
    @interface Delivery {
        int SUCCESS = 1;
        int FAILURE_CAN_RETRY = 2;
        int FAILURE_NO_RETRY = 3;
    }

    interface Factory {
        @NonNull
        <M> Channel<M> create(@NonNull final Channel<M> channel);
    }
}
