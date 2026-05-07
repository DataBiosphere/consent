<#assign pageTitle="Broad Data Use Oversight System - New DAR submitted that requires your approval">
<#assign greetingHtml="Hello ${userName},">
<#include "/freemarker/header.ftl">
    <tr>
      <td style="padding: 15px 15px 0px; font-family: 'Montserrat', sans-serif; font-size: 16px; color: #1F3B50; text-align: justify; line-height: 25px;">
        A researcher, ${researcherUserName}, submitted DAR ${darID}.  This Data Access Request requires your approval before it can be reviewed by the relevant Data Access Committees.
      </td>
    <tr>
      <td style="padding: 15px 15px; font-family: 'Montserrat', sans-serif; font-size: 16px; color: #1F3B50; text-align: center; line-height: 25px;">
        <a href="${serverUrl}"
           style="text-decoration: none; font-family: 'Montserrat', sans-serif; color: #00609F; font-size: 20px; font-weight: bold;">
          Please login to review and approve the request.
        </a>
      </td>
    </tr>
<#include "/freemarker/footer.ftl">
