#!/usr/bin/env bash

# An active sudo session is required
sudo true

# Get the Oracle jdk 8 ppa
sudo add-apt-repository ppa:webupd8team/java -y
sudo apt-get update

# Automate acceptance of oracle jdk license terms
echo debconf shared/accepted-oracle-license-v1-1 select true | sudo debconf-set-selections
echo debconf shared/accepted-oracle-license-v1-1 seen true | sudo debconf-set-selections

# Actually install java 8 and set all the default environment variables etc...
sudo apt-get install -y oracle-java8-installer
sudo apt-get install -y oracle-java8-set-def