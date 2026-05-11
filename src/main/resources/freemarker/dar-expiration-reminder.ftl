<#assign pageTitle="Broad Data Use Oversight System - Your DAR is about to expire">
<#assign greetingHtml="Hello ${userName},">
<#include "/freemarker/header.ftl">
    <tr id="expirationWarning">
      <td style="padding: 15px 15px 0px; font-family: 'Montserrat', sans-serif; font-size: 16px; color: #1F3B50; text-align: justify; line-height: 25px;">
        Your Data Access Request ${darCode} is expiring in 30 days. Please complete a progress
        report
        to preserve your access to this data.
      </td>
    </tr>
    <tr id="loginLink">
      <td style="padding: 15px 15px; font-family: 'Montserrat', sans-serif; font-size: 16px; color: #1F3B50; text-align: center; line-height: 25px;">
        <a href="${serverUrl}"
           style="text-decoration: none; font-family: 'Montserrat', sans-serif; color: #00609F; font-size: 20px; font-weight: bold;">
          Login to DUOS to submit a progress report.
        </a>
      </td>
    </tr>
<#include "/freemarker/footer.ftl">
