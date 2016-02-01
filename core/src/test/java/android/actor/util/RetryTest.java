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

package android.actor.util;

import android.actor.Providers;
import android.actor.TestCase;
import android.support.annotation.NonNull;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import mockit.Injectable;
import mockit.StrictExpectations;

import static android.actor.Providers.provider;
import static android.actor.Providers.value;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

public class RetryTest extends TestCase {

    private static final int ONCE = 1;

    @Injectable
    private Retry.Action mAction;

    private Retry mRetry;

    @BeforeMethod
    public final void setUp() {
        mRetry = new Retry(mAction);
    }

    @AfterMethod
    public final void tearDown() {
        verifyNoMoreInteractions(mAction);
    }

    @Test(groups = {"sanity", "sanity.retry"}, dataProvider = "results")
    public final void runWithoutRetry(final Providers.Value<Integer> retry) {
        new StrictExpectations() {{
            mAction.execute();
            result = retry.value();
        }};

        if (retry.value() == Retry.AGAIN) {
            new StrictExpectations() {{
                mAction.onNoMoreRetries();
            }};
        }

        assertThat("retry", mRetry.run(ONCE), is(retry.value()));
    }

    @Test(groups = {"sanity", "sanity.retry"},
            dataProvider = "success or failure", dataProviderClass = Providers.class)
    public final void runWithRetry(final Providers.Boolean success) {
        final int moreThanOnce = isA(RandomInteger.thatIs(greaterThan(ONCE)));

        new StrictExpectations() {{
            mAction.execute();
            result = Retry.AGAIN;
            mAction.onRetry(moreThanOnce - 1);
            mAction.execute();
            result = success.value() ? Retry.SUCCESS : Retry.FAILURE;
        }};

        final int result = success.value() ? Retry.SUCCESS : Retry.FAILURE;
        assertThat("retry", mRetry.run(moreThanOnce), is(result));
    }

    @Test(dependsOnGroups = "sanity.retry",
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = "Tries must be positive number!")
    public final void runWithNonPositiveNumberOfTries() {
        final Integer invalidTries = isA(RandomInteger.thatIs(lessThanOrEqualTo(0)));
        mRetry.run(invalidTries);
    }

    @NonNull
    @DataProvider(name = "results")
    private static Object[][] delivery() {
        return provider(
                value("success", Retry.SUCCESS),
                value("again", Retry.AGAIN),
                value("failure", Retry.FAILURE)
        );
    }
}
