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

Based on `DIM_Recap`

Frequency: Weekly/On Change

| Id | Session Name | Module Code |
|:--:|:------------:|:-----------:|
| Integer | String | String |

##### RecapSession.csv

Based on `RECAP` and/or `STAR_Recap`:

<!--**Note**: Some of the data in the Recap Sql12 table does not seem to be-->
<!--anonymised. For example, here is a row from `[dbo].[RECAP]`, where the 2nd and-->
<!--3rd columns are titled `student_id` and `login_id` respectively:-->

<!--```-->
<!--(2015-03-02 00:02:02 +0000,'S140191338','b4019133','MAS1341','Sampling the rows of a data frame',69.989,'01:02'),-->
<!--```-->

<!--Whereas here is a row from `STAR_BB_Sessions`, where the 2nd column is titled-->
<!--`PKstudent`: -->

<!--```-->
<!--(2015-10-07 17:35:51 +0000,15680,1019,23,0,24,6,299366454,2015-10-07 18:50:56 +0000),-->
<!--```-->

<!--As it stands I have no way to consistently join records from the RECAP table to-->
<!--records from any other table (by student anyway). Is there any chance we could-->
<!--replace the student/login id fields with a single anonymised Integer student id-->
<!--as with the other tables?-->

**Note**: Seconds Listened is a String field in Sql12. I indicate
Integer/Decimal below, but thats mostly because that feels semantically correct.
If String is easier thats no problem. I have to parse it all from CSV anyway 
(which is a string).

Frequency: Daily

| Session Start Timestamp | Recap Id | Student Id | Study Id | Stage Id | Seconds Listened |
|:-----------------------:|:--------:|:----------:|:--------:|:--------:|:----------------:|
| ISO 8601 String         | Integer  | Integer    | Integer  | Integer  | Integer/Decimal  |

##### Stage.csv

Based on `DIM_Stage`

**Note**: Any chance you could shed some semantic light on the difference
between Study Description (column 3) and Study Stage (column 4) for me? Here 
is an example from Sql12: 

```
(2,2,'Stage 1','UG Stage 1'),
...
(46,75,'Year 5 Writing Up','PGR Year 3+'),
```

Also - do you happen to know what the Stage number is used for vs the Id 
(PKStage in Sql12)?

Frequency: Intermittent/On Change

| Id | Stage Number | Study Stage |
|:--:|:------------:|:-----------------:|
| Integer | Integer | String |

##### Study.csv

Based on `DIM_Study`

Frequency: Intermittent/On Change

| Id | Programme Code | Program Title | Programme Type | School Code | School | Faculty Code | 
|:--:|:--------------:|:-------------:|:--------------:|:-----------:|:------:|:------------:|
| Integer | String | String | String | String | String | String |

##### BlackboardSession.csv

Based on `STAR_BB_Session`

Frequency: Daily

| Id | Start Timestamp | End Timestamp | Student Id | Study Id | Stage Id | Num Events | Num Clicks |
|:--:|:---------------:|:-------------:|:----------:|:--------:|:--------:|:----------:|:----------:|
| Integer | ISO 8601 String | ISO 8601 String | Integer | Integer | Integer | Integer | Integer | 

##### BlackboardSessionContent.csv

Based on `STAR_BB_sessioncontent`

**Note**: Do you Know what the `OtherContent` field in SQL12 is supposed to
represent? 

Also Module code below corresponds to BBModule field in Sql12.

Frequency: Daily

| Session Id | Student Id | Module Code | Monitored Content | Discussion | Tasks | Announcement | Assessment |
|:----------:|:----------:|:-----------:|:-----------------:|:----------:|:-----:|:------------:|:----------:|
| Integer | Integer | String | String | String | Integer | Integer | Integer |

##### Eportfolio.csv

Based on `STAR_LAeportfolio`

**Note**: The Meeting Date is just a date field in the Sql12, but a date string,
(yyyy-mm-dd) without a time or timezone adjustment is also ISO 8601 compatible: 
`1999-01-08`

Frequency: Weekly

| Meeting Date | Student Id | Study Id | Stage Id | Meeting Type | Contact Type |
|:------------:|:----------:|:--------:|:--------:|:------------:|:------------:|
| ISO 8601 String | Integer | Integer | Integer | String | String |

##### NessMarks.csv

Based on `STAR_NessMarks` 

Frequency: Weekly

| Student Id | Study Id | Stage Id | Academic Year | Progression Code | Module Code | Module Mark | Component Text | Component Attempt | Weighting | Timestamp Due | Timestamp Submitted | Submission Type |
|:----------:|:--------:|:--------:|:-------------:|:----------------:|:-----------:|:-----------:|:--------------:|:-----------------:|:---------:|:-------------:|:-------------------:|:---------------:|
| Integer | Integer | Integer | String | String | String | Decimal | String | Integer | Decimal | ISO 8601 String | ISO 8601 String | String |

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
