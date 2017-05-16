## Student Outcome Accelerator

**NOTE**: This documentation is woefully out of date - watch this space.

This repo contains the Soar framework for performing analytics jobs on data from Higher education. Soar is being 
developed as part of a HEFCE funded project, the aims of which include improving the quality of student and teacher
experiences at Universities. 

Broadly we will focus on the concept of "Human in the Loop" analytics, in order to allow faculty members to implicitly
train and retrain sophisticated predictive models of student performance without expensive interventions by data 
scientists etc... More on that here when we know what "that" is.

The rest of this readme describes the modules of the Soar framework, as well as instructions for their installation 
and use.

### Core

This module contains the base datastructures, types and helper methods upon which the rest of the project depends. There 
is no need to separately build the Core module; it will be automatically built where needed by the other modules.

### Model 

This module contains a minimal and untested spark job for producing models which predict the outcomes
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

### Evaluation

This module contains code used in the empirical evaluation of various parts of Soar. Over time as we describe and 
perform experiments for the purposes of publication, they will be added to this module. 

In order to make our evaluation of Soar as reproducible as possible, this module may be assembled into a single 
executable jar, much like the **model** module.

The first round of experiments evaluating Soar simply uses surveys containing samples of student marks to compare the 
predictive accuracy of the `ALS` algorithm employed in **model** to domain experts (teachers of modules). As such, 
the evaluation jar only has two functions at the moment: to generate these surveys and to read completed surveys and
calculate measures of accuracy for the predictions they contain. 

Assuming you have all the pre-requisites (which are the same as for **model**), you may build the evaluation jar 
using the following commands:

1. `sbt clean`
2. `sbt install`
3. `sbt evaluation/assembly`

Once this has finished running, you can then generate the surveys using the following command: 

4. `sudo ./submit.sh input.csv output_directory`

As with executing the **model** jar, `input.csv` contains a list of Student/Module scores in the form _Student Number, 
Module Code, Score_. The submit.sh file contains some default command line options which you may well want to change, 
such as what modules you would like to generate surveys for. 

If you would like to examine the other command line options, you can execute the evaluation jar directly with the 
following command (from within the project root directory): 

`java -jar evaluation/target/scala-2.11/soar-eval.jar --help`

This will produce a traditional unix style help dialogue:

> Soar Evaluation Survey generator 0.1.x
> 
>Usage: SoarEvalGen [options]
> 
>    -i, --input   <file>                    input is a required .csv file containing student/module scores. Format "StudentNumber, Module Code, Percentage" 
>
>    -o, --output  <directory>               output is a required parameter specifying the directory to write the surveys to.
>                             
>    -e, --elided  e.g. 20                   elided is an optional parameter specifying how many student records to partially elide in the generated surveys.
>
>    -m, --modules e.g. CSC1021, CSC2024...  modules is the list of modules for which to elide a students records. Only one module record will be elided per student. One survey is generated per elided module code.
>
>    -c, --common  e.g. CSC2024              common is an optional parameter specifying an additional module to elide student records for in *all* generated surveys.
>
>    -s, --seed    <int>                     seed is an optional parameter specifying a number to use as a seed when randomly selecting student records to elide.

Once you have executed the job, you will see that within the specified output directory, a folder has been created for 
each of the modules specified (except the module specified as common to all surveys, if any). Inside each of these 
folders is a file called `survey.csv` which may be directly opened with a spreadsheet program.

Those student/module scores  which we would like module leaders to predict have been been given the place holder 
value _-1.0_ for clarity. All scores following such a negative placeholder score have been elided.

Please keep track of which survey file belongs to which module code, as it may be harder to tell once they have been 
filled in by members of staff.
