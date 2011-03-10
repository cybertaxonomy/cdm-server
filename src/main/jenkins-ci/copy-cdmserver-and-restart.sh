#!/bin/bash

cp $WORKSPACE/target/cdm-server*.jar /opt/cdmserver/cdm-server.jar.by-jenkins
/etc/init.d/cdmserver restart