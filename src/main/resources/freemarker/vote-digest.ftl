<#assign pageTitle="DUOS - Votes Needed on DARs">
<#assign greetingHtml="Dear ${userName},">
<#include "/freemarker/header.ftl">
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
<#include "/freemarker/footer.ftl">
