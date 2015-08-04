Consent-UI v1
=============

consent-ui is an angularJS application that requires consent-ws up and running 
with some preloaded data: dacUser table at least or nobody will be able to log in.

If consent-ws web services app is not running, you will need to get it running first. 
Please refer to consent-ws README.md to do so. 

To run consent-ui app :

 1. git pull over genomebridge-consent folder
    git clone git@github.com:broadinstitute/genomebridge-consent.git
 2. change to genomebridge-consent/consent-ui folder.
 3. configure javascript environment executing :
         npm install -g gulp
         npm install
         bower install
 4. to start the application on a test server :
         gulp serve


