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
import static android.actor.System.SYSTEM_STOPPED;
import static android.actor.SystemMatchers.paused;
import static android.actor.SystemMatchers.started;
import static android.actor.SystemMatchers.stopped;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class SystemTest extends TestCase {

    @Mocked
    private Reference<?> mAllReferences;

    @Injectable
    private Executor mExecutor;
    @Injectable
    private Actor<?> mActor;

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
        new StrictExpectations() {{
            final Reference<?> reference =
                    new Reference<>((Context) any, mActor, (Reference.Callback) any);
            reference.start(mExecutor);
            result = true;
        }};

        final Reference<?> reference = mSystem.register(isA(RandomName), mActor);
        assertThat("actor reference", reference, is(notNullValue()));
    }

    @Test(dependsOnGroups = "sanity.system",
            expectedExceptions = UnsupportedOperationException.class,
            expectedExceptionsMessageRegExp = ".* could not be started!") //NON-NLS
    public final void registerActorThatDoesNotStart() {
        new StrictExpectations() {{
            final Reference<?> reference =
                    new Reference<>((Context) any, mActor, (Reference.Callback) any);
            reference.start(mExecutor);
            result = false;
        }};

        mSystem.register(a(RandomName), mActor);
    }

    @Test(dependsOnGroups = "sanity.system",
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".* is already registered!")
    public final void reregisterActorWithSameName(@Injectable final Actor<?> actor1,
                                                  @Injectable final Actor<?> actor2) {
        new StrictExpectations() {{
            final Reference<?> reference =
                    new Reference<>((Context) any, actor1, (Reference.Callback) any);
            reference.start(mExecutor);
            result = true;
        }};

        final String name = isA(RandomName);
        final Reference<?> reference = mSystem.register(name, actor1);
        assertThat("actor reference", reference, is(notNullValue()));

        mSystem.register(name, actor2);

        verifyNoMoreInteractions(actor1, actor2);
    }

    @Test(dependsOnGroups = "sanity.system")
    public final void reregisterActorWithSameNameAfterActorStop(@Injectable final Actor<?> actor1,
                                                                @Injectable final Actor<?> actor2) {
        final Context context = Captured.as(Context.class);
        final Reference.Callback callback = Captured.as(Reference.Callback.class);

        new StrictExpectations() {{
            final Reference<?> reference1 = new Reference<>(
                    withArgThat(is(Captured.into(context))),
                    actor1,
                    withArgThat(is(Captured.into(callback)))
            );
            reference1.start(mExecutor);
            result = true;

            final Reference<?> reference2 =
                    new Reference<>((Context) any, actor2, (Reference.Callback) any);
            reference2.start(mExecutor);
            result = true;
        }};

        final String name = isA(RandomName);
        final Reference<?> reference1 = mSystem.register(name, actor1);
        assertThat("first actor reference", reference1, is(notNullValue()));

        callback.onStop(context.getName());

        final Reference<?> reference2 = mSystem.register(name, actor2);
        assertThat("second actor reference", reference2, is(notNullValue()));

        verifyNoMoreInteractions(actor1, actor2);
    }

    @Test(groups = {"sanity", "sanity.system"}, dependsOnMethods = "registerActor")
    public final void registeredActorContext() {
        final Context context = Captured.as(Context.class);

        new StrictExpectations() {{
            final Reference<?> reference = new Reference<>(
                    withArgThat(is(Captured.into(context))),
                    mActor,
                    (Reference.Callback) any
            );
            reference.start(mExecutor);
            result = true;
        }};

        final String name = isA(RandomName);
        assertThat("actor reference", mSystem.register(name, mActor), is(notNullValue()));

        assertThat("context", context, hasName(new Actor.Name(null, name)));
        assertThat("context system", context.getSystem(), is(notNullValue()));
    }

    @Test(dependsOnGroups = "sanity.system")
    public final void registerChildActor(@Injectable final Actor<?> parent,
                                         @Injectable final Actor<?> child) {
        final Context context = Captured.as(Context.class);

        new StrictExpectations() {{
            final Reference<?> parentReference = new Reference<>(
                    withArgThat(is(Captured.into(context))),
                    parent,
                    (Reference.Callback) any
            );
            parentReference.start(mExecutor);
            result = true;
            final Reference<?> childReference =
                    new Reference<>((Context) any, child, (Reference.Callback) any);
            childReference.start(mExecutor);
            result = true;
        }};

        final Reference<?> reference = mSystem.register(isA(RandomName), parent);
        assertThat("parent reference", reference, is(notNullValue()));
        assertThat("child reference", context.register(isA(RandomName), child), is(notNullValue()));

        verifyNoMoreInteractions(parent, child);
    }

    @Test(dependsOnGroups = "sanity.system", dependsOnMethods = "registerChildActor")
    public final void registeredChildActorContext(@Injectable final Actor<?> parent,
                                                  @Injectable final Actor<?> child) {
        final Context parentContext = Captured.as(Context.class);
        final Context childContext = Captured.as(Context.class);

        new StrictExpectations() {{
            final Reference<?> parentReference = new Reference<>(
                    withArgThat(is(Captured.into(parentContext))),
                    parent, (Reference.Callback)
                    any
            );
            parentReference.start(mExecutor);
            result = true;
            final Reference<?> childReference = new Reference<>(
                    withArgThat(is(Captured.into(childContext))),
                    child,
                    (Reference.Callback) any
            );
            childReference.start(mExecutor);
            result = true;
        }};

        final String parentName = isA(RandomName);
        assertThat("parent reference", mSystem.register(parentName, parent), is(notNullValue()));
        final String childName = isA(RandomName);
        assertThat("child reference", parentContext.register(childName, child), is(notNullValue()));

        assertThat("child context", childContext,
                hasName(new Actor.Name(new Actor.Name(null, parentName), childName)));
        assertThat("child context system", childContext.getSystem(), is(notNullValue()));

        verifyNoMoreInteractions(parent, child);
    }

    @Test(dependsOnGroups = "sanity.system", dependsOnMethods = "registerChildActor")
    public final void stopParentActorWithChildren(@Injectable final Actor<?> parent,
                                                  @Injectable final Actor<?> child) {
        final Context context = Captured.as(Context.class);
        final Reference.Callback callback = Captured.as(Reference.Callback.class);

        new StrictExpectations() {{
            final Reference<?> parentReference = new Reference<>(
                    withArgThat(is(Captured.into(context))),
                    parent,
                    withArgThat(is(Captured.into(callback)))
            );
            parentReference.start(mExecutor);
            result = true;
            final Reference<?> childReference =
                    new Reference<>((Context) any, child, (Reference.Callback) any);
            childReference.start(mExecutor);
            result = true;
            childReference.stop(true);
            result = true;
        }};

        final Reference<?> parenReference = mSystem.register(isA(RandomName), parent);
        assertThat("parent reference", parenReference, is(notNullValue()));
        assertThat("child reference", context.register(isA(RandomName), child), is(notNullValue()));
        callback.onStop(context.getName());

        verifyNoMoreInteractions(parent, child);
    }

    @Test(dependsOnGroups = "sanity.system")
    public final void doubleStart() {
        new StrictExpectations() {{
            final Reference<?> reference =
                    new Reference<>((Context) any, mActor, (Reference.Callback) any);
            reference.start(mExecutor);
            result = true;
        }};

        final Reference<?> reference = mSystem.register(a(RandomName), mActor);
        assertThat("actor reference", reference, is(notNullValue()));

        assertThat("system start", mSystem.start(), is(true));
        assertThat("system", mSystem, is(started()));
    }

    @Test(dependsOnGroups = "sanity.system",
            dataProvider = "success or failure", dataProviderClass = Providers.class)
    public final void pause(final Providers.Boolean success) {
        new StrictExpectations() {{
            final Reference<?> reference =
                    new Reference<>((Context) any, mActor, (Reference.Callback) any);
            reference.start(mExecutor);
            result = true;
            reference.pause();
            result = success.value();
        }};

        final Reference<?> reference = mSystem.register(a(RandomName), mActor);
        assertThat("actor reference", reference, is(notNullValue()));

        assertThat("system pause", mSystem.pause(), is(success.value()));
        assertThat("system", mSystem, is(paused()));
    }

    @Test(dependsOnGroups = "sanity.system", dependsOnMethods = "pause")
    public final void doublePause() {
        new StrictExpectations() {{
            final Reference<?> reference =
                    new Reference<>((Context) any, mActor, (Reference.Callback) any);
            reference.start(mExecutor);
            result = true;
            reference.pause();
            result = true;
        }};

        final Reference<?> reference = mSystem.register(a(RandomName), mActor);
        assertThat("actor reference", reference, is(notNullValue()));

        assertThat("system pause", mSystem.pause(), is(true));
        assertThat("system pause", mSystem.pause(), is(true));
        assertThat("system", mSystem, is(paused()));
    }

    @Test(dependsOnGroups = "sanity.system")
    public final void registerActorWhenPaused() {
        assertThat("system pause", mSystem.pause(), is(true));
        assertThat("system", mSystem, is(paused()));

        new StrictExpectations() {{
            new Reference<>((Context) any, mActor, (Reference.Callback) any);
        }};

        final Reference<?> reference = mSystem.register(a(RandomName), mActor);
        assertThat("actor reference", reference, is(notNullValue()));
    }

    @Test(dependsOnGroups = "sanity.system",
            dataProvider = "success or failure", dataProviderClass = Providers.class)
    public final void startAfterPause(final Providers.Boolean started) {
        new StrictExpectations() {{
            final Reference<?> reference =
                    new Reference<>((Context) any, mActor, (Reference.Callback) any);
            reference.start(mExecutor);
            result = true;
            reference.pause();
            result = true;
            reference.start(mExecutor);
            result = started.value();
        }};

        final Reference<?> reference = mSystem.register(a(RandomName), mActor);
        assertThat("actor reference", reference, is(notNullValue()));

        assertThat("system pause", mSystem.pause(), is(true));
        assertThat("system start", mSystem.start(), is(started.value()));
        assertThat("system", mSystem, is(started()));
    }

    @Test(dependsOnGroups = "sanity.system", dataProvider = "(success, immediateness)")
    public final void stop(final Providers.Boolean success,
                           final Providers.Boolean immediately) {
        new StrictExpectations() {{
            final Reference<?> reference =
                    new Reference<>((Context) any, mActor, (Reference.Callback) any);
            reference.start(mExecutor);
            result = true;
            reference.stop(immediately.value());
            result = success.value();
        }};

        final Reference<?> reference = mSystem.register(a(RandomName), mActor);
        assertThat("actor reference", reference, is(notNullValue()));

        assertThat("system stop", mSystem.stop(immediately.value()), is(success.value()));
        assertThat("system", mSystem, is(stopped()));
    }

    @Test(dependsOnGroups = "sanity.system", dependsOnMethods = "stop",
            dataProvider = "immediateness")
    public final void doubleStop(final Providers.Boolean immediately) {
        new StrictExpectations() {{
            final Reference<?> reference =
                    new Reference<>((Context) any, mActor, (Reference.Callback) any);
            reference.start(mExecutor);
            result = true;
            reference.stop(immediately.value());
            result = true;
        }};

        final Reference<?> reference = mSystem.register(a(RandomName), mActor);
        assertThat("actor reference", reference, is(notNullValue()));

        assertThat("system stop", mSystem.stop(immediately.value()), is(true));
        assertThat("system stop", mSystem.stop(immediately.value()), is(true));
        assertThat("system", mSystem, is(stopped()));
    }

    @Test(dependsOnGroups = "sanity.system", dependsOnMethods = "stop",
            dataProvider = "immediateness",
            expectedExceptions = UnsupportedOperationException.class,
            expectedExceptionsMessageRegExp = SYSTEM_STOPPED)
    public final void registerActorAfterStop(final Providers.Boolean immediately) {
        new StrictExpectations() {{
            new Reference<>((Context) any, (Actor<?>) any, (Reference.Callback) any);
            maxTimes = 0;
        }};

        assertThat("system stop", mSystem.stop(immediately.value()), is(true));
        mSystem.register(a(RandomName), mActor);
    }

    @Test(dependsOnGroups = "sanity.system", dependsOnMethods = "stop",
            dataProvider = "immediateness",
            expectedExceptions = UnsupportedOperationException.class,
            expectedExceptionsMessageRegExp = SYSTEM_STOPPED)
    public final void startAfterStop(final Providers.Boolean immediately) {
        assertThat("system stop", mSystem.stop(immediately.value()), is(true));
        mSystem.start();
    }

    @Test(dependsOnGroups = "sanity.system", dependsOnMethods = "stop",
            dataProvider = "immediateness",
            expectedExceptions = UnsupportedOperationException.class,
            expectedExceptionsMessageRegExp = SYSTEM_STOPPED)
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
}
