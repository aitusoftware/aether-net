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
package com.aitusoftware.aether.net;

import com.aitusoftware.aether.Aether;
import io.aeron.Aeron;
import io.aeron.FragmentAssembler;
import io.aeron.Publication;
import io.aeron.Subscription;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import org.agrona.CloseHelper;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.SleepingMillisIdleStrategy;
import org.agrona.concurrent.UnsafeBuffer;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static io.aeron.CommonContext.IPC_CHANNEL;

public final class StubSystem implements AutoCloseable
{
    static final String CHANNEL_A = "aeron:udp?endpoint=localhost:54567";
    private static final String CHANNEL_B = "aeron:udp?endpoint=localhost:54577";
    private static final String CHANNEL_C = "aeron:udp?endpoint=localhost:54587";
    static final int STREAM_ID = 37;
    private static final byte[] PAYLOAD = "Message Payload".getBytes(StandardCharsets.UTF_8);
    private static final int POLL_LIMIT = 16;
    private final MediaDriver clientDriver;
    private final MediaDriver serverDriver;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Aeron clientAeron;
    private final Aeron serverAeron;

    public StubSystem()
    {
        clientDriver = MediaDriver.launchEmbedded(new MediaDriver.Context()
            .threadingMode(ThreadingMode.SHARED)
            .sharedIdleStrategy(new SleepingMillisIdleStrategy(1L)));
        serverDriver = MediaDriver.launchEmbedded(new MediaDriver.Context()
            .threadingMode(ThreadingMode.SHARED)
            .sharedIdleStrategy(new SleepingMillisIdleStrategy(1L)));
        clientAeron = Aeron.connect(new Aeron.Context().aeronDirectoryName(
            clientDriver.aeronDirectoryName()));
        serverAeron = Aeron.connect(new Aeron.Context().aeronDirectoryName(
            serverDriver.aeronDirectoryName()));
    }

    public List<Aether.MonitoringLocation> monitoringLocations()
    {
        System.out.printf("monitoring client at %s%n", clientDriver.aeronDirectoryName());
        System.out.printf("monitoring server at %s%n", serverDriver.aeronDirectoryName());
        return Arrays.asList(
            new Aether.MonitoringLocation("client", clientDriver.aeronDirectoryName()),
            new Aether.MonitoringLocation("server", serverDriver.aeronDirectoryName()));
    }

    public void start()
    {
        executorService.submit(this::run);
    }

    public void run()
    {
        Thread.currentThread().setName("stub-system");
        final FragmentAssembler assembler = new FragmentAssembler((buffer, offset, length, header) ->
        {
            // ignored
        });
        try (
            Publication pubA = clientAeron.addPublication(CHANNEL_A, STREAM_ID);
            Publication pubB = clientAeron.addPublication(CHANNEL_B, STREAM_ID);
            Publication pubC = clientAeron.addPublication(CHANNEL_C, STREAM_ID);
            Publication ipcPub = clientAeron.addPublication(IPC_CHANNEL, STREAM_ID);
            Subscription subA_0 = serverAeron.addSubscription(CHANNEL_A, STREAM_ID);
            Subscription subA_1 = serverAeron.addSubscription(CHANNEL_A, STREAM_ID);
            Subscription subA_2 = serverAeron.addSubscription(CHANNEL_A, STREAM_ID);
            Subscription subB_0 = serverAeron.addSubscription(CHANNEL_B, STREAM_ID);
            Subscription subB_1 = serverAeron.addSubscription(CHANNEL_B, STREAM_ID);
            Subscription subC_0 = serverAeron.addSubscription(CHANNEL_C, STREAM_ID);
            Subscription subC_1 = serverAeron.addSubscription(CHANNEL_C, STREAM_ID);
            Subscription ipcSub = clientAeron.addSubscription(IPC_CHANNEL, STREAM_ID))
        {
            final Random random = ThreadLocalRandom.current();
            final DirectBuffer payload = new UnsafeBuffer(PAYLOAD);
            while (!Thread.currentThread().isInterrupted())
            {
                final int rnd = random.nextInt(10);
                if (rnd >= 0)
                {
                    pubA.offer(payload, 0, PAYLOAD.length);
                }
                if (rnd >= 1)
                {
                    pubB.offer(payload, 0, PAYLOAD.length);
                }
                if (rnd >= 2)
                {
                    pubC.offer(payload, 0, PAYLOAD.length);
                    ipcPub.offer(payload, 0, PAYLOAD.length);
                }
                if (rnd >= 3)
                {
                    subA_0.poll(assembler, random.nextInt(POLL_LIMIT));
                }
                if (rnd >= 4)
                {
                    subA_1.poll(assembler, random.nextInt(POLL_LIMIT));
                }
                if (rnd >= 5)
                {
                    subA_2.poll(assembler, random.nextInt(POLL_LIMIT));
                }
                if (rnd >= 6)
                {
                    subB_0.poll(assembler, random.nextInt(POLL_LIMIT));
                    for (int i = 0; i < 100; i++)
                    {
                        pubA.offer(payload, 0, PAYLOAD.length);
                    }
                }
                if (rnd >= 7)
                {
                    subB_1.poll(assembler, random.nextInt(POLL_LIMIT));
                    ipcSub.poll(assembler, random.nextInt(POLL_LIMIT));
                }
                if (rnd >= 8)
                {
                    subC_0.poll(assembler, random.nextInt(POLL_LIMIT));
                }
                if (rnd >= 9)
                {
                    subC_1.poll(assembler, random.nextInt(POLL_LIMIT));
                }

                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(100L));
            }
        }
        catch (final Exception e)
        {
            // ignore
        }
        finally
        {
            System.out.println("Stub system exit");
        }
    }

    @Override
    public void close() throws Exception
    {
        executorService.shutdownNow();
        executorService.awaitTermination(1, TimeUnit.SECONDS);
        CloseHelper.close(clientAeron);
        CloseHelper.close(serverAeron);
        CloseHelper.close(clientDriver);
        CloseHelper.close(serverDriver);
    }
}