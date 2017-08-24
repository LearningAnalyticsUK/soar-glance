#!/usr/bin/env bash
java -jar target/scala-2.11/soar-glance-eval-cli.jar transform -c $1 -r $2 -m $3 -o $4 -p "CSC" -y 2015 -s 2
