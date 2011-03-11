#!/bin/bash

cp $WORKSPACE/../cdm-server/lastSuccessfulBuild/artifact/cdm-server/target/cdm-server-3.0.jar/copy-cdmserver-and-restart.sh /var/www /opt/cdmserver/cdm-server.jar.by-jenkins
/etc/init.d/cdmserver restart