## Schema

This document describes the form that your data must be in, in order to be imported and used with Glance.

Each type of data is contained within a single `.csv` file, with columns as described below. The information to be 
contained within each file is kept purposefully simple in order to be as agnostic as possible to the institution
producing the data. 

It is recommended that you use some sort of ETL (**E**xport, **T**ransform, **L**oad) tool, such as 
[Talend](https://www.talend.com/products/data-integration/) to export the necessary records from your institution's 
data-warehouse in the desired format.

Alternatively, if your institution operates some sort of "Data Integration" team, they should be able to use this 
document to provide you with the files.

**Note**: Many files are required by only some of the visualisations that it is possible to include in a Glance survey. 
If the set of visualisations you wish to include in your surveys do not require certain files, then you needn't prepare 
them. You can see which visualisations require which files [here](VISUALISATIONS.md).

If you have any questions about the files specified in this document, please create an issue with the following format:

TODO: Write an issue template for reporting schema issues.

### Files

##### Printer.csv

This file simply contains a list of the printers in an institution. 


| Id      | Location |
|:-------:|:--------:|
| Integer | String   |

An example row would look something like the following:

```
2,Library - First floor
```

##### Printed.csv

This file contains a list of events corresponding to students printing documents. 

| Timestamp | Student Id | Study Id | Stage Id | Printer Id | Num Sides |
|:---------:|:----------:|:--------:|:--------:|:----------:|:---------------:|
| ISO 8601 String | Integer | Integer | Integer | Integer | Integer |

**Note**: On the subject of timestamps: these should be expressed with ISO 8601 standards compliant strings including
timezone offset. The strings will look something like this: `1999-01-08T04:05:00+01:00` (where date portion is
yyyy-mm-dd). You can read about the ISO 8601 specification [here](https://en.wikipedia.org/wiki/ISO_8601).

An example row would look something like the following:

```
2015-09-07T07:58:10000Z,26567,249,23,104,1
```

##### Cluster.csv

This file contains a list of the computing clusters in an institution. I.e. The distinct locations or labs where a 
student may use a computer.

| Id | PC Name | Cluster Name | Building Name |
|:--:|:-------:|:------------:|:-------------:|
| Integer | String | String | String |

**Note**: The _PC Name_ column is expected to correspond to a prefix common to the names of all computers in a cluster. 
If this does information does not make sense in the context of your institution's data, it can be safely discarded.

An example row would look something like the following: 

```
6,COOKSONB,Cookson - Cluster,Medical School
```

Without the _PC Name_ field, this would be: 

```
6,,Cookson - Cluster,Medical School
```

**Note**: Remember to include the blank commas `6,,...`

##### ClusterSession.csv

This file contains a list of events corresponding to a student session using a computer in an institution's cluster. 


| Session Start Timestamp | Session End Timestamp | Student Id | Study Id | Stage Id | Machine Name |
|:-----------------------:|:---------------------:|:----------:|:--------:|:--------:|:------------:|
| ISO 8601 String         | ISO 8601 String       | Integer    | Integer  | Integer | String |

An example row would look something like the following: 

```
2015-08-30T08:04:40000Z,2015-08-30T08:36:10000Z,22186,1074,23,COOKSONB282
```

**Note**: If the _PC Name_ field is present in the `Cluster.csv` file then we can match that prefix against the 
_Machine Name_ field of `ClusterSession.csv` rows in order to determine in which cluster a session physically took place. 

##### Recap.csv

This file contains instances of lecture recordings which students may (re)watch at their leisure. This file is called 
`Recap.csv` as Recap is the lecture recording and playback software used at Newcastle, provided by [Panopto](https://www.panopto.com/).
Any conceptually similar lecture replay system will be fine as long as it can report the necessary information.

| Id | Session Name | Module Code |
|:--:|:------------:|:-----------:|
| Integer | String | String |

An example row would look something like the following: 

```
139,TCP3099/L03/01,TCP3099
```

**Note**: the _Module Code_ field is an identifier for the course, module or workshop from which the recording was 
produced. Also note that whilst the _Session Name_ field in the example contains the module code, this is not required. 
_Session Name_ simply identifies the actual thing being recorded and has no explicit structure. For instance, 
`TCP3099/L03/01` likely corresponds to the third lecture from the course with code TCP3099, but this could just as easily 
be `FooBar Lecture 124` 

##### RecapSession.csv

This file contains a list of events corresponding to a student watching a lecture recording.

| Session Start Timestamp | Recap Id | Student Id | Study Id | Stage Id | Seconds Listened |
|:-----------------------:|:--------:|:----------:|:--------:|:--------:|:----------------:|
| ISO 8601 String         | Integer  | Integer    | Integer  | Integer  | Integer/Decimal  |

An example row would look something like the following: 

```
2015-01-01T00:06:48000Z,52837,23349,996,23,1140.135
```

**Note**: the _Recap Id_ field should correspond to entries in the _Id_ field of `Recap.csv`.

##### Stage.csv

This file simply contains a list of the stages a student may occupy throughout their time at an institution. These normally
correspond to academic years.

| Id | Stage Number | Study Stage |
|:--:|:------------:|:-----------------:|
| Integer | Integer | String |

An example row would look something like the following: 

```
(2,1,'Undergraduate Stage 1'),
```

##### Study.csv

This file simply contains a list of the programmes of study students may be enrolled on. 

| Id | Programme Code | Program Title | Programme Type | School Code | School | Faculty Code | 
|:--:|:--------------:|:-------------:|:--------------:|:-----------:|:------:|:------------:|
| Integer | String | String | String | String | String | String |

An example row would look something like the following: 

```
5,09MD,Doctor of Medicine,Postgraduate Research,D-SCMS,CLINICAL MEDICAL SCIENCES,F-FMED
```


##### VLESession.csv

This file contains a list of events corresponding to student sessions in an institution's Virtual Learning Environment (VLE).
A common example of a VLE is the software [Blackboard](https://blackboard.com/). 

| Id | Start Timestamp | End Timestamp | Student Id | Study Id | Stage Id | Num Clicks |
|:--:|:---------------:|:-------------:|:----------:|:--------:|:--------:|:----------:|
| Integer | ISO 8601 String | ISO 8601 String | Integer | Integer | Integer | Integer | 

An example row would look something like the following: 

```
331440600,2017-07-04T18:01:55000Z,2017-07-04T18:02:00000Z,50873,249,23,4
```

##### VLESessionContent.csv

Within each VLE Session, a student may engage with multiple pieces of content. This file contains a list of events 
corresponding to a student engaging with (viewing, downloading etc...) a specific piece of content. 

| Session Id | Student Id | Module Code | Monitored Content |
|:----------:|:----------:|:-----------:|:-----------------:|
| Integer | Integer | String | String | 

An example row would look something like the following:

```
300296073,12488,ECO3001,Teaching Material/Exercise Sets/1
```

**Note**: the _Monitored Content_ field's structure has no significance. `Teaching Material/Exercise Sets/1` could just
as easily be `Economics Exercises Set 1` etc... Also note that the _Module Code_ field corresponds to the course to which 
the content being accessed belongs (it is assumed that all content will be owned by some module).

**Note**: the _Session Id_ field is expected to correspond to entries in the _Id_ field of `VLESession.csv`.

##### Meeting.csv


This file contains a list of events corresponding to recorded student meetings, likely with a tutor, supervisor or lecturer.

| Meeting Date | Student Id | Study Id | Stage Id | Meeting Type | 
|:------------:|:----------:|:--------:|:--------:|:------------:|
| ISO 8601 String | Integer | Integer | Integer | String | 

An example row would look something like the following: 

```
2016-08-31T09:00:00000Z,9982,1585,38,Tutor
```

**Note**: the type of the _Meeting Date_ field is listed as an ISO 8601 String. Meetings are often manually recorded and 
may only include a date, rather than a date and start time. In this case it is acceptable to provide the row in the
following form: 

```
2016-08-31,9982,1585,38,Tutor
```

The format `yyyy-mm-dd` is still technically valid according to ISO 8601 and will be parsed by Glance without issue. 

##### Marks.csv

This file contains a list of marks for student work (both exams and coursework).

| Student Id | Study Id | Stage Id | Academic Year | Module Code | Module Mark | Component Text | Component Attempt | Weighting |  
|:----------:|:--------:|:--------:|:-------------:|:-----------:|:-----------:|:--------------:|:-----------------:|:---------:|
| Integer | Integer | Integer | String | String | Decimal | String | Integer | Decimal | 

An example row would look something like the following: 

```
37802,5802,23,2015,GNM8002,84,Written Examination,1,84,100.0
```

**Note**: when a module/course has more than 1 component, students will achieve marks for both which will be recorded in
separate rows. Something about the Module mark in every row ... an example etc....


TODO: Module info file

TODO: A note about data completeness. Both in handling gaps or noise in the data coming from an institution and in having to 
discard potentially useful data to comply with the Glance spec. 

### Data anonymity and consistency 

Throughout this guide, fields such as _Student Id_ are referenced many times. Exactly how identifying such information is 
will vary from instituion to institution. For instance, can student ids be used to lookup contact info on some widely 
accessible service? If so, student ids will often need to be anonymised before use with Glance in order to preserve 
student privacy.

This anonymisation will often involve simply replacing a student id with some other integer which has no meaning; i.e. 
10378592 becomes 17. 

**If this anonymisation takes place, it must be consistent accross all files!**. If the id 17 refers to studnent number 
10378592 in `Printed.csv`, then it must also refer to student number 10378592 in `ClusterSession.csv`. If the anonymisation
of student data is inconsistent then Glance will link unrelated records to one another and present incorrect information. 
