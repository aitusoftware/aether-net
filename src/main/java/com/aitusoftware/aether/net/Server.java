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
import com.aitusoftware.aether.aggregation.RateBucket;
import com.aitusoftware.aether.aggregation.StreamRate;
import com.aitusoftware.aether.event.RateMonitor;
import com.aitusoftware.aether.event.StreamKey;
import com.aitusoftware.aether.event.SystemSnapshot;
import com.aitusoftware.aether.model.ChannelSessionKey;
import com.aitusoftware.aether.model.SubscriberCounterSet;
import com.aitusoftware.aether.net.model.PublisherData;
import com.aitusoftware.aether.net.model.SubscriberData;
import com.aitusoftware.aether.net.util.AggregateUpdateListener;
import com.aitusoftware.aether.transport.CounterSnapshotSubscriber;
import com.google.gson.Gson;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServer;
import org.agrona.CloseHelper;
import org.agrona.SystemUtil;
import org.agrona.concurrent.SleepingMillisIdleStrategy;

import java.io.Closeable;
import java.util.*;
import java.util.concurrent.TimeUnit;

public final class Server extends AbstractVerticle
{
    public static final int HTTP_PORT = Integer.getInteger("aether.net.http.port", 8090);
    private final Context context;
    private MediaDriver mediaDriver;
    private CounterSnapshotSubscriber counterSnapshotSubscriber;
    private Aether aether;

    public Server(final Context context)
    {
        this.context = context;
    }

    public static void main(final String[] args)
    {
        SystemUtil.loadPropertiesFiles(args);
        launchServer(new Context());
    }

    public static Closeable launchServer(final Context context)
    {
        final VertxOptions vertxOptions = new VertxOptions();
        final Vertx vertx = Vertx.vertx(vertxOptions);
        final Closeable closeable = vertx::close;
        final DeploymentOptions deploymentOptions = new DeploymentOptions();
        vertx.deployVerticle(new Server(context), deploymentOptions);
        return closeable;
    }

    @Override
    public void start()
    {
        final SystemSnapshot systemSnapshot = new SystemSnapshot();
        final RateMonitor rateMonitor = new RateMonitor(Arrays.asList(
            new RateBucket(10, TimeUnit.SECONDS),
            new RateBucket(1, TimeUnit.MINUTES)
        ));
        final AggregateUpdateListener listener = new AggregateUpdateListener(
            systemSnapshot, rateMonitor);
        final HttpServer httpServer = vertx.createHttpServer();
        final Gson gson = new Gson();
        final Mode mode = context.mode();
        if (mode == Mode.LOCAL)
        {
            aether = Aether.launch(new Aether.Context()
                .counterSnapshotListener(listener)
                .mode(Aether.Mode.LOCAL)
                .transport(Aether.Transport.LOCAL));
        }
        else
        {
            mediaDriver = MediaDriver.launchEmbedded(new MediaDriver.Context()
                .threadingMode(ThreadingMode.SHARED)
                .sharedIdleStrategy(new SleepingMillisIdleStrategy(1L)));
            counterSnapshotSubscriber = new CounterSnapshotSubscriber(new CounterSnapshotSubscriber.Context()
                .aeronDirectoryName(mediaDriver.aeronDirectoryName())
                .counterSnapshotListener(listener));
        }
        vertx.periodicStream(100).handler(i ->
        {
            if (counterSnapshotSubscriber != null)
            {
                counterSnapshotSubscriber.doWork();
            }
            else if (aether != null)
            {
                aether.doWork();
            }
        });
        httpServer.requestHandler(req ->
        {
            if (req.uri().endsWith(".js"))
            {
                req.response().putHeader("content-type", "text/javascript").sendFile("monitor.js");
            }
            else if (req.uri().endsWith(".json"))
            {
                final StringBuilder data = new StringBuilder();

                serialiseModel(systemSnapshot, rateMonitor, gson, data);
                req.response().putHeader("content-type", "application/json").end(data.toString());
            }
            else
            {
                req.response().putHeader("content-type", "text/html").sendFile("monitor.html");
            }
        });
        httpServer
            .websocketHandler(ws -> ws.handler(buffer ->
            {
                final StringBuilder data = new StringBuilder();

                serialiseModel(systemSnapshot, rateMonitor, gson, data);

                ws.writeTextMessage(data.toString());
            }))
            .listen(HTTP_PORT);
    }

    private void serialiseModel(
        final SystemSnapshot systemSnapshot,
        final RateMonitor rateMonitor,
        final Gson gson, final StringBuilder data)
    {
        final Map<StreamKey, Map<ChannelSessionKey, Set<ChannelSessionKey>>> connectionsByStream =
            systemSnapshot.getConnectionsByStream();
        final Map<String, Map<Integer, Set<PublisherData>>> treeView = new TreeMap<>();

        for (final Map.Entry<StreamKey, Map<ChannelSessionKey, Set<ChannelSessionKey>>> streamKeyMapEntry :
            connectionsByStream.entrySet())
        {
            for (final ChannelSessionKey pubChannelSessionKey : streamKeyMapEntry.getValue().keySet())
            {
                final Set<SubscriberData> subscriberList = new TreeSet<>();
                final Set<ChannelSessionKey> subChannelSessionKeys =
                    streamKeyMapEntry.getValue().get(pubChannelSessionKey);
                for (final ChannelSessionKey subChannelSessionKey : subChannelSessionKeys)
                {
                    final SubscriberCounterSet subscriberCounterSet =
                        systemSnapshot.getSubscriberCounterSet(subChannelSessionKey);
                    subscriberList.add(new SubscriberData(subChannelSessionKey.getLabel(), subscriberCounterSet));
                }

                final Set<PublisherData> streamMap =
                    treeView.computeIfAbsent(streamKeyMapEntry.getKey().getChannel(), key -> new TreeMap<>())
                    .computeIfAbsent(streamKeyMapEntry.getKey().getStreamId(), key -> new TreeSet<>());

                final PublisherData publisherData =
                    new PublisherData(pubChannelSessionKey.getLabel(),
                    systemSnapshot.getPublisherCounterSet(pubChannelSessionKey));
                final StreamRate streamRate = rateMonitor.publisherRates().get(pubChannelSessionKey);
                streamRate.consumeRates((duration, durationUnit, bytesPerSecond) ->
                {
                    publisherData.addPublishRate(duration + "_" + durationUnit, bytesPerSecond);
                });
                subscriberList.forEach(publisherData::addSubscriberData);
                streamMap.add(publisherData);
            }
        }

        final Map<String, Object> allData = new HashMap<>();
        allData.put("streams", treeView);
        allData.put("systemCounters", systemSnapshot.getSystemCounters());


        gson.toJson(allData, data);
    }

    @Override
    public void stop() throws Exception
    {
        super.stop();
        CloseHelper.quietClose(aether);
        CloseHelper.quietClose(counterSnapshotSubscriber);
        CloseHelper.quietClose(mediaDriver);
    }
}
