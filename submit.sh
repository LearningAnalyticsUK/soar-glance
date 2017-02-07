#!/usr/bin/env bash
spark-submit --driver-memory 6g --class uk.ac.ncl.la.ScorePredictor target/scala-2.11/student-attainment-predictor-assembly-0.1.jar -i $1 -o $2