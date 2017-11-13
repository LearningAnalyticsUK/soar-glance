#!/usr/bin/env bash

# Active sudo session required
sudo true

echo "[info] - Welcome to the install script for glance."
echo "[info] - This process may take many minutes."
echo "[info] - Installing build time dependencies of glance."

# Function to return boolean if package is installed
installed () {

    (($(dpkg-query -W --showformat='${Status}\n' $1 | grep -c "ok installed") == 0))
}

# Glance is a scala js build, which requires node js
if installed "nodejs"
then
  echo "[info] - installing nodejs now."
  sudo apt-get install nodejs;
fi

if installed "nodejs-legacy"
then
  sudo apt-get install nodejs-legacy;
fi

# sbt requires the bc package to check java versions
if installed "bc"
then
  echo "[info] - isntalling bc now."
  sudo apt-get install bc;
fi

# check that java is installed, otherwise install oracle jdk 8. Attempt to answer prompts automatically
if installed "oracle-java8-installer"
then
  echo "[info] - isntalling oracle jdk 8 now."
  bash bin/installers/java.sh
fi

# check that sbt is installed, otherwise install it
if installed "sbt"
then
  echo "[info] - isntalling sbt now."
  bash bin/installers/sbt.sh
fi

# check that docker is installed, otherwise install it
if installed "docker-ce"
then
  echo "[info] - installing docker now using the official install script. You may be asked for prompts."
  bash bin/installers/docker.sh
fi

# First we need to execute sbt-docker
sudo sbt dockerize

# Then we need to create the pg-data directory to be mounted as a volume in docker up
mkdir -p ~/glance/pg-data

# Also create the directory to mount glance's front end files to in the local filestystem, in case changes are needed
mkdir -p ~/glance/www

# Then we run docker compose up to launch the database container
sudo docker-compose up --build -d glance-eval-db

echo "[INFO] Sleeping now to allow the database time to start up. Back in 60 seconds."
sleep 60s
echo "[INFO] Resuming installation now."

sudo docker-compose up --build -d glance-eval-backend glance-eval-frontend
