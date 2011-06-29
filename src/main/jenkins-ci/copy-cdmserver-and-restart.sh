#!/bin/bash -x
#
# prior using this script make sure the requires sudo
# commands are allowed in /etc/sudoers by allowing the
# following command alias:
#  Cmnd_Alias      CDMSERVER = /bin/cp -f cdm-server*.jar /opt/cdmserver/, /etc/init.d/cdmserver *, /bin/rm -f /opt/cdmserver/cdm-server*.jar, /usr/bin/ln -s cdm-server*.jar cdm-server.jar

#MVN_PROJECT_TARGET_DIR="/home/andreas/workspaces/_svn-trunk/cdm-server/target"
MVN_PROJECT_TARGET_DIR="$WORKSPACE/cdm-server/target"

CDMSERVER_HOME="/opt/cdmserver"

cd \"$MVN_PROJECT_TARGET_DIR\"

CDMSERVER_JAR=$(ls -1 | grep cdm-server.*jar | grep -v sources)

if [ ! -e $CDMSERVER_JAR ]
then
    echo "cdmserver*.jar missing in target folder"
    exit 1
fi

sudo /etc/init.d/cdmserver stop
sudo -u cdm rm /opt/cdmserver/cdm-server.jar
sudo -u cdm cp -f $CDMSERVER_JAR /opt/cdmserver/
cd $CDMSERVER_HOME
sudo -u cdm ln -s $CDMSERVER_JAR cdm-server.jar
sudo /etc/init.d/cdmserver start