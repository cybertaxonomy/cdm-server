#!/bin/bash -x

# Requirements: 
# 1) for the commands executed by this script on the remote server the user jenkins 
# (requires to be member of the group 'cdm' ?still true?) and needs the following permissions in 
# sudoers:
#
#  |  # User alias specification
#  |  User_Alias      CDMUSERS = jenkins
#  |
#  |  #Cmnd alias specification
#  |  Cmnd_Alias      CDMSERVER = /bin/cp -f cdm-server*.jar /opt/cdmserver/, /etc/init.d/cdmserver *, /bin/rm -f /opt/cdmserver/cdm-server.jar, /bin/ln -s cdm-server*.jar cdm-server.jar, /bin/rm -rf /var/lib/cdmserver/*, /bin/mv /var/lib/cdmserver/*, /bin/chmod -R 775 /var/lib/cdmserver/*, /bin/chown -R * /var/lib/cdmserver/*
#  |  # User privilege specification
#  |  CDMUSERS        ALL=(ALL)NOPASSWD: CDMSERVER
#
# 2) the user executing this script (e.g. jenkins) must be allowed to ssh into the $REMOTE_SERVER 
#    via public key authentication 
#
# 3) /opt/cdmserver/.cdmLibrary/remote-webapp/index must be a symlink to the REMOTE_INDEX_CONTAINER
#
# 4) the REMOTE_INDEX_CONTAINER folder must have the permissions 775 for cdm:cdm
#    fix this: 
#      sudo chown -R cdm:cdm $REMOTE_INDEX_CONTAINER
#      sudo chmod -R 775 $REMOTE_INDEX_CONTAINER

if [ -z "${REMOTE_INDEX_CONTAINER}" ]; then
	REMOTE_INDEX_CONTAINER=/var/lib/cdmserver/
fi

if [ -z "${LOCAL_INDEX_CONTAINER}" ]; then
	LOCAL_INDEX_CONTAINER=/var/lib/cdmserver/
fi

if [ -z "${REMOTE_SERVER_DO_RESTART}" ]; then
	REMOTE_SERVER_DO_RESTART="restart"
fi
if [ -z "${REMOTE_SERVER}" ]; then
	echo '$REMOTE_SERVER missing'
	exit 1
fi

echo "cdmserver-delpoy-index:"
echo "  REMOTE_SERVER = ${REMOTE_SERVER}"
echo "  LOCAL_INDEX_CONTAINER = ${LOCAL_INDEX_CONTAINER}"
echo "  REMOTE_INDEX_CONTAINER = ${REMOTE_INDEX_CONTAINER}"
echo "  REMOTE_SERVER_DO_RESTART = ${REMOTE_SERVER_DO_RESTART}"
echo "  "

echo "transferring  index to "${REMOTE_SERVER}
ssh ${REMOTE_SERVER} "sudo rm -rf ${REMOTE_INDEX_CONTAINER}tmp; mkdir -p ${REMOTE_INDEX_CONTAINER}tmp"
cd ${LOCAL_INDEX_CONTAINER}
tar czf - index | ssh ${REMOTE_SERVER} "cd ${REMOTE_INDEX_CONTAINER}tmp ; tar xzf -"
# fixing owner and permissions
ssh ${REMOTE_SERVER} chmod -R 775 ${REMOTE_INDEX_CONTAINER}tmp
ssh ${REMOTE_SERVER} sudo chown -R cdm:cdm ${REMOTE_INDEX_CONTAINER}tmp

STATUS=$(ssh ${REMOTE_SERVER} "sudo /etc/init.d/cdmserver status")
NOT_RUNNING=$(echo "${STATUS}" | grep "not running")
if [ -z "${NOT_RUNNING}" ]; then
  echo "stopping $REMOTE_SERVER ..." 
  ssh ${REMOTE_SERVER} sudo /etc/init.d/cdmserver stop
  echo "waiting 20 seconds for server shutting down ..."
  sleep 20 
else 
  echo "server is not running"
fi

echo "switching to new index"
ssh ${REMOTE_SERVER} rm -rf ${REMOTE_INDEX_CONTAINER}index
ssh ${REMOTE_SERVER} mv ${REMOTE_INDEX_CONTAINER}tmp/index ${REMOTE_INDEX_CONTAINER}index
ssh ${REMOTE_SERVER} rm -rf ${REMOTE_INDEX_CONTAINER}tmp

if [ "${REMOTE_SERVER_DO_RESTART}" == "restart" ]; then
	echo "restarting server"
    ssh ${REMOTE_SERVER} sudo /etc/init.d/cdmserver start
fi