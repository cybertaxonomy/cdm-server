FROM ubuntu:noble
# using same image as for https://github.com/mariadb-corporation/mariadb-server-docker/blob/master/10.3/Dockerfile

ADD maven/@project.artifactId@-@project.version@.jar /root/cdm-server.jar

ARG CDMSERVER_PORT=8080
ARG HEAP_SIZE_ARG=1024M
ARG LOGDIR="/log/cdmserver"

ENV CDM_HOME=/data
ENV HEAP_SIZE=$HEAP_SIZE_ARG
ENV CPU_CORES=1
ENV DATASOURCES=$CDM_HOME/.cdmLibrary/datasources.xml

ENV JAVA_OPTIONS="-Dspring.profiles.active=remoting"

ENV JAVA_OPTS="-Xmx$HEAP_SIZE \
    -XX:+UseG1GC -XX:ParallelGCThreads=$CPU_CORES -XX:ConcGCThreads=$CPU_CORES  \
    -Duser.home=$CDM_HOME \
    -Dcdm.logDir=$LOGDIR"

ENV SERVER_ARGS="-forceSchemaUpdate -jmx"
ENV CDMSERVER_ARGS=" -httpPort=$CDMSERVER_PORT -datasources $DATASOURCES"
ENV TZ=CET

EXPOSE $CDMSERVER_PORT

# Java 25 JRE + jemalloc (renamed libjemalloc2 since Ubuntu 20.04)
RUN apt-get update && apt-get install -y openjdk-25-jre-headless libjemalloc2 && apt-get clean

RUN apt-get clean && apt-get update && apt-get install -y locales && rm -rf /var/lib/apt/lists/* \
 && locale-gen "en_US.UTF-8"
ENV LANG=en_US.UTF-8 \
    LANGUAGE=en_US:en \
    LC_ALL=en_US.UTF-8

ENV LD_PRELOAD=/usr/lib/x86_64-linux-gnu/libjemalloc.so.2

# ssmtp is gone from the Ubuntu/Debian repos — msmtp is the maintained replacement
RUN apt-get update && apt-get install -y msmtp msmtp-mta && rm -rf /var/lib/apt/lists/*

COPY msmtprc /etc/msmtprc

RUN apt-get update && apt-get install -y iproute2 && rm -rf /var/lib/apt/lists/*

RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

CMD java $JAVA_OPTS $JAVA_OPTIONS -jar /root/cdm-server.jar $SERVER_ARGS $CDMSERVER_ARGS