#!/usr/bin/env bash

# Check that the scala-eval-cli.jar exists, otherwise build it.
GLANCE_JAR = 'glance-eval-cli/target/scala-2.11/scala-eval-cli.jar'

if [ ! -f GLANCE_JAR ]; then
    echo "[INFO] - Glance CLI jar does not yet exist, compiling ..."
    echo "[INFO] - This could take a couple of minutes and produce a lot of output."
    echo "[INFO] - Don't worry though, the jar will still execute once built, you don't have to do anything."
    echo "[INFO] - Just hang tight."
    sleep 7

    sbt glance-eval-cli/assembly
fi

exec java -jar glance-eval-cli/target/scala-2.11/scala-eval-cli.jar "$@"