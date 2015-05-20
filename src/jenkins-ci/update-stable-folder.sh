#!/bin/bash 
#
# update the symlink for the stable folder
#

##############################################################
# NOTE: the ssh host should be configured in the ~/.ssh/config:
#     Host edit-deploy
#     HostName <ip ot host name>
#     User <deployment-user>
# 
# At the server to be delpoyed to you need to setup and configure the 

# 1. crate the <deployment-user> 
#
# 2. assuming you are logged in as the <deployment-user>:
#
#  echo "umask 0126" >> ~/.bashrc : 
#   
# 3. user 'www-data' must be member of the group of the <deployment-user>
#
# 4. The permissions of the folders to delpoyed to must be set to 775 ownership must be adjusted:
#   chmod -R 775 <the folders>
#   chown -R www-data:deploy <the folders> 
# 
#  
SSH_HOST='edit-deploy'
##############################################################

cd $WORKSPACE
PROJECT_VERSION=(`cat target/classes/version.properties | grep "cdm-webapp.version" | sed -e "s/[^0-9]*\([^\n\r]*\)/\1/g"`)

if [ -n "$PROJECT_VERSION" ]; then
 ssh $SSH_HOST "rm -r /var/www/download/cdmserver/stable"
 ssh $SSH_HOST "ln -s /var/www/download/cdmserver/$PROJECT_VERSION /var/www/download/cdmserver/stable"
fi

