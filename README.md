## Student Score Predictor

**Note**: Readme is severely out of date. Update incoming

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
3. `sudo ./submit.sh input_file.csv output_directory`

I will expand in the immediate future to allow interactive on-demand predictions from a generated
or loaded model.

**Note**: The submit script requires sudo at the moment because the quickest fix for specifying an
output location for spark's log4j logs was to use the system /var/log/ dir. Will modify to user home
soon but that requires a little bit of platform specific runtime hackery so its on the todo list...
