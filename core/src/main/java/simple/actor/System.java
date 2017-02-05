/*
 * Copyright 2017 Sanjin Sehic
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

package simple.actor;

import net.jcip.annotations.GuardedBy;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Executor;

import static simple.actor.Checks.checkNotNull;

/**
 * A group of {@link Actor Actors} which use a common {@link Runner} to {@link Actor#onMessage
 * deliver} messages.
 *
 * <p>System can be in three modes: {@link #resume running}, {@link #pause paused}, and {@link #stop
 * stopped}. All system are {@link #System created} in the running mode.
 *
 * <p>In running mode, the system will immediately deliver messages to registered {@code Actors}
 * using the common {@link Runner}.
 *
 * <p>In paused mode, the system will allow sending of messages to registered {@code Actors}, but
 * they wont be delivered and instead will be stored in-memory until system is {@link #resume
 * resumed}.
 *
 * <p>In stopped mode, the system will stop all registered {@code Actors}, which will cause any
 * future sending of messages to fail. Furthermore, any attempt to {@link #register register} an
 * {@code Actor} will throw an {@link IllegalStateException}.
 */
public final class System implements Context {

    final Object mLock = new Object();
    @GuardedBy("mLock")
    private final Node<?, ActorChannel<?>> mHead = new Node<>(
            null, null, null);

    @Nullable
    @GuardedBy("mLock")
    private Runner mRunner;
    @GuardedBy("mLock")
    private boolean mPaused = false;

    /**
     * Creates a running system that will use given {@link Runner} to deliver messages to {@link
     * Actor Actors}.
     *
     * @param runner the {@code Runner} that will be used to deliver messages to {@code Actors}.
     */
    public System(final Runner runner) {
        mRunner = runner;
        mHead.mNext = mHead;
        mHead.mPrevious = mHead;
    }

    /**
     * {@inheritDoc}
     *
     * <p>If system is {@link #resume running}, the {@link Actor} will be asynchronously {@link
     * Actor#onStart started}. If system is {@link #pause paused}, the {@code Actor} will be started
     * once the system is {@link #resume resumed}.
     *
     * @throws IllegalStateException if system is {@link #stop stopped}.
     */
    @Override
    public <M> Channel<M> register(final Actor<M> actor) {
        final ActorChannel<M> channel;
        synchronized (mLock) {
            if (mRunner == null) {
                throw new IllegalStateException("System is stopped");
            }

            final Node<?, ActorChannel<?>> last = checkNotNull(mHead.mPrevious, "last");
            final Node<Runnable, ActorChannel<?>> node = new Node<>(mRunner.create(), last, mHead);
            last.mNext = node;
            mHead.mPrevious = node;
            channel = new ActorChannel<>(actor, node, mPaused);
            node.set(channel);
        }

        channel.execute(() -> actor.onStart(channel, this));
        return channel;
    }

    /**
     * Pauses {@link Actor#onMessage delivery} of messages to all {@link Actor Actors} registered
     * with the system. Clients can still send messages to registered {@code Actors}, but they wont
     * be delivered until system is {@link #resume resumed}.
     */
    public void pause() {
        synchronized (mLock) {
            if (!mPaused) {
                Node<?, ActorChannel<?>> channel = checkNotNull(mHead.mNext, "first");
                while (channel != mHead) {
                    channel.get().pause();
                    channel = checkNotNull(channel.mNext, "next");
                }
                mPaused = true;
            }
        }
    }

    /**
     * Resumes {@link Actor#onMessage delivery} of messages to all {@link Actor Actors} registered
     * with the system. At this moment, all messages that were sent to registered {@code Actors}
     * while the system was {@link #pause paused} will be delivered.
     */
    public void resume() {
        synchronized (mLock) {
            if (mPaused) {
                Node<?, ActorChannel<?>> channel = checkNotNull(mHead.mNext, "first");
                while (channel != mHead) {
                    final Node<?, ActorChannel<?>> next = checkNotNull(channel.mNext, "next");
                    channel.get().resume();
                    channel = next;
                }
                mPaused = false;
            }
        }
    }

    /**
     * Stops all {@link Actor Actors} registered with the system. After this, sending of messages to
     * any of the registered {@code Actors} will fail. Furthermore, any attempt to register an
     * {@code Actor} will throw an {@link IllegalStateException}.
     *
     * <p>If system is {@link #pause paused}, pending messages will still be {@link Actor#onMessage
     * delivered} to its {@code Actors} once the system is {@link #resume resumed}.
     */
    public void stop() {
        synchronized (mLock) {
            Node<?, ActorChannel<?>> channel = checkNotNull(mHead.mNext, "first");
            while (channel != mHead) {
                final Node<?, ActorChannel<?>> next = checkNotNull(channel.mNext, "next");
                channel.get().stop();
                channel = next;
            }
            mRunner = null;
        }
    }

    /**
     * A pausable {@link Channel} that {@link Actor#onMessage delivers} messages to an {@link Actor}
     * using a {@link Runner#create runnable channel}.
     *
     * <p>{@code Channel} can be in three modes: {@link #resume running}, {@link #pause paused}, and
     * {@link #stop stopped}.
     *
     * <p>In running mode, the {@code Channel} will immediately deliver messages to the {@code
     * Actor} using the runnable channel.
     *
     * <p>In paused mode, the {@code Channel} will allow {@link #send sending} of messages to the
     * {@code Actor}, but they wont be delivered and instead will be stored in-memory until {@code
     * Channel} is {@link #resume resumed}.
     *
     * <p>In stopped mode, the {@code Channel} will prevent any future sending of message to the
     * {@code Actor} and will cause it to fail. However, all messages that were sent before the
     * {@code Channel} was stopped will still be delivered to the {@code Actor}.
     *
     * @param <M> the type of sent messages.
     */
    private static final class ActorChannel<M> implements Channel<M>, Executor {

        private final Actor<M> mActor;
        private final Channel<Runnable> mChannel;
        private final Mailbox<Runnable> mMailbox;

        /**
         * Creates a pausable {@link Channel} that will use given {@link Runner#create runnable
         * channel} to {@link Actor#onMessage deliver} messages to given {@link Actor}.
         *
         * @param actor   the {@code Actor} that will receive all sent messages.
         * @param channel the runnable channel that will be used to deliver messages to the {@code
         *                Actor}.
         * @param paused  if {@code Channel} is created in {@link #pause paused} or {@link #resume
         *                running} state.
         */
        ActorChannel(final Actor<M> actor,
                     final Channel<Runnable> channel,
                     final boolean paused) {
            mActor = actor;
            mChannel = channel;
            mMailbox = new Mailbox<>(paused ? null : channel);
        }

        /**
         * {@inheritDoc}
         *
         * <p>The message will be {@link Actor#onMessage delivered} using the {@link Runner#create
         * runnable channel} unless {@link #pause paused}. If {@link Channel} is {@link #pause
         * paused}, the message will be stored in-memory and delivered to the {@link Actor} once the
         * {@code Channel} is {@link #resume resumed}. If {@code Channel} is {@link #stop stopped},
         * the message will not delivered and the {@code false} value will be returned.
         *
         * <p>If message delivery using the runnable channel fails, {@code Channel} will be
         * immediately {@link #stop stopped} and removed from the {@link System}.
         */
        @Override
        public boolean send(final M message) {
            final boolean success = mMailbox.send(() -> mActor.onMessage(message));
            if (!success) {
                stop();
            }
            return success;
        }

        /**
         * {@inheritDoc}
         *
         * <p>If {@link Channel} is {@link #pause paused}, pending messages will still be {@link
         * Actor#onMessage delivered} once @code Channel} is {@link #resume resumed}.
         */
        @Override
        public void stop() {
            mMailbox.stop();
        }

        /**
         * Executes the given {@link Runnable task} using the {@link Runner#create runnable
         * channel}.
         *
         * @param command the task.
         */
        @Override
        public void execute(@NotNull final Runnable command) {
            mMailbox.send(command);
        }

        /**
         * Pauses {@link Actor#onMessage delivery} of messages to the {@link Actor}. Clients can
         * still {@link #send send} messages, but they wont be delivered until {@link Channel} is
         * {@link #resume resumed}.
         */
        void pause() {
            mMailbox.disconnect();
        }

        /**
         * Resumes {@link Actor#onMessage delivery} of messages to th {@link Actor}. At this moment,
         * all messages that were {@link #send sent} and stored in-memory while {@link Channel} was
         * {@link #pause paused} will be passed to the {@link Runner#create runnable channel} and
         * delivered to the {@code Actor}.
         */
        void resume() {
            mMailbox.connect(mChannel);
        }
    }

    /**
     * A {@link Channel} that delegates all its calls to a delegate {@link Channel} and is also a
     * node in a doubly linked list. When {@link Channel} is {@link #stop stopped}, the node removes
     * itself from the linked list.
     *
     * @param <M> the type of sent messages.
     * @param <V> the type of elements held in the linked list.
     */
    private final class Node<M, V> implements Channel<M> {

        private final Channel<M> mChannel;

        @Nullable
        private V mValue;

        @Nullable
        @GuardedBy("mLock")
        Node<?, V> mPrevious;
        @Nullable
        @GuardedBy("mLock")
        Node<?, V> mNext;

        /**
         * Creates a {@link Channel} that will delegate all its calls to the given {@code Channel}
         * and is situated between the specified previous and next nodes in the linked list.
         *
         * @param channel  the delegate {@code Channel}.
         * @param previous the previous node in the linked list.
         * @param next     the next node in the linked list.
         */
        Node(final Channel<M> channel,
             @Nullable final Node<?, V> previous,
             @Nullable final Node<?, V> next) {
            mChannel = channel;
            mPrevious = previous;
            mNext = next;
        }

        @Override
        public boolean send(final M message) {
            return mChannel.send(message);
        }

        /**
         * {@inheritDoc}
         *
         * <p>Additionally, the node will be removed from the linked list.
         */
        @Override
        public void stop() {
            remove();
            mChannel.stop();
        }

        /** Sets the value of this node. */
        void set(final V value) {
            mValue = value;
        }

        /** Returns the value of this node. */
        V get() {
            return checkNotNull(mValue, "value");
        }

        /**
         * Removes this node from the linked list by connecting the previous and next nodes to each
         * other.
         */
        private void remove() {
            synchronized (mLock) {
                if (mPrevious != null) {
                    mPrevious.mNext = mNext;
                }
                if (mNext != null) {
                    mNext.mPrevious = mPrevious;
                }

                mValue = null;
                mPrevious = null;
                mNext = null;
            }
        }
    }
}
