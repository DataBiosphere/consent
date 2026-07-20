<#assign pageTitle="Broad Data Use Oversight System - New DAR submitted to your DAC">
<#assign greetingHtml="Hello ${displayName},">
<#include "/freemarker/header.ftl">
    <tr>
      <td style="padding: 15px 15px 0; font-family: 'Montserrat', sans-serif; font-size: 16px; color: #1F3B50; text-align: justify; line-height: 25px;">
        Data Access Request with ID ${darId} has a closeout for your review and approval.
        Please click the following link to review it:
      </td>
    </tr>
    <tr>
      <td style="padding: 15px 15px; font-family: 'Montserrat', sans-serif; font-size: 16px; color: #1F3B50; text-align: center; line-height: 25px;">
        <a href="${linkUrl}"
           style="text-decoration: none; font-family: 'Montserrat', sans-serif; color: #00609F; font-size: 20px; font-weight: bold;">
          Login to review this closeout
        </a>
      </td>
    </tr>
<#include "/freemarker/footer.ftl">
