#!/usr/bin/env bash

# Active sudo session required
sudo true

# Potentially clean up docker images etc...
sudo docker-compose down

sudo docker rmi $(sudo docker images -a)

# Dockerize to build images for updated source
sudo sbt dockerize

# Then we run docker compose up to rebuild and launch all the containers
sudo docker-compose up --build -d

