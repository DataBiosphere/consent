<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" lang="en">
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Broad Data Use Oversight System - New Study Registration Confirmation</title>
</head>

<body
    style="text-align: center; -webkit-font-smoothing: antialiased; -webkit-text-size-adjust: none; width: 100%; height: 100%; margin: 0; padding: 0;">
<div style="text-align: left; margin: 0 auto; width: 600px;">
  <table style="width: 600px">
    <tr>
      <td style="padding: 15px 15px 0; text-align: center;">
        <img src="https://duos.org/images/favicon/android-chrome-192x192.png" height="100px"
             width="100px" alt="DUOS Logo">
      </td>
    </tr>
    <tr>
      <td id="userName"
          style="padding: 15px 15px 0; font-family: 'Montserrat', sans-serif; font-size: 22px; color: #1F3B50; text-align: left; line-height: 25px; display: block; font-weight: 500;">
        Hello ${studySubmitterName},
      </td>
    </tr>
    <tr>
      <td style="padding: 15px 15px 0; font-family: 'Montserrat', sans-serif; font-size: 16px; color: #1F3B50; text-align: justify; line-height: 25px;">
        <p id="content" style="margin: 0;">
          A new study, <b>${studyName}</b> (DUOS Study ID: <b>${studyId?string}</b>), has been submitted.
          <br><br>
          The submission includes the following assets:
        </p>
        <ul>
          <#list studyAssets?keys as assetType>
          <li>${assetType}: ${studyAssets[assetType]?size} item(s)</li>
        </#list>
        </ul>
        <p>
          This submission is pending review and will be published upon approval by a DUOS Admin or DAC.
        </p>
        <p>Kind regards,<br>the DUOS team</p>
      </td>
    </tr>
  </table>
</div>
</body>
</html>
