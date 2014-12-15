#!/bin/bash 
#
# update the symlink for the stable folder
#
SSH_HOST='edit-deploy'

cd $WORKSPACE
PROJECT_VERSION=(`cat target/classes/version.properties | grep "cdmlib-remote-webapp.version" | sed -e "s/[^0-9]*\([^\n\r]*\)/\1/g"`)

if [ -n "$PROJECT_VERSION" ]; then
 ssh $SSH_HOST "rm -r /var/www/download/cdmserver/stable"
 ssh $SSH_HOST "ln -s /var/www/download/cdmserver/$PROJECT_VERSION /var/www/download/cdmserver/stable"
fi

