#!/usr/bin/env bash

sudo true

# First we need to execute sbt-docker
sudo sbt dockerize

# Then we need to create the pg-data directory to be mounted as a volume in docker up
mkdir -p ~/pg-data

# Then we run docker compose up to launch the database container
sudo docker-compose up --build -d glance-eval-db

# run flyway migrations
#eval "$(cat .env)"
#sudo sbt -Dflyway.password=$GLANCE_DB_PASS -Dflyway.user=$GLANCE_DB_USER -Dflyway.url="jdbc:postgresql://localhost:8003/$GLANCE_DB_NAME" glance-coreJVM/flywayMigrate

echo "[INFO] Sleeping now to allow the database time to start up. Back in 60 seconds."
sleep 60s
echo "[INFO] Resuming installation now."

sudo docker-compose up --build -d glance-eval-backend glance-eval-frontend
