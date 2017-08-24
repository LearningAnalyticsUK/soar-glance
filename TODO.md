
### MVP 

* [x] Fix java.time types for JS compilation. Going to need to special case both sides with some conversions I think.
* [x] Create endpoint aggregating a SessionsRecord object for a survey and sending it to the front end
* [x] Modify survey generator such that it only picks candidates who have actually got a score for the module in question.
* [x] Remove datapoint for rankedModule from student records
* [x] Label session usage axis correctly, using Moment js if necessary
* [x] Check that rank changes are modifying state correctly and being stored
* [ ] Fix compared student data for wrong columns in attainment bars

### Modules 

* [x] Add filters, title and description fields to module table
* [x] Hand populate module descriptions, titles and filters
* [ ] Add ranked module description to banner at top of survey page
* [ ] Add bootstrap pop overs for module titles to table headers
* [ ] Create module api endpoint to fetch module table as json
* [ ] Add module info to survey model

### Cleanup 

* [x] Investigate missing recap datapoint
* [x] Remove notes field from submission form
* [ ] Highlight "compared to" student in ranking table
* [ ] Change drag handle on rank table to up/down arrows
* [ ] Add individual rank change tracking if at all possible
* [ ] Write up instructions for a) setting up postgres; b) running flyWay migrations via sbt; c) building and using the 
command line tools for transforming data, generating surveys and loading support data; and d) running the local server 
from sbt in a tmux session
* [x] Resize charts for 4 by 4 grid on widest setting if possible. Otherwise add back tab
 