Consent Services v1
===================

First version of "consent-related" services.  


There are three separate deliverables here: 
  1. consent-indexer : reads one or more ontology files, presented as OBO, and indexes them into Elastic Search
  2. consent-autosuggest : produces autosuggestions from the indexed OBO files for entry into web forms, and 
  3. consent-ws : a set of web-services containing methods for ingesting and retrieving expressions representing data use restrictions found within consent forms.
