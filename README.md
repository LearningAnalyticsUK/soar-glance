## Student Score Predictor

This repo contains a minimal and untested spark job for producing models which predict the outcomes
of student/module pairings based upon past performance. 

If this model is successful it could be expanded upon to incorporate more sources of data, and to 
act as a long running web service.

In order to build and run the model, you will need the following pre-requisites:

* Java jdk 8
* Sbt 13.*
* Spark 2.1.0

Everything else will be fetched by Sbt. You can normally install sbt as a debian package (or 
through homebrew on OSX). Otherwise, get it [here](http://www.scala-sbt.org/download.html).

Assuming you have all the pre-requisites, run the following commands:

1. `sbt clean`
2. `sbt assembly`
3. `./submit.sh input_file.csv output_directory`

I will expand in the immediate future to allow interactive on-demand predictions from a generated
or loaded model.