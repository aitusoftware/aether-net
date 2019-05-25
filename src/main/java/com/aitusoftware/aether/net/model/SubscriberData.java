/*
 * Copyright 2019 Aitu Software Limited.
 *
 * https://aitusoftware.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.aitusoftware.aether.net.model;

import java.util.Map;
import java.util.Objects;

import com.aitusoftware.aether.model.SubscriberCounterSet;

public final class SubscriberData implements Comparable<SubscriberData>
{
    private final String label;
    private final String channel;
    private final int streamId;
    private final int sessionId;
    private final Map<Long, Long> subscriberPositions;
    private final long receiverPosition;
    private final long receiverHighWaterMark;

    public SubscriberData(final String label, final SubscriberCounterSet counterSet)
    {
        this.label = label;
        this.channel = counterSet.channel().toString();
        this.streamId = counterSet.streamId();
        this.sessionId = counterSet.sessionId();
        this.subscriberPositions = counterSet.subscriberPositions();
        this.receiverPosition = counterSet.receiverPosition();
        this.receiverHighWaterMark = counterSet.receiverHighWaterMark();
    }

    @Override
    public boolean equals(final Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        final SubscriberData that = (SubscriberData)o;
        return streamId == that.streamId &&
            sessionId == that.sessionId &&
            channel.equals(that.channel) &&
            label.equals(that.label);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(label, channel, streamId, sessionId);
    }

    @Override
    public int compareTo(final SubscriberData subscriberData)
    {
        int fieldDiff = channel.compareTo(subscriberData.channel);
        if (fieldDiff == 0)
        {
            fieldDiff = label.compareTo(subscriberData.label);
        }
        if (fieldDiff == 0)
        {
            fieldDiff = Integer.compare(streamId, subscriberData.streamId);
        }
        if (fieldDiff == 0)
        {
            fieldDiff = Integer.compare(sessionId, subscriberData.sessionId);
        }
        return fieldDiff;
    }
}