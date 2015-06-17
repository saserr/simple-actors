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

import android.actor.TestCase;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class MailboxTest extends TestCase {

    private Mailbox<Integer> mMailbox;

    @Override
    public final void setUp() throws Exception {
        super.setUp();

        mMailbox = new Mailbox<>();
    }

    @Override
    public final void tearDown() throws Exception {
        mMailbox = null;

        super.tearDown();
    }

    public final void testNewMailboxIsEmpty() {
        assertThat("freshly created mailbox is empty", mMailbox.isEmpty(), is(true));
    }

    public final void testMailboxIsNotEmptyAfterPut() {
        assertThat("mailbox put", mMailbox.put(a(RandomInteger), 0), is(true));
        assertThat("mailbox is not empty after put", mMailbox.isEmpty(), is(false));
    }

    public final void testMailboxIsEmptyAfterClear() {
        assertThat("mailbox put", mMailbox.put(a(RandomInteger), 0), is(true));
        mMailbox.clear();
        assertThat("mailbox is empty after clear", mMailbox.isEmpty(), is(true));
    }

    public final void testMailboxIsEmptyAfterAllMessagesAreTaken() {
        assertThat("mailbox put", mMailbox.put(a(RandomInteger), 0), is(true));
        assertThat("mailbox take", mMailbox.take(), is(notNullValue()));
        assertThat("mailbox is empty after all messages are taken", mMailbox.isEmpty(), is(true));
    }

    public final void testUserMessageThatIsPutCanBeTaken() {
        final Integer expected = a(RandomInteger);
        assertThat("mailbox put", mMailbox.put(expected, 0), is(true));

        final MockMessengerCallback<Integer> callback = new MockMessengerCallback<>();
        assertThat("mailbox take", mMailbox.take().send(callback), is(true));

        final List<Integer> systemMessages = callback.getSystemMessages();
        assertThat("number of system messages", systemMessages.size(), is(0));

        final List<Integer> userMessages = callback.getUserMessages();
        assertThat("number of user messages", userMessages.size(), is(1));
        assertThat("user message", userMessages.get(0), is(expected));
    }

    public final void testSystemMessageThatIsPutCanBeTaken() {
        final int expected = a(RandomInteger);
        assertThat("mailbox put", mMailbox.put(expected), is(true));

        final MockMessengerCallback<Integer> callback = new MockMessengerCallback<>();
        assertThat("mailbox take", mMailbox.take().send(callback), is(true));

        final List<Integer> systemMessages = callback.getSystemMessages();
        assertThat("number of system messages", systemMessages.size(), is(1));
        assertThat("system message", systemMessages.get(0), is(expected));

        final List<Integer> userMessages = callback.getUserMessages();
        assertThat("number of user messages", userMessages.size(), is(0));
    }
}
