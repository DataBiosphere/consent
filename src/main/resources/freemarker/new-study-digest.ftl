<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" lang="en">
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>DUOS - New data in DUOS today!</title>
</head>

<body style="text-align: center; -webkit-font-smoothing: antialiased; -webkit-text-size-adjust: none; width: 100%; height: 100%; margin: 0; padding: 0;">
<div style="text-align: left; margin: 0 auto; width: 600px;">
  <table style="width: 600px">
    <tr>
      <td style="padding: 15px 15px 0px; text-align: center;">
        <img src="https://duos.org/images/favicon/android-chrome-192x192.png" height="100px" width="100px" alt="DUOS Logo">
      </td>
    </tr>
    <tr>
      <td id="userName" style="padding: 15px 15px 0px; font-family: 'Montserrat', sans-serif; font-size: 22px; color: #1F3B50; text-align: left; line-height: 25px; display: block; font-weight: 500;">
        Dear ${userName},
      </td>
    </tr>
    <tr>
      <td style="padding: 15px 15px 0px; font-family: 'Montserrat', sans-serif; font-size: 16px; color: #1F3B50; text-align: justify; line-height: 25px;">
        The studies below were registered in DUOS today! Click the Study link to view the study and its datasets, or go to the <a href="${serverUrl}datalibrary">DUOS Data Library</a>  to view all studies.
      </td>
    </tr>
    <tr>
      <td>
    <#if newStudies?has_content>
      <table style="padding: 15px 15px 0px; font-family: 'Montserrat', sans-serif; font-size: 16px; color: #1F3B50; text-align: justify; line-height: 25px;">
        <thead style="padding: 15px 15px 0px; font-family: 'Montserrat', sans-serif; font-size: 16px; color: #1F3B50; text-align: justify; line-height: 25px;">
        <tr>
          <th style="padding: 15px 15px 0px; font-family: 'Montserrat', sans-serif; font-size: 16px; color: #1F3B50; text-align: justify; line-height: 25px;">Study Name</th>
          <th style="padding: 15px 15px 0px; font-family: 'Montserrat', sans-serif; font-size: 16px; color: #1F3B50; text-align: justify; line-height: 25px;">Number of Datasets</th>
        </tr>
        </thead>
        <tbody>
      <#list newStudies as item>
        <tr><td style="padding: 15px 15px 0px; font-family: 'Montserrat', sans-serif; font-size: 16px; color: #1F3B50; text-align: justify; line-height: 25px;"><a href="${serverUrl}studies/${item.id()}">${item.name()}</a></td><td style="padding: 15px 15px 0px; font-family: 'Montserrat', sans-serif; font-size: 16px; color: #1F3B50; text-align: justify; line-height: 25px;">${item.datasetCount()}</td></tr>
      </#list>
        </tbody>
      </table>
    </#if>
      </td>
    </tr>
    <tr>
      <td style="padding: 15px 15px 0px; font-family: 'Montserrat', sans-serif; font-size: 16px; color: #1F3B50; text-align: left; line-height: 25px; display: block; font-weight: 500;">
        </br>
        </br>
        The DUOS team
      </td>
    </tr>
  </table>
</div>
</body>
</html>
