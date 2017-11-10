#!/usr/bin/env bash


# Active sudo session required
sudo true

echo "[info] - Installing build time dependencies of glance"

# Function to return boolean if package is installed
installed () {
    $(dpkg-query -W -showformat='${Status}\n' $1 | grep -c "ok installed") -eq 0
}

# Glance is a scala js build, which requires node js
if [ installed "nodejs" ];
then
  sudo apt-get install nodejs;
fi

if [ installed "nodejs-legacy" ];
then
  sudo apt-get install nodejs-legacy;
fi

# sbt requires the bc package to check java versions
if [ installed "bc" ];
then
  sudo apt-get install bc;
fi

# First we need to execute sbt-docker
sudo sbt dockerize

# Then we need to create the pg-data directory to be mounted as a volume in docker up
mkdir -p ~/pg-data

# Also create the directory to mount glance's front end files to in the local filestystem, in case changes are needed
mkdir -p ~/glance/www

# Then we run docker compose up to launch the database container
sudo docker-compose up --build -d glance-eval-db

echo "[INFO] Sleeping now to allow the database time to start up. Back in 60 seconds."
sleep 60s
echo "[INFO] Resuming installation now."

sudo docker-compose up --build -d glance-eval-backend glance-eval-frontend
