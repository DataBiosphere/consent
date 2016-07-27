#! /bin/bash
sudo apt-get install jq
sudo apt-get install curl
response=$(curl -sb -H "Accept:application/json" http://swse.deri.org/RDFAlerts/alerts?url=https://storage.googleapis.com/broad-dsde-consent-dev/ont/organization/data-use.owl"&"format=json)
filled=$(echo $response | jq '.results | has(0)' )
if (echo $response | grep -q "okay");
then
   echo "SWSE Service responded successfully."
else
   echo "Something went wrong with the online validator service. Please check."
   retval=1
fi
if [ "$filled" == "true" ]; then
    echo "Unsuccessful parsing of data-use.owl file."
    retval=1
else
    echo "Successful parsing of data-use.owl file."
    retval=0
fi
return $retval