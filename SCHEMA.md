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

This file simply contains a list of the printers in an institution. An example row would look something like the 
following:

```
2, 'Library - First floor'
```

Frequency: Intermittent/On Change

| Id      | Location |
|:-------:|:--------:|
| Integer | String   |

##### Printed.csv

This file contains a list of events corresponding to student's printing documents. 

Again, essentially a stripped down version of `STAR_Print`

**Note**: On the subject of timestamps, if Talend provides easy options for
this, then my preference is for ISO 8601 standards compliant strings including
timezone offset. Assuming that is possible, the strings will probably come out
looking something like this: `1999-01-08 04:05:00 +00:00` (where date portion is
yyyy-mm-dd). If the output doesn't look like this, please don't worry about it
as it may still be standards compliant or easily convertible. 

Frequency: Daily

| Timestamp | Student Id | Study Id | Stage Id | Printer Id | Num Sides |
|:---------:|:----------:|:--------:|:--------:|:----------:|:---------------:|
| ISO 8601 String | Integer | Integer | Integer | Integer | Integer |

##### Cluster.csv

Based on `DIM_Cluster`

Frequency: Intermittent/On Change

| Id | PC Name | Cluster Name | Building Name |
|:--:|:-------:|:------------:|:-------------:|
| Integer | String | String | String |

##### CSClusterSession.csv

Based on `STAR_CS_Cluster`

Frequency: Daily

| Session Start Timestamp | Session End Timestamp | Student Id | Study Id | Stage Id | Machine Name |
|:-----------------------:|:---------------------:|:----------:|:--------:|:--------:|:------------:|
| ISO 8601 String         | ISO 8601 String       | Integer    | Integer  | Integer | String |

##### ClusterSession.csv

Based on `STAR_Cluster`

**Note**: In Sql12, only `STAR_CS_Cluster` has the machine name field. Is this
an artifact of the import job or is machine name info only reported for sessions
in CS clusters?

Frequency: Daily

| Session Start Timestamp | Session End Timestamp | Student Id | Study Id | Stage Id | 
|:-----------------------:|:---------------------:|:----------:|:--------:|:--------:|
| ISO 8601 String         | ISO 8601 String       | Integer    | Integer  | Integer  | 

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
