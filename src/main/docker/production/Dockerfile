FROM ubuntu:bionic
# using same image as for https://github.com/mariadb-corporation/mariadb-server-docker/blob/master/10.3/Dockerfile

# paths of files and directories will be interpreted as relative to the source of the context of the build.

### ADD directive for the maven docker plugin
# 1. The maven plugin copies the jar to the artifacts assembly locaton at:
#    ./target/docker/eu.etaxonomy/cdm-server/${project.version}/build/maven/cdm-server-${project.version}.jar
# 2. a tar is being created at target/docker/eu.etaxonomy/cdm-server/${project.version}/tmp/docker-build.tar 
#    which contains all files located in /target/docker/production and the artifacts. 
#    Artifacts are put into the folder maven/ inside the tar archive
# 3. The tar archive is passed to the dockerdaemon and is used as working directory for the build
#ADD target/docker/@project.groupId@/@project.artifactId@/@project.version@/build/maven/@project.build.finalName@.jar /root/cdm-server.jar
#ADD target/cdm-server-5.11.0-SNAPSHOT.jar /root/cdm-server.jar
ADD maven/@project.artifactId@-@project.version@.jar /root/cdm-server.jar

ARG CDMSERVER_PORT=8080
ARG HEAP_SIZE_ARG=1024M
ARG LOGDIR="/log/cdmserver"

ENV CDM_HOME=/data
ENV HEAP_SIZE=$HEAP_SIZE_ARG
ENV CPU_CORES=1
ENV DATASOURCES=$CDM_HOME/.cdmLibrary/datasources.xml

# java options which can be overwritten
ENV JAVA_OPTIONS="-Dspring.profiles.active=remoting"

# fixed java options which must not be overwritten
ENV JAVA_OPTS="-Xmx$HEAP_SIZE \
    -XX:+UseG1GC -XX:ParallelGCThreads=$CPU_CORES -XX:ConcGCThreads=$CPU_CORES  \
    -Duser.home=$CDM_HOME \
     -Dcdm.logDir=$LOGDIR"

ENV SERVER_ARGS="-forceSchemaUpdate -jmx"
ENV CDMSERVER_ARGS=" -httpPort=$CDMSERVER_PORT -datasources $DATASOURCES"
ENV TZ=CET

EXPOSE $CDMSERVER_PORT

RUN apt-get update; apt-get install -y openjdk-8-jre-headless libjemalloc1; apt-get clean
#set encoding
#RUN apt-get clean && apt-get update && apt-get install -y locales locales-all
#RUN sed -i -e 's/# en_US.UTF-8 UTF-8/en_US.UTF-8 UTF-8/' /etc/locale.gen && \
#    locale-gen
#RUN export LC_ALL=en_US.UTF-8
#RUN export LANG=en_US.UTF-8
#RUN locale-gen en_US.UTF-8
#ENV LANG en_US.UTF-8
#ENV LANGUAGE en_US:en
#ENV LC_ALL en_US.UTF-8

RUN apt-get clean && apt-get update && apt-get install -y locales && rm -rf /var/lib/apt/lists/* \
 && locale-gen "en_US.UTF-8"
ENV LANG=en_US.UTF-8 \
    LANGUAGE=en_US:en \
    LC_ALL=en_US.UTF-8

ENV LD_PRELOAD=/usr/lib/x86_64-linux-gnu/libjemalloc.so.1

RUN apt-get update && apt-get install -y ssmtp && rm -rf /var/lib/apt/lists/*

COPY ssmtp.conf /etc/ssmtp/ssmtp.conf

RUN apt-get update && apt-get install -y iproute2 && rm -rf /var/lib/apt/lists/*

RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone


CMD java $JAVA_OPTS $JAVA_OPTIONS  -jar /root/cdm-server.jar $SERVER_ARGS $CDMSERVER_ARGS
