FROM registry.access.redhat.com/ubi8/ubi-minimal

RUN mkdir -p /opt/camel-k
COPY target/camel-k-runtime-example-quarkus-kamelets-runner /opt/camel-k/runner
RUN chmod u+x /opt/camel-k/runner

ENTRYPOINT [ "/opt/camel-k/runner" ]