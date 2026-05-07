<#assign pageTitle="Broad Data Use Oversight System - New Progress Report ready for your vote">
<#assign greetingHtml="Hello ${userName},">
<#include "/freemarker/header.ftl">
        <tr>
            <td style="padding: 15px 15px 0px; font-family: 'Montserrat', sans-serif; font-size: 16px; color: #1F3B50; text-align: justify; line-height: 25px;">
                Progress Report Review case id ${entityName}, has been created. Please click on the following link to log your vote on it:
            </td>
        </tr>
        <tr>
            <td style="padding: 15px 15px; font-family: 'Montserrat', sans-serif; font-size: 16px; color: #1F3B50; text-align: center; line-height: 25px;">
                <a href="${serverUrl}" style="text-decoration: none; font-family: 'Montserrat', sans-serif; color: #00609F; font-size: 20px; font-weight: bold;">
                    Login to record your vote
                </a>
            </td>
        </tr>
<#include "/freemarker/footer.ftl">
