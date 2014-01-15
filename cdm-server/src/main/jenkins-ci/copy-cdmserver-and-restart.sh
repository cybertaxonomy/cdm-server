#!/bin/bash
#
# prior using this script make sure the required sudo
# commands are allowed in /etc/sudoers by allowing the
# following the below used commands to be executed without
# password. Add the following enties to /etc/sudoers: 
# ------------------------------------------------------------
#   User_Alias      CDMUSERS = jenkins
#   Cmnd_Alias      CDMSERVER = /bin/cp -f cdm-server*.jar /opt/cdmserver/, /etc/init.d/cdmserver *, /bin/rm -f /opt/cdmserver/cdm-server.jar, /bin/ln -s cdm-server*.jar cdm-server.jar
#   CDMUSERS        ALL=(ALL)NOPASSWD: CDMSERVER
# ------------------------------------------------------------

if [ -z "${RESTART_AFTER_UPDATE}" ]; then
	RESTART_AFTER_UPDATE="restart"
fi

#TARGET_DIR="/home/andreas/workspaces/_svn-trunk/cdm-server/target"
if [ -z "$TARGET_DIR" ]; then
  TARGET_DIR="$WORKSPACE/cdm-server/target"
fi
if [ -z "$CDMSERVER_HOME" ]; then
  CDMSERVER_HOME="/opt/cdmserver"
fi
echo "copy-cdmserver-and-restart:"
echo "  TARGET_DIR="$TARGET_DIR
echo "  CDMSERVER_HOME="$CDMSERVER_HOME
echo "  RESTART_AFTER_UPDATE="$RESTART_AFTER_UPDATE

cd "${TARGET_DIR}"

CDMSERVER_JAR=$(ls -1 | grep cdm-server.*jar | grep -v sources)

if [ ! -e $CDMSERVER_JAR ]
then
    echo "cdmserver.*jar missing in target folder"
    exit 1
fi

sudo /etc/init.d/cdmserver stop
sudo -u cdm /bin/rm -f /opt/cdmserver/cdm-server.jar
sudo -u cdm /bin/cp -f $CDMSERVER_JAR /opt/cdmserver/
cd $CDMSERVER_HOME
sudo -u cdm /bin/ln -s $CDMSERVER_JAR cdm-server.jar
echo "restarting server"
if [ "${RESTART_AFTER_UPDATE}" == "restart" ]; then
    sudo /etc/init.d/cdmserver start
fi
