#!/usr/bin/env bash
spark-submit --driver-memory 6g --class uk.ac.ncl.la.ScorePredictor target/scala-2.11/model.jar -i $1 -o $2