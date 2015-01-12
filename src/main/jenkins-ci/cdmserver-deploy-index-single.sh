#!/bin/bash -x

# NOTE: 
# This script will only deploy the index for a specific cdmserver instance specified by 
# $INSTANCE_NAME. In order to securly install the new index it is recommendet to
# stop the instance before the index deployment is done. After this script has been run 
# the instance must be of course be started again. The EDIT Jeknins servers proviede 
# the sessecary groovy scripts (see jenkins scriptler)
#
#
#
# Requirements: 
# 1) for the commands executed by this script on the remote server the user jenkins 
# (requires to be member of the group 'cdm' ?still true?) and needs the following permissions in 
# sudoers:
#
#  |  # User alias specification
#  |  User_Alias      CDMUSERS = jenkins
#  |
#  |  #Cmnd alias specification
#  |  Cmnd_Alias      CDMSERVER = /bin/rm -rf /var/lib/cdmserver/*, /bin/mv /var/lib/cdmserver/*, /bin/chmod -R 775 /var/lib/cdmserver/*, /bin/chown -R * /var/lib/cdmserver/*
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
if [ -z "${REMOTE_SERVER}" ]; then
	echo '$REMOTE_SERVER missing'
	exit 1
fi
if [ -z "${INSTANCE_NAME}" ]; then
  echo '$INSTANCE_NAME missing'
  exit 1
fi

echo "cdmserver-delpoy-index:"
echo "  REMOTE_SERVER = ${REMOTE_SERVER}"
echo "  LOCAL_INDEX_CONTAINER = ${LOCAL_INDEX_CONTAINER}"
echo "  REMOTE_INDEX_CONTAINER = ${REMOTE_INDEX_CONTAINER}"
echo "  INSTANCE_NAME = ${INSTANCE_NAME}"
echo "  "

echo "transferring  index to "${REMOTE_SERVER}
ssh ${REMOTE_SERVER} "sudo rm -rf ${REMOTE_INDEX_CONTAINER}tmp; sudo mkdir -p ${REMOTE_INDEX_CONTAINER}tmp; sudo chown -R cdm:cdm ${REMOTE_INDEX_CONTAINER}tmp"
cd ${LOCAL_INDEX_CONTAINER}/index
tar czf - ${INSTANCE_NAME} | ssh ${REMOTE_SERVER} "cd ${REMOTE_INDEX_CONTAINER}tmp ; tar xzf -"
# fixing owner and permissions
ssh ${REMOTE_SERVER} chmod -R 775 ${REMOTE_INDEX_CONTAINER}tmp
ssh ${REMOTE_SERVER} sudo chown -R cdm:cdm ${REMOTE_INDEX_CONTAINER}tmp

echo "installing new index for ${INSTANCE_NAME}"
ssh ${REMOTE_SERVER} sudo rm -rf ${REMOTE_INDEX_CONTAINER}index/${INSTANCE_NAME}
ssh ${REMOTE_SERVER} sudo mv ${REMOTE_INDEX_CONTAINER}tmp/${INSTANCE_NAME} ${REMOTE_INDEX_CONTAINER}index/${INSTANCE_NAME}
ssh ${REMOTE_SERVER} sudo rm -rf ${REMOTE_INDEX_CONTAINER}tmp
