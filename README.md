# Aether-Net

A front-end for [Aether](https://github.com/aitusoftware/aether).

## Usage

Start an [Aether Collector](https://github.com/aitusoftware/aether#collector), then create
a configuration file:

#### aether-net.properties

```
aether.transport=AERON
aether.mode=SUBSCRIBER
# Describe the endpoint to receive data on
# this should match the value supplied to the Aether collector
aether.transport.channel=aeron:udp?endpoint=monitoring-host:18996
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
