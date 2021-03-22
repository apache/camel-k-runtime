# Camel-K-runtime YAML Cron Example

This example shows the usage of Camel-k-runtime to run a YAML Cron route.

The route involves log and timer components

## How to run

- Plain

You have two ways of doing this.

First approach:

```
    mvn exec:exec
```

This approach will pack and run a camel-quarkus runner.

Second approach

```
    mvn clean package
    export CAMEL_K_CONF=${project.basedir}/data/application.properties
    export CAMEL_K_ROUTES=file:${project.basedir}/data/routes.yaml?interceptors=cron
    java -jar target/quarkus-app/quarkus-run.jar
```

- Native

You have two ways of doing this.

First approach:

```
    mvn exec:exec -Pnative
```

This approach will pack and run a camel-quarkus runner.

Second approach

```
    mvn clean package -Pnative
    export CAMEL_K_CONF=${project.basedir}/data/application.properties
    export CAMEL_K_ROUTES=file:${project.basedir}/data/routes.yaml?interceptors=cron
    ./target/camel-k-runtime-example-cron-runner
```

You should get the following output in both cases

```
2021-02-09 07:37:47,492 INFO  [org.apa.cam.k.Runtime] (main) Apache Camel K Runtime 1.7.0-SNAPSHOT
2021-02-09 07:37:47,494 INFO  [org.apa.cam.qua.cor.CamelBootstrapRecorder] (main) bootstrap runtime: org.apache.camel.quarkus.main.CamelMainRuntime
2021-02-09 07:37:47,499 INFO  [org.apa.cam.k.lis.SourcesConfigurer] (main) Loading routes from: file:/home/oscerd/workspace/apache-camel/camel-k-runtime/examples/cron/data/routes.yaml?interceptors=cron
2021-02-09 07:37:47,956 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main) Apache Camel 3.7.0 (camel-q) is starting
2021-02-09 07:37:47,956 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main) StreamCaching is enabled on CamelContext: camel-q
2021-02-09 07:37:47,963 INFO  [org.apa.cam.imp.eng.DefaultStreamCachingStrategy] (main) StreamCaching in use with spool directory: /tmp/camel-q and rules: [Spool > 128K body size]
2021-02-09 07:37:47,963 INFO  [org.apa.cam.imp.eng.InternalRouteStartupManager] (main) Route: route1 started and consuming from: timer://camel-k-cron-override
2021-02-09 07:37:47,966 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main) Total 1 routes, of which 1 are started
2021-02-09 07:37:47,966 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main) Apache Camel 3.7.0 (camel-q) started in 10ms
2021-02-09 07:37:47,987 INFO  [info] (Camel (camel-q) thread #0 - timer://camel-k-cron-override) Exchange[ExchangePattern: InOnly, BodyType: String, Body: cron]
2021-02-09 07:37:47,991 INFO  [org.apa.cam.k.cro.CronSourceLoaderInterceptor$CronShutdownStrategy] (Camel (camel-q) thread #0 - timer://camel-k-cron-override) Initiate runtime shutdown
2021-02-09 07:37:47,992 INFO  [org.apa.cam.k.cro.CronSourceLoaderInterceptor$CronShutdownStrategy] (Camel (camel-q) thread #1 - CronShutdownStrategy) Shutting down the runtime
2021-02-09 07:37:48,074 INFO  [io.quarkus] (main) camel-k-runtime-example-cron 1.7.0-SNAPSHOT on JVM (powered by Quarkus 1.11.0.Final) started in 1.388s. Listening on: http://0.0.0.0:8080
2021-02-09 07:37:48,074 INFO  [io.quarkus] (main) Profile prod activated. 
2021-02-09 07:37:48,074 INFO  [io.quarkus] (main) Installed features: [camel-attachments, camel-bean, camel-core, camel-cron, camel-endpointdsl, camel-k-core, camel-k-cron, camel-k-loader-yaml, camel-k-runtime, camel-log, camel-main, camel-platform-http, camel-support-common, camel-timer, cdi, mutiny, smallrye-context-propagation, vertx, vertx-web]
2021-02-09 07:37:48,091 INFO  [org.apa.cam.mai.MainLifecycleStrategy] (main) CamelContext: camel-q has been shutdown, triggering shutdown of the JVM.
2021-02-09 07:37:48,091 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main) Apache Camel 3.7.0 (camel-q) is shutting down
2021-02-09 07:37:48,099 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main) Apache Camel 3.7.0 (camel-q) uptime 143ms
2021-02-09 07:37:48,100 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main) Apache Camel 3.7.0 (camel-q) is shutdown in 9ms
2021-02-09 07:37:48,117 INFO  [io.quarkus] (main) camel-k-runtime-example-cron stopped in 0.042s
```

## Help and contributions

If you hit any problem using Camel or have some feedback, then please
https://camel.apache.org/support.html[let us know].

We also love contributors, so
https://camel.apache.org/contributing.html[get involved] :-)

The Camel riders!
