/*
 * Copyright 2019-2020 Aitu Software Limited.
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
package com.aitusoftware.aether.net.util;

import com.aitusoftware.aether.event.CounterSnapshotListener;
import com.aitusoftware.aether.model.PublisherCounterSet;
import com.aitusoftware.aether.model.SubscriberCounterSet;
import com.aitusoftware.aether.model.SystemCounters;

import java.util.List;

public final class AggregateUpdateListener implements CounterSnapshotListener
{
    private final CounterSnapshotListener[] delegates;

    public AggregateUpdateListener(final CounterSnapshotListener... delegates)
    {
        this.delegates = delegates;
    }

    @Override
    public void onSnapshot(
        final String label, final long timestamp,
        final List<PublisherCounterSet> publisherCounters,
        final List<SubscriberCounterSet> subscriberCounters,
        final SystemCounters systemCounters)
    {
        for (final CounterSnapshotListener delegate : delegates)
        {
            delegate.onSnapshot(label, timestamp, publisherCounters, subscriberCounters, systemCounters);
        }
    }
}
