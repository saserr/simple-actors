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

import android.actor.channel.Mailbox;
import android.support.annotation.NonNull;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import mockit.Injectable;
import mockit.Mocked;
import mockit.StrictExpectations;
import mockit.Verifications;

import static android.actor.ContextMatchers.hasName;
import static android.actor.Providers.booleans;
import static android.actor.Providers.cartesian;
import static android.actor.Providers.successOrFailure;
import static android.actor.SystemMatchers.paused;
import static android.actor.SystemMatchers.started;
import static android.actor.SystemMatchers.stopped;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class SystemTest extends TestCase {

    @Mocked
    private Mailbox.Runner mAllMailboxRunners;
    @Mocked
    private Reference<Message> mAllReferences;

    @Injectable
    private Executor mExecutor;
    @Injectable
    private Actor<Message> mActor;

    private System mSystem;

    @BeforeMethod
    public final void setUp() {
        mSystem = new System(mExecutor);
    }

    @AfterMethod
    public final void tearDown() {
        new Verifications() {{
            mAllReferences.toString();
            minTimes = 0;
        }};

        verifyNoMoreInteractions(mAllReferences);
        verifyNoMoreInteractions(mExecutor, mActor);
    }

    @Test(groups = {"sanity", "sanity.system"})
    public final void newlyCreatedSystemIsStarted() {
        assertThat("system", mSystem, is(started()));
    }

    @Test(groups = {"sanity", "sanity.system"})
    public final void registerActor() {
        final String name = isA(RandomName);

        new StrictExpectations() {{
            final Mailbox.Runner runner = new Mailbox.Runner((Mailbox<Message>) any);
            final Reference<Message> reference =
                    new Reference<>(mSystem, new Actor.Name(null, name), (Channel<Message>) any);
            mActor.postStart((Context) any, reference);
            runner.start(mExecutor);
            result = true;
        }};

        final Reference<?> reference = mSystem.register(name, mActor);
        assertThat("actor reference", reference, is(notNullValue()));
    }

    @Test(dependsOnGroups = "sanity.system",
            expectedExceptions = UnsupportedOperationException.class,
            expectedExceptionsMessageRegExp = ".* could not be started!") //NON-NLS
    public final void registerActorThatDoesNotStart() {
        final String name = isA(RandomName);

        new StrictExpectations() {{
            final Reference<Message> reference =
                    new Reference<>(mSystem, new Actor.Name(null, name), (Channel<Message>) any);
            mActor.postStart((Context) any, reference);
            result = new RuntimeException();
        }};

        mSystem.register(name, mActor);
    }

    @Test(dependsOnGroups = "sanity.system",
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".* is already registered!")
    public final void reregisterActorWithSameName(@Injectable final Actor<Message> actor1,
                                                  @Injectable final Actor<Message> actor2) {
        final String name = isA(RandomName);

        new StrictExpectations() {{
            final Mailbox.Runner runner = new Mailbox.Runner((Mailbox<Message>) any);
            final Reference<Message> reference =
                    new Reference<>(mSystem, new Actor.Name(null, name), (Channel<Message>) any);
            actor1.postStart((Context) any, reference);
            runner.start(mExecutor);
            result = true;
        }};

        final Reference<?> reference = mSystem.register(name, actor1);
        assertThat("actor reference", reference, is(notNullValue()));

        mSystem.register(name, actor2);

        verifyNoMoreInteractions(actor1, actor2);
    }

    @Test(dependsOnGroups = "sanity.system")
    public final void reregisterActorWithSameNameAfterActorStop(@Injectable final Actor<Message> actor1,
                                                                @Injectable final Actor<Message> actor2) {
        final String name = isA(RandomName);

        new StrictExpectations() {{
            final Mailbox.Runner runner1 = new Mailbox.Runner((Mailbox<Message>) any);
            final Reference<Message> reference1 =
                    new Reference<>(mSystem, new Actor.Name(null, name), (Channel<Message>) any);
            actor1.postStart((Context) any, reference1);
            runner1.start(mExecutor);
            result = true;
            runner1.stop();
            result = true;

            final Mailbox.Runner runner2 = new Mailbox.Runner((Mailbox<Message>) any);
            final Reference<Message> reference2 =
                    new Reference<>(mSystem, new Actor.Name(null, name), (Channel<Message>) any);
            actor2.postStart((Context) any, reference2);
            runner2.start(mExecutor);
            result = true;
        }};


        final Reference<?> reference1 = mSystem.register(name, actor1);
        assertThat("first actor reference", reference1, is(notNullValue()));

        assertThat("first actor stop", mSystem.stop(new Actor.Name(null, name)), is(true));

        final Reference<?> reference2 = mSystem.register(name, actor2);
        assertThat("second actor reference", reference2, is(notNullValue()));

        verifyNoMoreInteractions(actor1, actor2);
    }

    @Test(groups = {"sanity", "sanity.system"}, dependsOnMethods = "registerActor")
    public final void registeredActorContext() {
        final String name = isA(RandomName);
        final Context context = Captured.as(Context.class);

        new StrictExpectations() {{
            final Mailbox.Runner runner = new Mailbox.Runner((Mailbox<Message>) any);
            final Reference<Message> reference =
                    new Reference<>(mSystem, new Actor.Name(null, name), (Channel<Message>) any);
            mActor.postStart(withArgThat(is(Captured.into(context))), reference);
            runner.start(mExecutor);
            result = true;
        }};

        assertThat("actor reference", mSystem.register(name, mActor), is(notNullValue()));

        assertThat("context", context, hasName(new Actor.Name(null, name)));
        assertThat("context system", context.getSystem(), is(notNullValue()));
    }

    @Test(dependsOnGroups = "sanity.system")
    public final void registerChildActor(@Injectable final Actor<Message> parent,
                                         @Injectable final Actor<Message> child) {
        final String parentName = isA(RandomName);
        final String childName = isA(RandomName);
        final Context context = Captured.as(Context.class);

        new StrictExpectations() {{
            final Mailbox.Runner parentRunner = new Mailbox.Runner((Mailbox<Message>) any);
            final Reference<Message> parentReference =
                    new Reference<>(mSystem, new Actor.Name(null, parentName), (Channel<Message>) any);
            parent.postStart(withArgThat(is(Captured.into(context))), parentReference);
            parentRunner.start(mExecutor);
            result = true;

            final Mailbox.Runner childRunner = new Mailbox.Runner((Mailbox<Message>) any);
            final Reference<Message> childReference =
                    new Reference<>(
                            mSystem,
                            new Actor.Name(new Actor.Name(null, parentName), childName),
                            (Channel<Message>) any);
            child.postStart((Context) any, childReference);
            childRunner.start(mExecutor);
            result = true;
        }};

        final Reference<?> reference = mSystem.register(parentName, parent);
        assertThat("parent reference", reference, is(notNullValue()));
        assertThat("child reference", context.register(childName, child), is(notNullValue()));

        verifyNoMoreInteractions(parent, child);
    }

    @Test(dependsOnGroups = "sanity.system", dependsOnMethods = "registerChildActor")
    public final void registeredChildActorContext(@Injectable final Actor<Message> parent,
                                                  @Injectable final Actor<Message> child) {
        final String parentName = isA(RandomName);
        final String childName = isA(RandomName);
        final Context parentContext = Captured.as(Context.class);
        final Context childContext = Captured.as(Context.class);

        new StrictExpectations() {{
            final Mailbox.Runner parentRunner = new Mailbox.Runner((Mailbox<Message>) any);
            final Reference<Message> parentReference =
                    new Reference<>(mSystem, new Actor.Name(null, parentName), (Channel<Message>) any);
            parent.postStart(withArgThat(is(Captured.into(parentContext))), parentReference);
            parentRunner.start(mExecutor);
            result = true;

            final Mailbox.Runner childRunner = new Mailbox.Runner((Mailbox<Message>) any);
            final Reference<Message> childReference =
                    new Reference<>(
                            mSystem,
                            new Actor.Name(new Actor.Name(null, parentName), childName),
                            (Channel<Message>) any);
            child.postStart(withArgThat(is(Captured.into(childContext))), childReference);
            childRunner.start(mExecutor);
            result = true;
        }};

        assertThat("parent reference", mSystem.register(parentName, parent), is(notNullValue()));
        assertThat("child reference", parentContext.register(childName, child), is(notNullValue()));

        assertThat("child context", childContext,
                hasName(new Actor.Name(new Actor.Name(null, parentName), childName)));
        assertThat("child context system", childContext.getSystem(), is(notNullValue()));

        verifyNoMoreInteractions(parent, child);
    }

    @Test(dependsOnGroups = "sanity.system", dependsOnMethods = "registerChildActor")
    public final void stopParentActorWithChildren(@Injectable final Actor<Message> parent,
                                                  @Injectable final Actor<Message> child) {
        final String parentName = isA(RandomName);
        final String childName = isA(RandomName);
        final Context context = Captured.as(Context.class);

        new StrictExpectations() {{
            final Mailbox.Runner parentRunner = new Mailbox.Runner((Mailbox<Message>) any);
            final Reference<Message> parentReference =
                    new Reference<>(mSystem, new Actor.Name(null, parentName), (Channel<Message>) any);
            parent.postStart(withArgThat(is(Captured.into(context))), parentReference);
            parentRunner.start(mExecutor);
            result = true;

            final Mailbox.Runner childRunner = new Mailbox.Runner((Mailbox<Message>) any);
            final Reference<Message> childReference =
                    new Reference<>(
                            mSystem,
                            new Actor.Name(new Actor.Name(null, parentName), childName),
                            (Channel<Message>) any);
            child.postStart((Context) any, childReference);
            childRunner.start(mExecutor);
            result = true;

            parentRunner.stop();
            result = true;
            childRunner.stop();
            result = true;
        }};

        final Reference<?> parenReference = mSystem.register(parentName, parent);
        assertThat("parent reference", parenReference, is(notNullValue()));
        assertThat("child reference", context.register(childName, child), is(notNullValue()));
        mSystem.stop(new Actor.Name(null, parentName));

        verifyNoMoreInteractions(parent, child);
    }

    @Test(dependsOnGroups = "sanity.system")
    public final void doubleStart() {
        final String name = isA(RandomName);

        new StrictExpectations() {{
            final Mailbox.Runner runner = new Mailbox.Runner((Mailbox<Message>) any);
            final Reference<Message> reference =
                    new Reference<>(mSystem, new Actor.Name(null, name), (Channel<Message>) any);
            mActor.postStart((Context) any, reference);
            runner.start(mExecutor);
            result = true;
        }};

        final Reference<?> reference = mSystem.register(name, mActor);
        assertThat("actor reference", reference, is(notNullValue()));

        assertThat("system start", mSystem.start(), is(true));
        assertThat("system", mSystem, is(started()));
    }

    @Test(dependsOnGroups = "sanity.system",
            dataProvider = "success or failure", dataProviderClass = Providers.class)
    public final void pause(final Providers.Boolean success) {
        final String name = isA(RandomName);

        new StrictExpectations() {{
            final Mailbox.Runner runner = new Mailbox.Runner((Mailbox<Message>) any);
            final Reference<Message> reference =
                    new Reference<>(mSystem, new Actor.Name(null, name), (Channel<Message>) any);
            mActor.postStart((Context) any, reference);
            runner.start(mExecutor);
            result = true;

            runner.stop();
            result = success.value();
        }};

        final Reference<?> reference = mSystem.register(name, mActor);
        assertThat("actor reference", reference, is(notNullValue()));

        assertThat("system pause", mSystem.pause(), is(success.value()));
        assertThat("system", mSystem, is(paused()));
    }

    @Test(dependsOnGroups = "sanity.system", dependsOnMethods = "pause")
    public final void doublePause() {
        final String name = isA(RandomName);

        new StrictExpectations() {{
            final Mailbox.Runner runner = new Mailbox.Runner((Mailbox<Message>) any);
            final Reference<Message> reference =
                    new Reference<>(mSystem, new Actor.Name(null, name), (Channel<Message>) any);
            mActor.postStart((Context) any, reference);
            runner.start(mExecutor);
            result = true;

            runner.stop();
            result = true;
        }};

        final Reference<?> reference = mSystem.register(name, mActor);
        assertThat("actor reference", reference, is(notNullValue()));

        assertThat("system pause", mSystem.pause(), is(true));
        assertThat("system pause", mSystem.pause(), is(true));
        assertThat("system", mSystem, is(paused()));
    }

    @Test(dependsOnGroups = "sanity.system",
            dataProvider = "success or failure", dataProviderClass = Providers.class)
    public final void startAfterPause(final Providers.Boolean started) {
        final String name = isA(RandomName);

        new StrictExpectations() {{
            final Mailbox.Runner runner = new Mailbox.Runner((Mailbox<Message>) any);
            final Reference<Message> reference =
                    new Reference<>(mSystem, new Actor.Name(null, name), (Channel<Message>) any);
            mActor.postStart((Context) any, reference);
            runner.start(mExecutor);
            result = true;

            runner.stop();
            result = true;

            // TODO don't start the paused actor that have already been started
            mActor.postStart((Context) any, reference);
            runner.start(mExecutor);
            result = started.value();
        }};

        final Reference<?> reference = mSystem.register(name, mActor);
        assertThat("actor reference", reference, is(notNullValue()));

        assertThat("system pause", mSystem.pause(), is(true));
        assertThat("system start", mSystem.start(), is(started.value()));
        assertThat("system", mSystem, is(started()));
    }

    @Test(dependsOnGroups = "sanity.system")
    public final void registerActorWhenPaused() {
        final String name = isA(RandomName);

        assertThat("system pause", mSystem.pause(), is(true));
        assertThat("system", mSystem, is(paused()));

        new StrictExpectations() {{
            new Reference<>(mSystem, new Actor.Name(null, name), (Channel<Message>) any);
        }};

        final Reference<?> reference = mSystem.register(name, mActor);
        assertThat("actor reference", reference, is(notNullValue()));
    }

    @Test(dependsOnGroups = "sanity.system", dataProvider = "(success, immediateness)")
    public final void stop(final Providers.Boolean success,
                           final Providers.Boolean immediately) {
        final String name = isA(RandomName);

        new StrictExpectations() {{
            final Mailbox.Runner runner = new Mailbox.Runner((Mailbox<Message>) any);
            final Reference<Message> reference =
                    new Reference<>(mSystem, new Actor.Name(null, name), (Channel<Message>) any);
            mActor.postStart((Context) any, reference);
            runner.start(mExecutor);
            result = true;
            runner.stop();
            result = success.value();
        }};

        final Reference<?> reference = mSystem.register(name, mActor);
        assertThat("actor reference", reference, is(notNullValue()));

        assertThat("system stop", mSystem.stop(immediately.value()), is(success.value()));
        assertThat("system", mSystem, is(stopped()));
    }

    @Test(dependsOnGroups = "sanity.system", dependsOnMethods = "stop",
            dataProvider = "immediateness")
    public final void doubleStop(final Providers.Boolean immediately) {
        final String name = isA(RandomName);

        new StrictExpectations() {{
            final Mailbox.Runner runner = new Mailbox.Runner((Mailbox<Message>) any);
            final Reference<Message> reference =
                    new Reference<>(mSystem, new Actor.Name(null, name), (Channel<Message>) any);
            mActor.postStart((Context) any, reference);
            runner.start(mExecutor);
            result = true;
            runner.stop();
            result = true;
        }};

        final Reference<?> reference = mSystem.register(name, mActor);
        assertThat("actor reference", reference, is(notNullValue()));

        assertThat("system stop", mSystem.stop(immediately.value()), is(true));
        assertThat("system stop", mSystem.stop(immediately.value()), is(true));
        assertThat("system", mSystem, is(stopped()));
    }

    @Test(dependsOnGroups = "sanity.system", dependsOnMethods = "stop",
            dataProvider = "immediateness",
            expectedExceptions = UnsupportedOperationException.class,
            expectedExceptionsMessageRegExp = System.STOPPED)
    public final void registerActorAfterStop(final Providers.Boolean immediately) {
        new StrictExpectations() {{
            new Reference<>(mSystem, (Actor.Name) any, (Channel<Message>) any);
            maxTimes = 0;
        }};

        assertThat("system stop", mSystem.stop(immediately.value()), is(true));
        mSystem.register(a(RandomName), mActor);
    }

    @Test(dependsOnGroups = "sanity.system", dependsOnMethods = "stop",
            dataProvider = "immediateness",
            expectedExceptions = UnsupportedOperationException.class,
            expectedExceptionsMessageRegExp = System.STOPPED)
    public final void startAfterStop(final Providers.Boolean immediately) {
        assertThat("system stop", mSystem.stop(immediately.value()), is(true));
        mSystem.start();
    }

    @Test(dependsOnGroups = "sanity.system", dependsOnMethods = "stop",
            dataProvider = "immediateness",
            expectedExceptions = UnsupportedOperationException.class,
            expectedExceptionsMessageRegExp = System.STOPPED)
    public final void pauseAfterStop(final Providers.Boolean immediately) {
        assertThat("system stop", mSystem.stop(immediately.value()), is(true));
        mSystem.pause();
    }

    private static final RandomDataGenerator<String> RandomName = RandomString;

    @NonNull
    @DataProvider(name = "(success, immediateness)")
    private static Object[][] successAndImmediateness() {
        return cartesian(successOrFailure(), immediateness());
    }

    @NonNull
    @DataProvider(name = "immediateness")
    private static Object[][] immediateness() {
        return booleans("not immediately", "immediately");
    }

    private interface Message {
    }
}
