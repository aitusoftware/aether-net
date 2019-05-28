# Aether-Net

A front-end for [Aether](https://github.com/aitusoftware/aether).

## Usage

Start an [Aether Collector](https://github.com/aitusoftware/aether#collector) using the following
configuration file:

#### aether-collector.properties

```
aether.monitoringLocations=client:/path/to/client/media-driver;server:/path/to/server/media-driver
aether.transport=AERON
aether.mode=PUBLISHER
aeron.dir=/path/to/aether-publisher-media-driver
aether.transport.channel=aeron:udp?endpoint=localhost:18996
```

### Start the collector

```
$ java -cp /path/to/aether-net-all.jar \
    com.aitusoftware.aether.Aether /path/to/aether-collector.properties
```

then create a server configuration file:

#### aether-net.properties

```
aether.transport=AERON
aether.mode=SUBSCRIBER
# Describe the endpoint to receive data on
# this should match the value supplied to the Aether collector
aether.transport.channel=aeron:udp?endpoint=localhost:18996
aether.net.http.port=8080
```

### Start the server

```
$ java -cp /path/to/aether-net-all.jar \
    com.aitusoftware.aether.net.Server /path/to/aether-net.properties
```

### GUI

The server will listen on the specified port (default `8080`), stream data can
be viewed at [http://localhost:8080/web](http://localhost:8080/web).

![Aether Net front-end](https://github.com/aitusoftware/aether-net/raw/master/doc/img/fe.png "Aether Net front-end")

## For local development

If all Aeron instances are available on the local machine (i.e. during application development),
Aether-Net can be launched in local mode:

```
$ java -cp /path/to/aether-net-all.jar \
    -Daether.monitoringLocations=A:/path/to/A/media-driver;B:/path/to/B/media-driver \
    -Daether.net.mode=LOCAL \
    com.aitusoftware.aether.net.Server /path/to/aether-net.properties
```