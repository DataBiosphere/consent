# Data Use Oversight System - User Guide
## Documentation V1 Release

### 1. How to set up an instance of the service
This application relies on some web services, and related database, deployed and running.  That is included in consent-ws project, which is deployed on https://consent.dsde-dev.broadinstitute.org by Jenkins. It also relies on consent-ui, which is deployed by jenkins on  https://consent-ui.dsde-dev.broadinstitute.org/ .

To run it locally, for testing and development purposes, please refer to [README.md](https://github.com/broadinstitute/consent-ui/blob/develop/README.md) file on the source code repository.

### 2. How to make accounts for new users *(Admin)*
Log into the system. By default the landing page will be the Admin console (unless you also have a DAC role set, in that case you should click into **“Admin Console”** button in the header).

Once in the Admin Console click on **“Add User”** button. A window is going to pop up.

In order to catalog a new user in the system you should fill all fields: *Name, Google account ID* and at least a *Role* must be set as well. Please take a look at this file to be aware of the [User Roles and Management](https://github.com/broadinstitute/consent/tree/master/consent-ws/docs/UserRolesandManagement-V1Release.md). Once all the information is written, click on **“Add”** button. 

If the google account ID is already set in the system, an alert message will appear on the bottom of the window announcing the conflict. If no conflicts show up when clicking the button, this new user will be cataloged in the system and you will be redirected to the Manage Users page where you will be able to see your recent load at the top of the list.

>**Additional information:** You can also access the Add User window from the Admin Console page, by clicking the “Manage Users” button. On the top-right corner of the Manage Users page, an “Add User” button is displayed. 

### 3. How to edit users *(Admin)*
Log into the system. By default the landing page will be the Admin console (unless you also have a DAC role set, in that case you should click into **“Admin Console”** button in the header).

Once in the Admin Console click on **“Manage Users”** button. This will redirect you to a page. There will be the list of users cataloged in the system with all their information and a link to **“Edit”** this data. If you click on the link, a pop up window will emerge with the selected user information filled.

At least one of the fields must be modified to enabled the **“Save”** button.

For the moment, if there are any open elections when trying to edit a user, an alert message will be displayed on the bottom of the window announcing a conflict and the system will not allow you to edit roles. If no conflicts appear and you want to edit roles, please take a look at this file to be aware of the [User Roles and Management](https://github.com/broadinstitute/consent/tree/master/consent-ws/docs/UserRolesandManagement-V1Release.md).

Once changes are done and saved, the user information will be updated in the system and you will be redirected to the Manage Users page where could see your recent edition.

### 4. How to upload Data Use Limitations to the system *(Admin)*
Log into the system. By default the landing page will be the Admin console (unless you also have a DAC role set, in that case you should click into **“Admin Console”** button in the header).

Once in the Admin Console click on **“Add Data Use Limitations”** button. A window is going to pop up.

In order to create new Data Use Limitations (DUL) in the system you should fill all fields: 

* ***Consent ID*** - a unique DUL record ID  (use the ORSP portal record ID if available).  
* ***Name*** - (use the ORSP portal record ID if available).
* ***Structured Limitations*** - a string summarizing the content of the original Data Use Limitations in a structured format (use the ORSP portal record ID if available). 
* Upload a ***File*** for the Data Use Limitation. To this end, click the **“Upload File”** button and a window to browse and select the file will pop up. Once it’s selected, click **“Open”** and the File name will appear next to the button. When all the information is filled and the file is uploaded, click on **“Add”** button. If the Consent ID or the Name are already set in the system, an alert message will appear on the bottom of the window announcing the conflict. If no conflicts show up when clicking the button, this new Data Use Limitations will be upload in the system and you will be redirected to the Manage Data Use Limitations where you will be able to see your recent load at the top of the list.

>**Additional information:** You can also access the Add Data Use Limitations window from the Admin Console page, by clicking the “Manage Data Use Limitations” button. On the top-right corner of that page, a “Add Data Use Limitations” button is displayed. 

### 5. How to edit Data Use Limitations Record *(Admin)*
Log into the system. By default the landing page will be the Admin console (unless you also have a DAC role set, in that case you should click into **“Admin Console”** button in the header).

Once in the Admin Console click on **“Manage Data Use Limitations”** button. This will redirect you to a page. There will be the list of consents cataloged in the system with:

* ***Consent ID***
* ***Consent Name***
* Link to ***“Edit”*** this data
* ***Election status***. The administrator can submit consent records for review and voting by the DAC by creating an **Election**. The election status  may be: *Un-reviewed* if the election hasn’t been created yet, *Open* if the election is already created, *Canceled* if the election was created but then canceled and *Reviewed*, if the DAC has already voted. 
* ***Election actions*** links. From where you can either **“Create”** the election or **“Cancel”** it.

You should take into account that in order to be able to **“Edit”** a Data Use Limitations, the election cannot be opened, otherwise the system will disable the button. If you still want to edit it, you must cancel the election first. If the Edit button is enabled, you click on it, and a pop up window will emerge with the selected Data Use Limitations information filled.

At least one of the fields must be modified to enabled the **“Save”** button. The Consent ID field is not editable (to change this info the record must be deleted).  A Name can be updated with a new value only if a consent with the same denomination does not already exist in the system,  else an alert message will be displayed announcing the conflict.

### 6. How to create DAC elections for Data Use Limitations Review *(Admin)*
Log into the system. By default the landing page will be the Admin console (unless you also have a DAC role set, in that case you should click into **“Admin Console”** button in the header).

Once in the Admin Console click on **“Manage Data Use Limitations”** button. This will redirect you to a page. There will be the list of consents cataloged in the system with:

* ***Consent ID***
* ***Creation Date***
* Link to ***“Edit”*** this data
* ***Election status***. The administrator can submit consent records for review and voting by the DAC by creating an **Election**. The Election status may be: *Un-reviewed* if the election hasn’t been created yet, *Open* if the election is already created, *Canceled* if the election was created but then canceled and *Reviewed*, if the DAC has already voted. 
* Election actions links. From over here you will see a switch button with the option **“Create”** if the election is currently in an Un-reviewed, Canceled or Reviewed status and with the option **“Cancel”** if the election is Open.
* Bucket icon to delete a Data use Limitation. Only Enabled when the Data use Limitation is "Unreviewed".

If you either click on **“Create”** or in **“Cancel”** button a pop up window will show up asking for confirmation before any changes occurred. In order to create the election, the system checks the presence of a Chairperson and at least 4 Members to vote. If any of these cases is not accomplished an alert will appear announcing the conflict and the election won’t be created.

>**Additional information:** Every DAC Member cataloged in the system at the time of the creation of an election will have to vote.
If an election is canceled all the votes already logged are going to be lost.

>**IMPORTANT INFORMATION:** When the Data Use Limitation Status is "Reviewed", meaning that the DAC already voted on it. The **Admin** user can **Re-open** the Election in case something has changed, the DAC will have to vote on it again. In Statistics ("Reviewed Cases Records") all historical elections will appear with the Same Id but different dates and/or Results.

### 6.1. How to log a vote for a Data Use Limitations election *(DAC Member)*
Log into the system. By default the landing page will be the DAC Console. In this console, a list of open elections will appear. These will show the following information:

* ***Consent ID*** of the open election.
* ***Vote Status***. “Pending” if the vote hasn’t been logged yet, “Editable” if a vote has been logged and can be edited because the election is still open or “Urgent“ if a reminder was sent by the Chairperson to you.
* Amount of members that have ***Logged*** their vote in that election.
* ***Vote*** or ***Edit*** button (on top of that col, the amount of pending cases for review will appear). 

By clicking the **“Vote”** button you will enter a page to log your vote. From this page you will also be able to download the Data Use Limitation File and read the Structured format to compare them before voting.

After logging a vote you will be redirected to the DAC Console page.

### 6.2. How to Collect votes for a Data Use Limitations election *(Chairperson)*
Log into the system. By default the landing page will be the DAC Console. In this console, a list of open elections will appear. These will show the following information:

* ***Consent ID*** of the open election.
* ***Vote*** or ***Edit*** button (on top of that col, the quantity of pending cases for review will appear). 
* Amount of members that have ***Logged*** their vote in that election.
* ***Actions***, with links for **“Collect Votes”** of every election.

By clicking the **“Vote”** button you will enter a page to log your vote. From this page you will also be able to download the Data Use Limitation File and read the Structured format to compare them before voting.

After posting the vote, you will be redirected to the DAC Console page. From there you will be able to enter the Collect Votes page by clicking the **“Collect votes”** button in the list that now will be enabled. In this page, you can also download the File and read the Structured format of the Data Use Limitation. In addition to this, you can see the votes of all the Members that are involved in the election and you could send reminders to the ones that haven’t already voted. 


After all the votes have been posted, you will be able to close the election by posting the **Final Vote** in this page (the button to Vote will be dimmed until all DAC Members votes are logged).


### 7. How to create DAC elections for Data Access Requests Review *(Admin)*
Log into the system. By default the landing page will be the Admin console (unless you also have a DAC role set, in that case you should click into **“Admin Console”** button in the header).

Once in the Admin Console click on **“Manage Data Access Request”** button. This will redirect you to a page. There will be the list of Data Access Requests cataloged in the system with:

* ***Data Request ID***
* ***Project Title***
* ***Creation Date***
* ***RUS***. Link that opens a Modal with the "Research Use Statement"
* ***Election status***. The administrator can submit request records for review and voting by the DAC by creating an **Election**. The Election status may be: *Un-reviewed* if the election hasn’t been created yet, *Open* if the election is already created, *Canceled* if the election was created but then canceled and *Reviewed*, if the DAC has already voted. 
* ***Election actions links***. From over here you will see a switch button with the option **“Create”** if the election is currently in an Un-reviewed, Canceled or Reviewed status and with the option **“Cancel”** if the election is Open.

If you either click on **“Create”** or in **“Cancel”** button a pop up window will show up asking for confirmation before any changes occurred. In order to create the election, the system checks the presence of a Chairperson and at least 4 Members to vote. If any of these cases is not accomplished an alert will appear announcing the conflict and the election won’t be created.

>**Additional information:** Every DAC Member cataloged in the system at the time of the creation of an election will have to vote.
If an election is canceled all the votes already logged are going to be lost.

>**IMPORTANT INFORMATION:** When the Data Access Request Status is "Reviewed", meaning that the DAC already voted on it. The **Admin** user can **Re-open** the Election in case something has changed, the DAC will have to vote on it again. In Statistics ("Reviewed Cases Records") all historical elections will appear with the Same Id but different dates and/or Results.

### 7.1. How to log a vote for Data Access Request election *(DAC Member)*
Log into the system. By default the landing page will be the DAC Console. Under the section "Data Access Request Review", a list of open elections will appear. These will show the following information:

* ***Data Request ID*** of the open election.
* ***Vote Status***. “Pending” if the vote hasn’t been logged yet, “Editable” if a vote has been logged and can be edited because the election is still open or “Urgent“ if a reminder was sent by the Chairperson to you.
* Amount of members that have ***Logged*** their vote in that election.
* ***Vote*** or ***Edit*** button (on top of that col, the amount of pending cases for review will appear). 

By clicking the **“Vote”** button you will enter a page to log your vote. In this page you will see two accordions.
* The first one is to vote if Data Access should be granted to the Applicant. From this page you will also be able to download the Data Use Limitation File and see the "Research Use Statement" as well as the Application Summary with all the information needed to post the vote.
* The second one is to compare the Research Purpose and it's Structured Format.

>**Additional information:** You have to vote on both cases to complete the process.


After logging the votes you will be redirected to the DAC Console page.

### 7.2. How to Collect votes for Data Access Request election  *(Chairperson)*
Log into the system. By default the landing page will be the DAC Console. In this console, a list of open elections will appear. These will show the following information:

* ***Data Request ID*** of the open election.
* ***Vote*** or ***Edit*** button (on top of that col, the quantity of pending cases for review will appear). 
* Amount of members that have ***Logged*** their vote in that election.
* ***Actions***, with links for **“Collect Votes”** of every election.

By clicking the **“Vote”** button you will enter a page to log your vote as a Dac Member as explained in **7.1** . 

After posting the votes, you will be redirected to the DAC Console page. From there you will be able to enter the Collect Votes page by clicking the **“Collect votes”** button in the list that now will be enabled. In this page, you will also see the accordions with the information stated in **7.1**. In addition to this, you can see the votes of all the Members that are involved in the election and you could send reminders to the ones that haven’t already voted. 


After all the votes have been posted, you will be able to post **Final Votes** for both cases in this page (the button to Vote will be dimmed until all DAC members votes are logged).

### 7.3. How to Complete Access Election Process (Final Vote) *(Chairperson)*

Log into the system. By default the landing page will be the DAC Console. In this console, a list of open elections will appear. These will show the following information:

* ***Data Request ID*** of the open election.
* ***Vote*** or ***Edit*** button (on top of that col, the quantity of pending cases for review will appear). 
* Amount of members that have ***Logged*** their vote in that election.
* ***Actions***, with links for **“FINAL VOTE”** of every election.

From there you will be able to enter the Final Vote page by clicking the **“FINAL VOTE”** button in the list that now will be enabled. In this page, you will see the Research Purpose and the Data Use Limitation File to download. You'll see as well the accordions with the results of the votes for Data Use Limitation and Data Access Request.
Here you will post the Final decision on whether the Researcher is going to be allowed to access the Research Study or not. You'll also vote for "Decision Agreement", which means if the VAULT Decision (Automatic match decision) and the DAC Decision were consistent or not. After posting this two votes, the Election will be closed.


### 8. Upload Datasets *(Admin)*

Log into the system. By default the landing page will be the Admin console (unless you also have a DAC role set, in that case you should click into **“Admin Console”** button in the header).

Once in the Admin Console click on **“Add Datasets”** button. This will open a modal, there you can click the **"upload file"** button and select the file with the Dataset batch that you wish to upload to the system.
If some dataset in the batch is already uploaded but you wish to upload it again (if you change something for example), you'll have to check the **"Overwrite existing Datasets"** option. 

If any error appears during this process, the system will show the user a downloadable text file with a list of them so they can be fixed before uploading the batch again.

>**Additional information:** There is a link to download a Spreadsheet model with Datasets at the bottom of the modal window.

### 9. Dataset Catalog (Admin)

This will show the list of Datasets and their relation with the specific Consent. The user will be able to check one or all of them and download the selected batch.

In addition to this, the **Admin** is entitled to **delete** a Dataset (if it's not related to any Data Access Request) by clicking the trash can icon and to **disable** it so it can no longer be used by the Researcher to ask for access. 
>**Additional information:** SINCE THERE IS NO VERSIONING OF DATASETS, THE LAST UPLOADED ONE WILL BE THE ONE RELATED TO PAST AND FUTURE DATA ACCESS REQUESTS AND ITS ELECTIONS.







