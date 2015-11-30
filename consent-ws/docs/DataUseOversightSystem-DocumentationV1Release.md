# Data Use Oversight System - User Guide
## Documentation V1 Release

### a. How to set up an instance of the service
This application relies on some web services, and related database, deployed and running.  That is included in consent-ws project, which is deployed on https://consent-ci.broadinstitute.org/ by Jenkins. At the moment of writing this, a similar Jenkins’s deployment task for consent-ui is in progress, not yet finished, but we may expect it soon. 

To run it locally, for testing and development purposes, please refer to [README.md](https://github.com/broadinstitute/consent-ui/blob/develop/README.md) file on the source code repository.

### b. How to make accounts for new users *(Admin)*
Log into the system. By default the landing page will be the Admin console (unless you also have a DAC role set, in that case you should click into **“Admin Console”** button in the header).

Once in the Admin Console click on **“Add User”** button. A window is going to pop up.

In order to catalog a new user in the system you should fill all fields: *Name, Google account ID* and at least a *Role* must be set as well. Please take a look at this file to be aware of the [User Roles and Management](https://github.com/broadinstitute/consent/tree/master/consent-ws/docs/UserRolesandManagement-V1Release.md). Once all the information is written, click on **“Add”** button. 

If the google account ID is already set in the system, an alert message will appear on the bottom of the window announcing the conflict. If no conflicts show up when clicking the button, this new user will be cataloged in the system and you will be redirected to the Manage Users page where you will be able to see your recent load at the top of the list.

>**Additional information:** You can also access the Add User window from the Admin Console page, by clicking the “Manage Users” button. On the top-right corner of the Manage Users page, a “Add User” button is displayed. 

### c. How to edit users *(Admin)*
Log into the system. By default the landing page will be the Admin console (unless you also have a DAC role set, in that case you should click into **“Admin Console”** button in the header).

Once in the Admin Console click on **“Manage Users”** button. This will redirect you to a page. There will be the list of users cataloged in the system with all their information and a link to **“Edit”** this data. If you click on the link, a pop up window will emerge with the selected user information filled.

At least one of the fields must be modified to enabled the **“Save”** button.

For the moment, if there are any open elections when trying to edit a user, an alert message will be displayed on the bottom of the window announcing a conflict and the system will not allow you to edit roles. If no conflicts appear and you want to edit roles, please take a look at this file to be aware of the [User Roles and Management](https://github.com/broadinstitute/consent/tree/master/consent-ws/docs/UserRolesandManagement-V1Release.md).

Once changes are done and saved, the user information will be updated in the system and you will be redirected to the Manage Users page where could see your recent edition.

### d. How to upload Data Use Limitations to the system *(Admin)*
Log into the system. By default the landing page will be the Admin console (unless you also have a DAC role set, in that case you should click into **“Admin Console”** button in the header).

Once in the Admin Console click on **“Add Data Use Limitations”** button. A window is going to pop up.

In order to create new Data Use Limitations (DUL) in the system you should fill all fields: 

* ***Consent ID*** - a unique DUL record ID  (use the ORSP portal record ID if available).  
* ***Name*** - (use the ORSP portal record ID if available).
* ***Structured Limitations*** - a string summarizing the content of the original Data Use Limitations in a structured format (use the ORSP portal record ID if available). 
* Upload a ***File*** for the Data Use Limitation. To this end, click the **“Upload File”** button and a window to browse and select the file will pop up. Once it’s selected, click **“Open”** and the File name will appear next to the button. When all the information is filled and the file is uploaded, click on **“Add”** button. If the Consent ID or the Name are already set in the system, an alert message will appear on the bottom of the window announcing the conflict. If no conflicts show up when clicking the button, this new Data Use Limitations will be upload in the system and you will be redirected to the Manage Data Use Limitations where you will be able to see your recent load at the top of the list.

>**Additional information:** You can also access the Add Data Use Limitations window from the Admin Console page, by clicking the “Manage Data Use Limitations” button. On the top-right corner of that page, a “Add Data Use Limitations” button is displayed. 

### e. How to edit Data Use Limitations Record *(Admin)*
Log into the system. By default the landing page will be the Admin console (unless you also have a DAC role set, in that case you should click into **“Admin Console”** button in the header).

Once in the Admin Console click on **“Manage Data Use Limitations”** button. This will redirect you to a page. There will be the list of consents cataloged in the system with:

* ***Consent ID***
* ***Consent Name***
* Link to ***“Edit”*** this data
* ***Election status***. The administrator can submit consent records for review and voting by the DAC by creating an **Election**. The election status  may be: *Un-reviewed* if the election hasn’t been created yet, *Open* if the election is already created, *Canceled* if the election was created but then canceled and *Reviewed*, if the DAC has already voted. 
* ***Election actions*** links. From where you can either **“Create”** the election or **“Cancel”** it.

You should take into account that in order to be able to **“Edit”** a Data Use Limitations, the election cannot be opened, otherwise the system will disable the button. If you still want to edit it, you must cancel the election first. If the Edit button is enabled, you click on it, and a pop up window will emerge with the selected Data Use Limitations information filled.

At least one of the fields must be modified to enabled the **“Save”** button. The Consent ID field is not editable (to change this info the record must be deleted).  A Name can be updated with a new value only if a consent with the same denomination does not already exist in the system,  else an alert message will be displayed announcing the conflict.

### f. How to create DAC elections for Data Use Limitations Review *(Admin)*
Log into the system. By default the landing page will be the Admin console (unless you also have a DAC role set, in that case you should click into **“Admin Console”** button in the header).

Once in the Admin Console click on **“Manage Data Use Limitations”** button. This will redirect you to a page. There will be the list of consents cataloged in the system with:

* ***Consent ID***
* ***Consent Name***
* Link to ***“Edit”*** this data
* ***Election status***. The administrator can submit consent records for review and voting by the DAC by creating an **Election**. The Election status may be: *Un-reviewed* if the election hasn’t been created yet, *Open* if the election is already created, *Canceled* if the election was created but then canceled and *Reviewed*, if the DAC has already voted. 
* Election actions links. From over here you will see a switch button with the option **“Create”** if the election is currently in an Un-reviewed, Canceled or Reviewed status and with the option **“Cancel”** if the election is Open.

If you either click on **“Create”** or in **“Cancel”** button a pop up window will show up asking for confirmation before any changes occurred. In order to create the election, the system checks the presence of a Chairperson and at least 4 Members to vote. If any of these cases is not accomplished an alert will appear announcing the conflict and the election won’t be created.

>**Additional information:** Every DAC Member cataloged in the system at the time of the creation of an election will have to vote.
If an election is canceled all the votes already logged are going to be lost.

### g. How to log a vote *(DAC Member)*
Log into the system. By default the landing page will be the DAC Console. In this console, a list of open elections will appear. These will show the following information:

* ***Consent ID*** of the open election.
* ***Vote Status***. “Pending” if the vote hasn’t been logged yet, “Editable” if a vote has been logged and can be edited because the election is still open or “Urgent“ if a reminder was sent by the Chairperson to you (This last feature is not implemented yet).
* Amount of members that have ***Logged*** their vote in that election.
* ***Vote*** or ***Edit*** button (on top of that col, the amount of pending cases for review will appear). 

By clicking the **“Vote”** button you will enter a page to log your vote. From this page you will also be able to download the Data Use Limitation File and read the Structured format to compare them before voting.

After logging a vote you will be redirected to the DAC Console page.

### h. How to Collect votes *(Chairperson)*
Log into the system. By default the landing page will be the DAC Console. In this console, a list of open elections will appear. These will show the following information:

* ***Consent ID*** of the open election.
* ***Vote*** or ***Edit*** button (on top of that col, the quantity of pending cases for review will appear). 
* Amount of members that have ***Logged*** their vote in that election.
* ***Actions***, with links for **“Collect Votes”** of every election.

By clicking the **“Vote”** button you will enter a page to log your vote. From this page you will also be able to download the Data Use Limitation File and read the Structured format to compare them before voting.

After posting the vote, you will be redirected to the DAC Console page. From there you will be able to enter the Collect Votes page by clicking the **“Collect votes”** button in the list that now will be enabled. In this page, you can also download the File and read the Structured format of the Data Use Limitation. In addition to this, you can see the votes of all the Members that are involved in the election and you could send reminders to the ones that haven’t already voted. 


After all the votes have been posted, you will be able to close the election by posting the **Final Vote** in this page (the button to Vote will be dimmed until all votes are logged).





