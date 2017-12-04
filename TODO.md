
### Survey list feature

* [ ] Add a `--collection` command line option to the `generate` command. Given a collection parameter _n_, the `generate` 
command should: 
    * Create _n_ surveys, uniformly sampling from the students each time. This should be repeated for each module code
    requested.
    * Create a Collection object which indexes these surveys. 
* [ ] Add two additional tables to Glance's DB Schema, `Collection` and `CollectionMembership`:
    * The `Collection` table should have a structure like: `UUID | Module Code | Number`
    * The `CollectionMembership` table should have a structure like: `Collection Id | Survey Id | Index | Last`
* [ ] Add an API endpoint which given a collection id returns the first survey in a collection. Perhaps in a wrapper 
object carrying some meta data like the survey's index and whether it is the last one in the collection
* [ ] Build a "Next Survey" button into the thank you page, unless its the last survey at which point tell the user. 
* [ ] Build some basic session management so that a member of staff only has to put their email in at the start of a 
survey list, and if they come back to the same list later they resume the list from where they left it.

### Variable visualisations feature

* [x] Add two additional tables to Glance's DB Schema `Visualisation` and `SurveyVisualisation`:
    * The `Visualisations` table should have a structure like `UUID | Name | Description` for now.
    * The `SurveyVisualisations` table is a classic pivot table. It should have a structure like `Survey Id| Visualisation Id`    
* [x] Create the repository types for the tables
* [x] Draw out a map of React Components to API calls, so that I can make sure the API is robust enough to handle this 
kind of config. 
* [x] Create some kind of in memory component "index" and make sure we can retrieve components by whatever keys come along
with a requested survey.
* [x] On the front end, when loading a survey check its visualisations and selectively include the relevant components.
* [ ] Look at loading the data for surveys in 2+ trips, adding endpoints for the data required for each viz and fetching
only the data that is needed (this is low priority unless its low hanging fruit. For now shoving stuff in a big survey 
json object is fine).

### Cleanup

* [ ] Titles for visualisations.
* [ ] Scores / Results optionally included on thank you screen
* [ ] Root url should take you to a generic landing page which lists surveys and lists.
* [ ] Website should link to github repo in case of issues

### Dockerize

* [ ] Setup http-auth in nginx docker file.
* [ ] Check that rebuild script does the least amount of work but still actually captures updates in code.
* [ ] Create an uninstall script which removes docker images and all mounted volumes etc...
    * Add an option for a hard uninstall which removes all dependencies as well.
     
