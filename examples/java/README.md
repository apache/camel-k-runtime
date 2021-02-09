# Camel-K-runtime Java Example

This example shows the usage of Camel-k-runtime to run a Java route.

The route involves timer component and log component.

## How to run

You have two ways of doing this.

First approach:

    mvn exec:exec

This approach will pack and run a camel-quarkus runner.

Second approach

```
    mvn clean package
    export CAMEL_K_CONF=${project.basedir}/data/application.properties
    export CAMEL_K_ROUTES=file:${project.basedir}/data/MyRoutes.java
    java -jar target/camel-k-runtime-example-java-runner.jar
```

You should get the following output in both cases

```
    2021-02-08 18:25:50,700 INFO  [org.apa.cam.k.Runtime] (main) Apache Camel K Runtime 1.7.0-SNAPSHOT
    2021-02-08 18:25:50,727 INFO  [org.apa.cam.qua.cor.CamelBootstrapRecorder] (main) bootstrap runtime: org.apache.camel.quarkus.main.CamelMainRuntime
    2021-02-08 18:25:50,733 INFO  [org.apa.cam.k.lis.SourcesConfigurer] (main) Loading routes from: file:/home/oscerd/workspace/apache-camel/camel-k-runtime/examples/java/data/MyRoutes.java
    2021-02-08 18:25:51,360 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main) Apache Camel 3.7.0 (camel-q) is starting
    2021-02-08 18:25:51,360 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main) StreamCaching is enabled on CamelContext: camel-q
    2021-02-08 18:25:51,366 INFO  [org.apa.cam.imp.eng.DefaultStreamCachingStrategy] (main) StreamCaching in use with spool directory: /tmp/camel-q and rules: [Spool > 128K body size]
    2021-02-08 18:25:51,367 INFO  [org.apa.cam.imp.eng.InternalRouteStartupManager] (main) Route: route1 started and consuming from: timer://tick
    2021-02-08 18:25:51,369 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main) Total 1 routes, of which 1 are started
    2021-02-08 18:25:51,369 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main) Apache Camel 3.7.0 (camel-q) started in 9ms
    2021-02-08 18:25:51,372 INFO  [io.quarkus] (main) camel-k-runtime-example-java 1.7.0-SNAPSHOT on JVM (powered by Quarkus 1.11.0.Final) started in 1.048s. 
    2021-02-08 18:25:51,373 INFO  [io.quarkus] (main) Profile prod activated. 
    2021-02-08 18:25:51,373 INFO  [io.quarkus] (main) Installed features: [camel-bean, camel-core, camel-endpointdsl, camel-k-core, camel-k-loader-java, camel-k-runtime, camel-log, camel-main, camel-support-  common, camel-timer, cdi]
    2021-02-08 18:25:52,402 INFO  [info] (Camel (camel-q) thread #0 - timer://tick) Exchange[ExchangePattern: InOnly, BodyType: null, Body: [Body is null]]
    2021-02-08 18:25:53,370 INFO  [info] (Camel (camel-q) thread #0 - timer://tick) Exchange[ExchangePattern: InOnly, BodyType: null, Body: [Body is null]]
```

## Help and contributions

If you hit any problem using Camel or have some feedback, then please
https://camel.apache.org/support.html[let us know].

We also love contributors, so
https://camel.apache.org/contributing.html[get involved] :-)

The Camel riders!
