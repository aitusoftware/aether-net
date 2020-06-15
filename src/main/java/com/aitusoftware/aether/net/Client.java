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
package com.aitusoftware.aether.net;

import com.aitusoftware.aether.Aether;
import com.aitusoftware.aether.transport.CounterSnapshotPublisher;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import org.agrona.SystemUtil;
import org.agrona.concurrent.ShutdownSignalBarrier;
import org.agrona.concurrent.SleepingMillisIdleStrategy;

public final class Client
{
    public static void main(final String[] args)
    {
        SystemUtil.loadPropertiesFiles(args);
        try (
            MediaDriver mediaDriver = MediaDriver.launchEmbedded(
                new MediaDriver.Context()
                .sharedIdleStrategy(new SleepingMillisIdleStrategy(1))
                .threadingMode(ThreadingMode.SHARED));
            CounterSnapshotPublisher snapshotPublisher =
                new CounterSnapshotPublisher(
                new CounterSnapshotPublisher.Context()
                .aeronDirectoryName(mediaDriver.aeronDirectoryName()));
            Aether aether = Aether.launch(new Aether.Context()
                .counterSnapshotListener(snapshotPublisher)
                .aeronDirectoryName(mediaDriver.aeronDirectoryName())
                .threadingMode(Aether.ThreadingMode.THREADED)))
        {
            new ShutdownSignalBarrier().await();
        }
    }
}