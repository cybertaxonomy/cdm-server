# CDM Server Docker Container

## Requirements

Install the latest version of Docker (you will need at least version 18.09.0, earlier versions might work also but are untested). Instructions on the installation process for various operation systems are found at https://docs.docker.com/install/

NOTE: On linux you may want to add your user to the docker group (https://docs.docker.com/install/linux/linux-postinstall/):

    sudo usermod -aG docker $USER
    
Log out and in afterwards to update the group membership.

If you plan to use docker-compose install it now: https://docs.docker.com/compose/install/

## Build docker container manually

## with the `docker` command

In the root of the cdm-server project execute:

    docker build --tag=cybertaxonomy/cdm-server:${version} -f src/main/docker/${project.version}/Dockerfile.dev ./

e.g.
  
    docker build --tag="cybertaxonomy/cdm-server:5.5.0-SNAPSHOT" -f src/main/docker/production/Dockerfile.dev ./
    
### with maven

    mvn docker:build

## Running the image 

### with docker

Running the cdmserver docker container as developer

Expects the datasource file as `datasources-cdm-server.xml` (see cdm-server-dev.env)

NOTE: If you are using **symlinks** in the `.cdmLibrary` these must be use path outside of this folder. Otherwise they mioght not be valid inside the container

    docker run -dit --restart unless-stopped \
        -p 8080:8080 \
        -h cdmserver-dev \
        --env-file=src/main/docker/production/cdm-server-dev.env \
        --mount type=bind,source=$HOME/.cdmLibrary,destination=/data/.cdmLibrary \
        --mount type=bind,source=$(pwd)/,destination=/var/log/cdmserver \
        cybertaxonomy.eu/cdm-server:${version}
    
### with docker-compose

Run as detached service 

    docker-compose up -d
    
this starts the containers in the background and leaves them running.

Run and inspect the running container with the bash shell

    docker-compose run cdmserver /bin/bash



### with maven

    ...

## Shell access to the running docker container

1. find the `CONTAINER ID` or container name

    docker ps
    
1. Start the shell inside the container

    docker exec -it $CONAINER_ID|$CONTAINER_NAME /bin/bash

-i : --intertactive, -t: --tty
