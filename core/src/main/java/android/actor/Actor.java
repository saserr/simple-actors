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

import org.jetbrains.annotations.NonNls;

public abstract class Actor<M> {

    protected abstract void onMessage(@NonNull final M m);

    protected void postStart(@NonNull final Context context,
                             @NonNull final Reference<M> self) {/* do nothing */}

    protected void preStop() {/* do nothing */}

    public interface Repository {
        @NonNull
        <M> Reference<M> with(@NonNls @NonNull final String name, @NonNull final Actor<M> actor);
    }

    public static class Name {

        @Nullable
        private final Name mParent;
        @NonNls
        @NonNull
        private final String mPart;

        public Name(@Nullable final Name parent, @NonNls @NonNull final String part) {
            super();

            if (part.indexOf('.') >= 0) {
                throw new IllegalArgumentException("Name part cannot contain '.' character");
            }

            mParent = parent;
            mPart = part;
        }

        public final boolean isParentOf(@NonNull final Name other) {
            Name parent = other.mParent;

            while ((parent != null) && !equals(parent)) {
                parent = parent.mParent;
            }

            return parent != null;
        }

        @Override
        public final int hashCode() {
            return (31 * ((mParent == null) ? 0 : mParent.hashCode())) + mPart.hashCode();
        }

        @Override
        public final boolean equals(@Nullable final Object obj) {
            boolean result = this == obj;

            if (!result && (obj instanceof Name)) {
                final Name other = (Name) obj;
                result = mPart.equals(other.mPart) &&
                        ((mParent == null) ? (other.mParent == null) : mParent.equals(other.mParent));
            }

            return result;
        }

        @NonNls
        @NonNull
        @Override
        public final String toString() {
            return (mParent == null) ? mPart : (mParent.toString() + '.' + mPart);
        }
    }
}
