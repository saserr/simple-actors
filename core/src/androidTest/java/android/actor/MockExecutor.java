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
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import static android.os.Looper.myLooper;
import static java.util.Collections.unmodifiableList;

public class MockExecutor implements Executor {

    private final List<Submission> mSubmissions = new ArrayList<>(1);

    private boolean mStopped = false;

    public MockExecutor() {
        super();
    }

    public final boolean isStopped() {
        return mStopped;
    }

    @NonNull
    public final List<Submission> getSubmissions() {
        return unmodifiableList(mSubmissions);
    }

    @Nullable
    @Override
    public final Submission submit(@NonNull final Executor.Task task) {
        final Submission submission = new Submission(task);
        mSubmissions.add(submission);
        return submission;
    }

    @Override
    public final void stop() {
        mStopped = true;
    }

    public static class Submission implements Executor.Submission {

        @NonNull
        private final Executor.Task mTask;

        private boolean mStopped = false;

        public Submission(@NonNull final Executor.Task task) {
            super();

            mTask = task;
            mTask.attach(myLooper());
        }

        public final boolean isStopped() {
            return mStopped;
        }

        @NonNull
        public final Executor.Task getTask() {
            return mTask;
        }

        @Override
        public final boolean stop() {
            mTask.detach();
            mStopped = true;
            return true;
        }
    }
}
