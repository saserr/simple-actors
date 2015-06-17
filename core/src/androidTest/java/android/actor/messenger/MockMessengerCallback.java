/*
 * Copyright 2015 the original author or authors
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

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.unmodifiableList;

public class MockMessengerCallback<M> implements Messenger.Callback<M> {

    private final List<Integer> mSystemMessages = new ArrayList<>(1);
    private final List<M> mUserMessages = new ArrayList<>(1);

    public MockMessengerCallback() {
        super();
    }

    @NonNull
    public final List<Integer> getSystemMessages() {
        return unmodifiableList(mSystemMessages);
    }

    @NonNull
    public final List<M> getUserMessages() {
        return unmodifiableList(mUserMessages);
    }

    @Override
    public final boolean onMessage(final int message) {
        mSystemMessages.add(message);
        return true;
    }

    @Override
    public final boolean onMessage(@NonNull final M message) {
        mUserMessages.add(message);
        return true;
    }
}
