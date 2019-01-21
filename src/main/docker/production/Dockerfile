FROM anapsix/alpine-java:8

# paths of files and directories will be interpreted as relative to the source of the context of the build.

### ADD directive for the maven docker plugin
# 1. The maven plugin copies the jar to the artifacts assembly locaton at:
#    ./target/docker/eu.etaxonomy/cdm-server/${project.version}/build/maven/cdm-server-${project.version}.jar
# 2. a tar is being created at target/docker/eu.etaxonomy/cdm-server/${project.version}/tmp/docker-build.tar 
#    which contains all files located in /target/docker/production and the artifacts. 
#    Artifacts are put into the folder maven/ inside the tar archive
# 3. The tar archive is passed to the dockerdaemon and is used as working directory for the build
#ADD target/docker/@project.groupId@/@project.artifactId@/@project.version@/build/maven/@project.build.finalName@.jar /root/cdm-server.jar
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
ARG JAVA_OPTS="-Xmx$HEAP_SIZE \
    -XX:+UseG1GC -XX:ParallelGCThreads=$CPU_CORES -XX:ConcGCThreads=$CPU_CORES  \
    -Duser.home=$CDM_HOME $JAVA_OPTIONS"

ARG SERVER_ARGS="-forceSchemaUpdate -jmx"

ENV CDMSERVER_ARGS="-httpPort=$CDMSERVER_PORT -logdir=$LOGDIR -datasources $DATASOURCES"

EXPOSE $CDMSERVER_PORT

CMD java $JAVA_OPTS $JAVA_OPTIONS  -jar /root/cdm-server.jar $SERVER_ARGS $CDMSERVER_ARGS
