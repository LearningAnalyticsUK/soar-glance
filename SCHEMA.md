# Schema

This document describes the form that your data must be in, in order to be imported and used with Glance.

Each type of data is contained within a single `.csv` file, with columns as described below. The information to be 
contained within each file is kept purposefully simple in order to be as agnostic as possible to the institution
producing the data. 

It is recommended that you use some sort of ETL (**E**xport, **T**ransform, **L**oad) tool, such as 
[Talend](https://www.talend.com/products/data-integration/) to export the necessary records from your institution's 
data-warehouse in the desired format.

Alternatively, if your institution operates some sort of "Data Integration" team, they should be able to use this 
document to provide you with the files.

All `.csv` files below should conform to the widely accepted [RFC 4180](https://tools.ietf.org/html/rfc4180) spec except 
where otherwise stated. 

**Note**: Many files are required by only some of the visualisations that it is possible to include in a Glance survey. 
If the set of visualisations you wish to include in your surveys do not require certain files, then you needn't prepare 
them. You can see which visualisations require which files [here](VISUALISATIONS.md). Likewise, the files listed below 
are unlikely to be an exhaustive description of all the useful student data available to each institution. Instead they 
are simply the data which is needed to power Glance's various visualisations.  

If you have any questions about the files specified in this document, please create an issue with the following format:

TODO: Write an issue template for reporting schema issues.

## Files

#### Printer.csv

This file simply contains a list of the printers in an institution. 


| Id      | Location |
|:-------:|:--------:|
| Integer | String   |

An example row would look something like the following:

```
2,Library - First floor
```

#### Printed.csv

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

#### Cluster.csv

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

#### ClusterSession.csv

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

#### Recap.csv

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

#### RecapSession.csv

This file contains a list of events corresponding to a student watching a lecture recording.

| Session Start Timestamp | Recap Id | Student Id | Study Id | Stage Id | Seconds Listened |
|:-----------------------:|:--------:|:----------:|:--------:|:--------:|:----------------:|
| ISO 8601 String         | Integer  | Integer    | Integer  | Integer  | Integer/Decimal  |

An example row would look something like the following: 

```
2015-01-01T00:06:48000Z,52837,23349,996,23,1140.135
```

**Note**: the _Recap Id_ field should correspond to entries in the _Id_ field of `Recap.csv`.

#### Stage.csv

This file simply contains a list of the stages a student may occupy throughout their time at an institution. These normally
correspond to academic years.

| Id | Stage Number | Study Stage |
|:--:|:------------:|:-----------------:|
| Integer | Integer | String |

An example row would look something like the following: 

```
2,1,Undergraduate Stage 1
```

#### Study.csv

This file simply contains a list of the programmes of study students may be enrolled on. 

| Id | Programme Code | Program Title | Programme Type | School Code | School | Faculty Code | 
|:--:|:--------------:|:-------------:|:--------------:|:-----------:|:------:|:------------:|
| Integer | String | String | String | String | String | String |

An example row would look something like the following: 

```
5,09MD,Doctor of Medicine,Postgraduate Research,D-SCMS,CLINICAL MEDICAL SCIENCES,F-FMED
```

#### VLESession.csv

This file contains a list of events corresponding to student sessions in an institution's Virtual Learning Environment (VLE).
A common example of a VLE is the software [Blackboard](https://blackboard.com/). 

| Id | Start Timestamp | End Timestamp | Student Id | Study Id | Stage Id | Num Clicks |
|:--:|:---------------:|:-------------:|:----------:|:--------:|:--------:|:----------:|
| Integer | ISO 8601 String | ISO 8601 String | Integer | Integer | Integer | Integer | 

An example row would look something like the following: 

```
331440600,2017-07-04T18:01:55000Z,2017-07-04T18:02:00000Z,50873,249,23,4
```

#### VLESessionContent.csv

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

#### Meeting.csv


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

#### Marks.csv

This file contains a list of marks for student work (both exams and coursework).

| Student Id | Study Id | Stage Id | Academic Year | Module Code | Component Text | Component Attempt | Component Mark | Weighting |  
|:----------:|:--------:|:--------:|:-------------:|:-----------:|:-----------:|:--------------:|:-----------------:|:--------------:|
| Integer | Integer | Integer | String | String | String | Integer | Integer | Decimal | 

An example row would look something like the following: 

```
37802,5802,23,2015,GNM8002,Written Examination,1,84,100.0
```

**Note**: when a module/course has more than 1 component, students will achieve marks for both which will be recorded in
separate rows. An example of this would look like the following: 

```
34814,1089,23,2015,SPG8027,Oral Presentation 1,1,70,20.0
34814,1089,23,2015,SPG8027,Individual Project Plan,1,66,80.0
```

Together, these marks constitute student **34814**'s performance in the module **SPG8027**.

**Note**: In the background, Glance will group these rows together and combine component marks, multiplied by their 
respective weights, to compute a total module/course mark. Therefore, if a given student completed a module, the _Weighting_
fields of all associated rows in `Marks.csv` should sum to 100.
 
#### Module.csv

This file contains a list of the modules/courses that a student may take, across an institution. 

| Id | Module Code | Title | Description | Keywords |
|:--:|:-----------:|:-----:|:-----------:|:--------:|
| Integer | String | String | String | String\|String\| ... \|String |

An example row would look something like the following: 

```
217,CSC2021,Software Engineering,"Teaches students the principles, tools and techniques of software engineering.",Programming|Commercial Accumen|Exam heavy 
```

**Note**: the _Description_ field is enclosed in double quotes as such a long field is likely to include commas. If left 
outside double quotes, commas are treated as field separators. Columns which contain these will then have too many rows 
may cause Glance to omit entries or even break. This is infact true of all files listed here, but mentioned specifically 
here as the most likely place to encounter the problem. 

**Note**: unlike any other fields in the other files listed here, the structure of the _Keywords_ field is important. The
[csv format](https://tools.ietf.org/html/rfc4180) is not naturally good at representing fields which which may be lists of 
arbitrary length. We choose here to represent multiple keywords in a single _Keyword_ field by separating them by pipe 
symbols (**|**). If you have a row with 0 or 1 keywords, then no pipe symbols are needed. Equally, pipe symbols are only 
needed to **separate** keywords; you do not need a trailing pipe symbol. Glance will parse such correctly formatted 
_Keyword_ fields as lists of distinct keywords. As with the  _Description_ field incorrect formatting may cause Glance to
omit entries or even break. 

## Data anonymity and consistency 

Throughout this guide, fields such as _Student Id_ are referenced many times. Exactly how identifying such information is 
will vary from institution to institution. For instance, can student ids be used to lookup contact info on some widely 
accessible service? If so, student ids will often need to be anonymised before use with Glance in order to preserve 
student privacy.

This anonymisation will often involve simply replacing a student id with some other integer which has no meaning; i.e. 
10378592 becomes 17. 

**If this anonymisation takes place, it must be consistent accross all files!**. If the id 17 refers to student number 
10378592 in `Printed.csv`, then it must also refer to student number 10378592 in `ClusterSession.csv`. If the anonymisation
of student data is inconsistent then Glance will link unrelated records to one another and present incorrect information. 
