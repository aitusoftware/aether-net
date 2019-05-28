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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import com.aitusoftware.aether.Aether;
import com.aitusoftware.aether.transport.CounterSnapshotPublisher;
import com.google.gson.Gson;

import org.agrona.CloseHelper;
import org.agrona.concurrent.SleepingMillisIdleStrategy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.aeron.Aeron;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;

class IntegrationTest
{
    private final Gson gson = new Gson();
    private MediaDriver driver;
    private Closeable server;
    private CounterSnapshotPublisher snapshotPublisher;
    private Aether aether;
    private Aeron aeron;
    private StubSystem stubSystem;

    @BeforeEach
    void setUp()
    {
        stubSystem = new StubSystem();
        driver = MediaDriver.launchEmbedded(new MediaDriver.Context()
            .threadingMode(ThreadingMode.SHARED)
            .sharedIdleStrategy(new SleepingMillisIdleStrategy(1L)));
        server = Server.launchServer(new Context().mode(Mode.NETWORK));
        snapshotPublisher = new CounterSnapshotPublisher(
            new CounterSnapshotPublisher.Context()
                .aeronDirectoryName(driver.aeronDirectoryName()));
        aether = Aether.launch(new Aether.Context()
            .monitoringLocations(stubSystem.monitoringLocations())
            .counterSnapshotListener(snapshotPublisher)
            .aeronDirectoryName(driver.aeronDirectoryName())
            .threadingMode(Aether.ThreadingMode.THREADED));
        aeron = Aeron.connect(new Aeron.Context().aeronDirectoryName(driver.aeronDirectoryName()));
        stubSystem.start();
    }

    @Test
    void shouldReceiveCounterUpdates()
    {
        waitForPort(Server.HTTP_PORT);
        aether.doWork();
        final long failAt = System.currentTimeMillis() + 5_000L;
        Map data = new HashMap();
        while (System.currentTimeMillis() < failAt)
        {
            data = getData();
            if (!data.isEmpty())
            {
                if (data.containsKey("streams"))
                {
                    final Map streamData = (Map)data.get("streams");
                    if (streamData.containsKey(StubSystem.CHANNEL_A))
                    {
                        final Map streamMap = (Map)streamData.get(StubSystem.CHANNEL_A);
                        if (streamMap.containsKey(Integer.toString(StubSystem.STREAM_ID)))
                        {
                            final List subscriberList = (List)streamMap.get(Integer.toString(StubSystem.STREAM_ID));
                            if (!subscriberList.isEmpty())
                            {
                                break;
                            }
                        }
                    }
                }
            }
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(100L));
        }
        assertThat(System.currentTimeMillis()).isLessThan(failAt);
        assertThat(data.isEmpty()).isFalse();

        assertStreamData((Map)data.get("streams"));
    }

    private void assertStreamData(final Map data)
    {
        assertThat(data.containsKey(StubSystem.CHANNEL_A));
        final Map streamMap = (Map)data.get(StubSystem.CHANNEL_A);
        final List subscriberList = (List)streamMap.get(Integer.toString(StubSystem.STREAM_ID));
        assertThat(subscriberList.isEmpty()).isFalse();
        final Map publisherData = (Map)subscriberList.get(0);
        assertThat(publisherData.containsKey("channel"));
        assertThat(publisherData.containsKey("streamId"));
        assertThat(publisherData.containsKey("publisherPosition"));
    }

    private Map getData()
    {
        try
        {
            final HttpURLConnection urlConnection =
                (HttpURLConnection)new URL(
                "http://localhost:" + Server.HTTP_PORT + "/data.json").openConnection();
            return gson.fromJson(new InputStreamReader(urlConnection.getInputStream()), Map.class);
        }
        catch (final IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    private void waitForPort(final int httpPort)
    {
        final long failAt = System.currentTimeMillis() + 5_000L;
        while (failAt > System.currentTimeMillis())
        {
            try (Socket socket = new Socket())
            {
                socket.connect(new InetSocketAddress("localhost", httpPort), 1000);
                return;
            }
            catch (final IOException e)
            {
                // ignore
            }
        }
        fail();
    }

    @AfterEach
    void tearDown()
    {
        CloseHelper.close(stubSystem);
        CloseHelper.close(aeron);
        CloseHelper.close(snapshotPublisher);
        CloseHelper.close(aether);
        CloseHelper.close(server);
        CloseHelper.close(driver);
    }
}