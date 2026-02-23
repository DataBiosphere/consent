<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" lang="en">
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>DUOS - Votes Needed on DARs</title>
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
        You have Data Access Requests to vote on in DUOS!
      </td>
    </tr>
    <#if openedThisWeek?has_content>
      <tr></br><td id="submittedThisWeek" style="padding: 15px 15px 0px; font-family: 'Montserrat', sans-serif; font-size: 16px; color: #1F3B50; text-align: justify; line-height: 25px;"></br>Submitted this week:</td></tr>
      <#list openedThisWeek as item>
        <tr><td style="padding: 15px 15px 0px; font-family: 'Montserrat', sans-serif; font-size: 16px; color: #1F3B50; text-align: justify; line-height: 25px;">${item.darCode()}</td></tr>
      </#list>
    </#if>
    <#if openedLastWeek?has_content>
      <tr><td id="submittedLastWeek" style="padding: 15px 15px 0px; font-family: 'Montserrat', sans-serif; font-size: 16px; color: #1F3B50; text-align: justify; line-height: 25px;" ></br>Submitted last week:</td></tr>
      <#list openedLastWeek as item>
        <tr><td style="padding: 15px 15px 0px; font-family: 'Montserrat', sans-serif; font-size: 16px; color: #1F3B50; text-align: justify; line-height: 25px;">${item.darCode()}</td></tr>
      </#list>
    </#if>
    <#if olderRequests?has_content>
      <tr><td id="olderRequests" style="padding: 15px 15px 0px; font-family: 'Montserrat', sans-serif; font-size: 16px; color: #1F3B50; text-align: justify; line-height: 25px;" ></br>Submitted more than 2 weeks ago:</td></tr>
      <#list olderRequests as item>
        <tr><td style="padding: 15px 15px 0px; font-family: 'Montserrat', sans-serif; font-size: 16px; color: #1F3B50; text-align: justify; line-height: 25px;">${item.darCode()}</td></tr>
      </#list>
    </#if>
      <td style="padding: 15px 15px 0px; font-family: 'Montserrat', sans-serif; font-size: 16px; color: #1F3B50; text-align: left; line-height: 25px; display: block; font-weight: 500;">
        Please <a href="${serverUrl}" style="text-decoration: none; font-family: 'Montserrat', sans-serif; color: #00609F; font-size: 20px; font-weight: bold;">log into DUOS</a> and vote on these requests.
        </br>
        </br>
        Thanks,</br>
        </br>
        The DUOS team
      </td>
    </tr>
  </table>
</div>
</body>
</html>
