<#assign pageTitle="Broad Data Use Oversight System - Your vote was requested for a Data Access Request">
<#assign greetingHtml="Hello ${userName},">
<#include "/freemarker/header.ftl">
        <tr>
            <td style="padding: 15px 15px 0px; font-family: 'Montserrat', sans-serif; font-size: 16px; color: #1F3B50; text-align: justify; line-height: 25px;">
                Please vote on Data Access Request: ${entityName}. Click on the following link to log your vote:
            </td>
        </tr>
        <tr>
            <td style="padding: 15px 15px; font-family: 'Montserrat', sans-serif; font-size: 16px; color: #1F3B50; text-align: center; line-height: 25px;">
                <a href="${serverUrl}" style="text-decoration: none; font-family: 'Montserrat', sans-serif; color: #00609F; font-size: 20px; font-weight: bold;">
                    Click here to log your vote
                </a>
            </td>
        </tr>
<#include "/freemarker/footer.ftl">
