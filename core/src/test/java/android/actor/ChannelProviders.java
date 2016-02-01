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

import android.support.annotation.NonNull;

import org.testng.annotations.DataProvider;

import static android.actor.Providers.booleans;
import static android.actor.Providers.provider;
import static android.actor.Providers.value;

public final class ChannelProviders {

    @NonNull
    @DataProvider(name = "delivery")
    public static Object[][] delivery() {
        return provider(
                value("success", Channel.Delivery.SUCCESS),
                value("failure", Channel.Delivery.FAILURE),
                value("error", Channel.Delivery.ERROR)
        );
    }

    @NonNull
    @DataProvider(name = "immediateness")
    public static Object[][] immediateness() {
        return booleans("not immediately", "immediately");
    }

    private ChannelProviders() {
        super();
    }
}
