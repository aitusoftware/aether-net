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

import com.aitusoftware.aether.model.PublisherCounterSet;

import java.util.*;

public final class PublisherData implements Comparable<PublisherData>
{
    private final String label;
    private final String channel;
    private final int streamId;
    private final int sessionId;
    private final long publisherPosition;
    private final long publisherLimit;
    private final long senderPosition;
    private final long senderLimit;
    private final long backPressureEvents;
    private final long sendBacklog;
    private final long remainingBuffer;
    private final Set<SubscriberData> subscribers = new TreeSet<>();
    private final Map<String, Long> publishRates = new HashMap<>();

    public PublisherData(final String label, final PublisherCounterSet counterSet)
    {
        this.label = label;
        this.channel = counterSet.channel().toString();
        this.streamId = counterSet.streamId();
        this.sessionId = counterSet.sessionId();
        this.publisherPosition = counterSet.publisherPosition();
        this.publisherLimit = counterSet.publisherLimit();
        this.senderPosition = counterSet.senderPosition();
        this.senderLimit = counterSet.senderLimit();
        this.backPressureEvents = counterSet.backPressureEvents();
        this.remainingBuffer = publisherLimit - publisherPosition;
        this.sendBacklog = Math.max(0, publisherPosition - senderPosition);
    }

    public void addSubscriberData(final SubscriberData subscriberData)
    {
        subscribers.add(subscriberData);
    }

    public Set<SubscriberData> getSubscribers()
    {
        return subscribers;
    }

    public String getLabel()
    {
        return label;
    }

    public String getChannel()
    {
        return channel;
    }

    public int getStreamId()
    {
        return streamId;
    }

    public int getSessionId()
    {
        return sessionId;
    }

    public long remainingBuffer()
    {
        return remainingBuffer;
    }

    public long sendBacklog()
    {
        return sendBacklog;
    }

    public void addPublishRate(final String key, final long value)
    {
        publishRates.put(key, value);
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
        final PublisherData that = (PublisherData)o;
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
    public int compareTo(final PublisherData publisherData)
    {
        int fieldDiff = channel.compareTo(publisherData.channel);
        if (fieldDiff == 0)
        {
            fieldDiff = label.compareTo(publisherData.label);
        }
        if (fieldDiff == 0)
        {
            fieldDiff = Integer.compare(streamId, publisherData.streamId);
        }
        if (fieldDiff == 0)
        {
            fieldDiff = Integer.compare(sessionId, publisherData.sessionId);
        }
        return fieldDiff;
    }
}