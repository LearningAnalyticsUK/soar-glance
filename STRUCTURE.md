## Modules

This project is divided into a series of sub-modules as follows: 

* **core**: Contains typeclasses, data types and utilities common to all other modules of the **soar**.
* **model**: Contains spark batch program for generating predictive model for student attainment based upon attainment 
training data.
* **db**: Does what it says on the tin. Contains all database queries, db.db.migrations etc... used by the rest of **soar** 
(though not __glance-eval*__).
* **db-cli**: Contains cli tool for bulk import/export to/from the **soar** database.
* **server**: Contains the finch API for serving **soar** data, used by **glance**, **reports** etc...
* **glance-core**: Contains typeclasses, data types, react components and utilities common to the **glance-web**, 
**glance-eval** and **glance-eval-cli** submodules.
* **glance-web**: Contains the front end application Glance. Essentially a dashboard which provides many different 
descriptive statistics for student performance.
* **glance-eval**: Contains a version of the glance dashboard used for evaluation. Presents surveys which consist of 
"training" student data and a number of queries (students with certain module records elided). Surveyed staff members 
are asked to rank the query students by their expected performance in the elided modules. How accurate the ranking is 
indicates the effectiveness of Glance's descriptive statistics/visualisations.
* **glance-eval-cli**: Contains a cli program for generating Surveys presented in **glance-eval**.