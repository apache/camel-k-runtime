# Camel-K-runtime Java Example for kafka consumer to AWS S3

This example shows the usage of Camel-k-runtime for kafka consumer to AWS S3

The route involves kafka and aws2-s3 component

## Setup

You'll need to have a kafka instance running on your machine or in docker.
You'll need AWS Credentials.

Fill correctly the application.properties file.

## How to run

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
    export CAMEL_K_ROUTES=file:${project.basedir}/data/MyRoutes.java
    java -jar target/quarkus-app/quarkus-run.jar
```

You should get the following output in both cases

```
2021-02-09 08:27:13,463 INFO  [org.apa.cam.k.Runtime] (main) Apache Camel K Runtime 1.7.0-SNAPSHOT
2021-02-09 08:27:13,482 INFO  [org.apa.cam.qua.cor.CamelBootstrapRecorder] (main) bootstrap runtime: org.apache.camel.quarkus.main.CamelMainRuntime
2021-02-09 08:27:13,488 INFO  [org.apa.cam.k.lis.SourcesConfigurer] (main) Loading routes from: file:/home/oscerd/workspace/apache-camel/camel-k-runtime/examples/kafka-source-s3/data/MyRoutes.java
2021-02-09 08:27:14,345 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main) Apache Camel 3.7.0 (camel-q) is starting
2021-02-09 08:27:14,345 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main) StreamCaching is enabled on CamelContext: camel-q
2021-02-09 08:27:15,724 INFO  [org.apa.cam.imp.eng.DefaultStreamCachingStrategy] (main) StreamCaching in use with spool directory: /tmp/camel-q and rules: [Spool > 128K body size]
2021-02-09 08:27:15,725 INFO  [org.apa.cam.com.kaf.KafkaConsumer] (main) Starting Kafka consumer on topic: testtopic with breakOnFirstError: false
2021-02-09 08:27:15,747 INFO  [org.apa.kaf.cli.con.ConsumerConfig] (main) ConsumerConfig values: 
	allow.auto.create.topics = true
	auto.commit.interval.ms = 5000
	auto.offset.reset = latest
	bootstrap.servers = [localhost:9092]
	check.crcs = true
	client.dns.lookup = default
	client.id = 
	client.rack = 
	connections.max.idle.ms = 540000
	default.api.timeout.ms = 60000
	enable.auto.commit = true
	exclude.internal.topics = true
	fetch.max.bytes = 52428800
	fetch.max.wait.ms = 500
	fetch.min.bytes = 1
	group.id = 94ee637f-5058-4b8a-98b3-6a8e6e3fcc5b
	group.instance.id = null
	heartbeat.interval.ms = 3000
	interceptor.classes = []
	internal.leave.group.on.close = true
	isolation.level = read_uncommitted
	key.deserializer = class org.apache.kafka.common.serialization.StringDeserializer
	max.partition.fetch.bytes = 1048576
	max.poll.interval.ms = 300000
	max.poll.records = 500
	metadata.max.age.ms = 300000
	metric.reporters = []
	metrics.num.samples = 2
	metrics.recording.level = INFO
	metrics.sample.window.ms = 30000
	partition.assignment.strategy = [org.apache.kafka.clients.consumer.RangeAssignor]
	receive.buffer.bytes = 65536
	reconnect.backoff.max.ms = 1000
	reconnect.backoff.ms = 50
	request.timeout.ms = 40000
	retry.backoff.ms = 100
	sasl.client.callback.handler.class = null
	sasl.jaas.config = null
	sasl.kerberos.kinit.cmd = /usr/bin/kinit
	sasl.kerberos.min.time.before.relogin = 60000
	sasl.kerberos.service.name = null
	sasl.kerberos.ticket.renew.jitter = 0.05
	sasl.kerberos.ticket.renew.window.factor = 0.8
	sasl.login.callback.handler.class = null
	sasl.login.class = null
	sasl.login.refresh.buffer.seconds = 300
	sasl.login.refresh.min.period.seconds = 60
	sasl.login.refresh.window.factor = 0.8
	sasl.login.refresh.window.jitter = 0.05
	sasl.mechanism = GSSAPI
	security.protocol = PLAINTEXT
	security.providers = null
	send.buffer.bytes = 131072
	session.timeout.ms = 10000
	ssl.cipher.suites = null
	ssl.enabled.protocols = [TLSv1.2]
	ssl.endpoint.identification.algorithm = https
	ssl.key.password = null
	ssl.keymanager.algorithm = SunX509
	ssl.keystore.location = null
	ssl.keystore.password = null
	ssl.keystore.type = JKS
	ssl.protocol = TLSv1.2
	ssl.provider = null
	ssl.secure.random.implementation = null
	ssl.trustmanager.algorithm = PKIX
	ssl.truststore.location = null
	ssl.truststore.password = null
	ssl.truststore.type = JKS
	value.deserializer = class org.apache.kafka.common.serialization.StringDeserializer

2021-02-09 08:27:15,850 WARN  [org.apa.kaf.cli.con.ConsumerConfig] (main) The configuration 'specific.avro.reader' was supplied but isn't a known config.
2021-02-09 08:27:15,851 INFO  [org.apa.kaf.com.uti.AppInfoParser] (main) Kafka version: 2.5.0
2021-02-09 08:27:15,851 INFO  [org.apa.kaf.com.uti.AppInfoParser] (main) Kafka commitId: 66563e712b0b9f84
2021-02-09 08:27:15,851 INFO  [org.apa.kaf.com.uti.AppInfoParser] (main) Kafka startTimeMs: 1612855635850
2021-02-09 08:27:15,854 INFO  [org.apa.cam.imp.eng.InternalRouteStartupManager] (main) Route: route1 started and consuming from: kafka://testtopic
2021-02-09 08:27:15,854 INFO  [org.apa.cam.com.kaf.KafkaConsumer] (Camel (camel-q) thread #0 - KafkaConsumer[testtopic]) Subscribing testtopic-Thread 0 to topic testtopic
2021-02-09 08:27:15,855 INFO  [org.apa.kaf.cli.con.KafkaConsumer] (Camel (camel-q) thread #0 - KafkaConsumer[testtopic]) [Consumer clientId=consumer-94ee637f-5058-4b8a-98b3-6a8e6e3fcc5b-1, groupId=94ee637f-5058-4b8a-98b3-6a8e6e3fcc5b] Subscribed to topic(s): testtopic
2021-02-09 08:27:15,857 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main) Total 1 routes, of which 1 are started
2021-02-09 08:27:15,857 INFO  [org.apa.cam.imp.eng.AbstractCamelContext] (main) Apache Camel 3.7.0 (camel-q) started in 1s512ms
2021-02-09 08:27:15,860 INFO  [io.quarkus] (main) camel-k-runtime-example-java 1.7.0-SNAPSHOT on JVM (powered by Quarkus 1.11.0.Final) started in 2.798s. 
2021-02-09 08:27:15,860 INFO  [io.quarkus] (main) Profile prod activated. 
2021-02-09 08:27:15,861 INFO  [io.quarkus] (main) Installed features: [camel-aws2-commons, camel-aws2-s3, camel-bean, camel-core, camel-endpointdsl, camel-k-core, camel-k-loader-java, camel-k-runtime, camel-kafka, camel-main, camel-support-common, camel-support-commons-logging, cdi]
2021-02-09 08:27:16,036 INFO  [org.apa.kaf.cli.Metadata] (Camel (camel-q) thread #0 - KafkaConsumer[testtopic]) [Consumer clientId=consumer-94ee637f-5058-4b8a-98b3-6a8e6e3fcc5b-1, groupId=94ee637f-5058-4b8a-98b3-6a8e6e3fcc5b] Cluster ID: ujmZ7YiORXCtQJ2h9USuEw
2021-02-09 08:27:16,037 INFO  [org.apa.kaf.cli.con.int.AbstractCoordinator] (Camel (camel-q) thread #0 - KafkaConsumer[testtopic]) [Consumer clientId=consumer-94ee637f-5058-4b8a-98b3-6a8e6e3fcc5b-1, groupId=94ee637f-5058-4b8a-98b3-6a8e6e3fcc5b] Discovered group coordinator ghost:9092 (id: 2147483647 rack: null)
2021-02-09 08:27:16,045 INFO  [org.apa.kaf.cli.con.int.AbstractCoordinator] (Camel (camel-q) thread #0 - KafkaConsumer[testtopic]) [Consumer clientId=consumer-94ee637f-5058-4b8a-98b3-6a8e6e3fcc5b-1, groupId=94ee637f-5058-4b8a-98b3-6a8e6e3fcc5b] (Re-)joining group
2021-02-09 08:27:16,052 INFO  [org.apa.kaf.cli.con.int.AbstractCoordinator] (Camel (camel-q) thread #0 - KafkaConsumer[testtopic]) [Consumer clientId=consumer-94ee637f-5058-4b8a-98b3-6a8e6e3fcc5b-1, groupId=94ee637f-5058-4b8a-98b3-6a8e6e3fcc5b] Join group failed with org.apache.kafka.common.errors.MemberIdRequiredException: The group member needs to have a valid member id before actually entering a consumer group
2021-02-09 08:27:16,052 INFO  [org.apa.kaf.cli.con.int.AbstractCoordinator] (Camel (camel-q) thread #0 - KafkaConsumer[testtopic]) [Consumer clientId=consumer-94ee637f-5058-4b8a-98b3-6a8e6e3fcc5b-1, groupId=94ee637f-5058-4b8a-98b3-6a8e6e3fcc5b] (Re-)joining group
2021-02-09 08:27:16,056 INFO  [org.apa.kaf.cli.con.int.ConsumerCoordinator] (Camel (camel-q) thread #0 - KafkaConsumer[testtopic]) [Consumer clientId=consumer-94ee637f-5058-4b8a-98b3-6a8e6e3fcc5b-1, groupId=94ee637f-5058-4b8a-98b3-6a8e6e3fcc5b] Finished assignment for group at generation 1: {consumer-94ee637f-5058-4b8a-98b3-6a8e6e3fcc5b-1-bde70b4e-c3e5-4e67-b832-b258cbaad60d=Assignment(partitions=[testtopic-0])}
2021-02-09 08:27:16,062 INFO  [org.apa.kaf.cli.con.int.AbstractCoordinator] (Camel (camel-q) thread #0 - KafkaConsumer[testtopic]) [Consumer clientId=consumer-94ee637f-5058-4b8a-98b3-6a8e6e3fcc5b-1, groupId=94ee637f-5058-4b8a-98b3-6a8e6e3fcc5b] Successfully joined group with generation 1
2021-02-09 08:27:16,065 INFO  [org.apa.kaf.cli.con.int.ConsumerCoordinator] (Camel (camel-q) thread #0 - KafkaConsumer[testtopic]) [Consumer clientId=consumer-94ee637f-5058-4b8a-98b3-6a8e6e3fcc5b-1, groupId=94ee637f-5058-4b8a-98b3-6a8e6e3fcc5b] Adding newly assigned partitions: testtopic-0
2021-02-09 08:27:16,072 INFO  [org.apa.kaf.cli.con.int.ConsumerCoordinator] (Camel (camel-q) thread #0 - KafkaConsumer[testtopic]) [Consumer clientId=consumer-94ee637f-5058-4b8a-98b3-6a8e6e3fcc5b-1, groupId=94ee637f-5058-4b8a-98b3-6a8e6e3fcc5b] Found no committed offset for partition testtopic-0
2021-02-09 08:27:16,083 INFO  [org.apa.kaf.cli.con.int.SubscriptionState] (Camel (camel-q) thread #0 - KafkaConsumer[testtopic]) [Consumer clientId=consumer-94ee637f-5058-4b8a-98b3-6a8e6e3fcc5b-1, groupId=94ee637f-5058-4b8a-98b3-6a8e6e3fcc5b] Resetting offset for partition testtopic-0 to offset 3.
```

## Help and contributions

If you hit any problem using Camel or have some feedback, then please
https://camel.apache.org/support.html[let us know].

We also love contributors, so
https://camel.apache.org/contributing.html[get involved] :-)

The Camel riders!
