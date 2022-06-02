# Uploading Institutions

To run, add a file with a list of institutions
to this directory. Run npm install, then run
from the command line:

```shell
node app.js \
    ./institutions.txt
    `gcloud auth print-access-token` \
    https://consent.dsde-dev.broadinstitute.org/
```

* Ensure that you have an admin role in the consent environment you are pointing to.
* Replace `institutions.txt` with the name of your text file.
* Replace `https://consent.dsde-dev.broadinstitute.org/` with the desired consent environment.

Errors will be printed out to the console

