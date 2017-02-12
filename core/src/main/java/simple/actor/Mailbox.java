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

import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * A {@link Channel} that delegates all calls to the connected {@code Channel} or if no {@code
 * Channel} is connected, stores messages and resends them when a {@code Channel} is connected.
 *
 * <p>Mailbox can be in two modes: connected and disconnected. The connected mode is when mailbox is
 * connected to a non-{@code null} {@code Channel} that was either passed in through the {@link
 * #Mailbox(Channel) constructor} or the {@link #connect(Channel) connect} method. Otherwise,
 * mailbox is the disconnected mode (i.e. it is not connected to any {@code Channel}).
 *
 * <p>In connected mode, the mailbox will simply delegate all calls to the connected {@code
 * Channel}. This includes both {@link #send sending} messages and {@link #stop stopping} the
 * mailbox.
 *
 * <p>In disconnected mode, the mailbox will store all messages until stopped or a {@code Channel}
 * is connected. Once a {@code Channel} is connected, it will resend all stored messages in the same
 * order they were received. If the mailbox was stopped, it will also stop the {@code Channel}.
 *
 * @param <M> the type of sent messages.
 */
public final class Mailbox<M> implements Channel<M> {

    private final Object mLock = new Object();
    @GuardedBy("mLock")
    private final Queue<M> mPending = new ArrayDeque<>();

    @Nullable
    @GuardedBy("mLock")
    private Channel<M> mChannel;

    @GuardedBy("mLock")
    private boolean mStopped = false;

    /** Creates a mailbox in disconnected mode. */
    public Mailbox() {
        this(null);
    }

    /**
     * Creates a mailbox that is connected to the given {@link Channel}.
     *
     * @param channel the connected {@code Channel}.
     */
    public Mailbox(@Nullable final Channel<M> channel) {
        mChannel = channel;
    }

    /**
     * Connects the mailbox to the given {@link Channel}.
     *
     * <p>At this moment, the mailbox will resend any stored messages in the same order they were
     * received. If the mailbox was stopped, it will also stop the {@code Channel}.
     *
     * @param channel the {@code Channel} to connect to.
     */
    public void connect(final Channel<M> channel) {
        synchronized (mLock) {
            while (!mPending.isEmpty()) {
                channel.send(mPending.poll());
            }

            if (mStopped) {
                channel.stop();
            } else {
                mChannel = channel;
            }
        }
    }

    /**
     * Disconnects the mailbox from any connected {@link Channel}.
     *
     * <p>From this moment, the mailbox will store all messages until stopped or a {@code Channel}
     * is connected.
     */
    public void disconnect() {
        synchronized (mLock) {
            mChannel = null;
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>In connected mode, the mailbox will simply delegate the sending to the connected {@link
     * Channel}. In disconnected mode, the mailbox will store all messages unless it was previously
     * stopped.
     */
    @Override
    public boolean send(final M message) {
        synchronized (mLock) {
            return (mChannel == null)
                    ? (!mStopped && mPending.add(message))
                    : mChannel.send(message);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>In connected mode, the mailbox will stop the connected {@link Channel}. In disconnected
     * mode, it will remember that it was stopped so that it knows to stop any {@code Channel
     * Channels} that get connected.
     */
    @Override
    public void stop() {
        synchronized (mLock) {
            if (!mStopped) {
                if (mChannel != null) {
                    mChannel.stop();
                    mChannel = null;
                }
                mStopped = true;
            }
        }
    }
}
