## TODO

* Refactor generator and assessor away from cli options to be a Finch HTTP service for surveys and responses.
* Potentially rename module evaluation-server and create an evaluation-web module, though see if this is a) truly 
 necessary and b) might be done using sub-sub modules if so.
* Work out the build / deploy story for this small web service. (docker)
* Use uri params to set survey view which controls whether survey is base form fill in interface or uses one of the 
 fancier visualisation.
* Get more data.
* Decide on a data store - mongo good for now?
* Create small test suite.
* Create a config file or db entry mapping module codes to Titles, descriptions, topics etc...
* Import and play with D3 to produce dummy vis for surveys prior to Sara meeting.
