#!/bin/bash 
#
# update the symlink for the stable folder
#
cd $WORKSPACE
PROJECT_VERSION=(`cat target/classes/version.properties | grep "cdmlib-remote-webapp.version" | sed -e "s/[^0-9]*\([^\n\r]*\)/\1/g"`)

if [ -n "$PROJECT_VERSION" ]; then
 ssh root@wp5.e-taxonomy.eu "rm -r /var/www/download/cdmserver/stable"
 ssh root@wp5.e-taxonomy.eu "ln -s /var/www/download/cdmserver/$PROJECT_VERSION /var/www/download/cdmserver/stable"
 ssh root@wp5.e-taxonomy.eu "chown -R www-data:www-data /var/www/download/cdmserver/"$PROJECT_VERSION
 ssh root@wp5.e-taxonomy.eu "chown -R www-data:www-data /var/www/download/cdmserver/stable"
fi

