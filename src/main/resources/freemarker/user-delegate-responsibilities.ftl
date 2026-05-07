<#assign pageTitle="Broad Data Use Oversight System - Delegated Responsibilities Notification">
<#assign greetingHtml="Hello ${userName},">
<#include "/freemarker/header.ftl">
        <tr>
            <td style="padding: 0px 15px; font-family: 'Montserrat', sans-serif; font-size: 16px; color: #1F3B50; text-align: justify; line-height: 25px;">
                <p>
                    The Administrator of the system has added a new role (${newRole}) to your user that includes
                    voting in the following elections:
                </p>
                <table style="margin: 15px 0 10px 0; border-collapse: collapse; width: 600px;">
                    <tr>
                        <th style="text-align: left; font-family: 'Montserrat', sans-serif; font-style: italic; font-size: 15px; color: #00609f; border-bottom: 1px solid #cccccc;">
                            Election ID
                        </th>
                        <th style="text-align: left; font-family: 'Montserrat', sans-serif; font-style: italic; font-size: 15px; color: #00609f; border-bottom: 1px solid #cccccc;">
                            Election Type
                        </th>
                    </tr>
                    <#list delegatedVotes>
                        <#items as vote>
                            <tr>
                                <td style="font-family: 'Montserrat', sans-serif; padding: 5px; border-bottom: 1px solid #cccccc;">
                                    ${vote.electionIdentifier}
                                </td>
                                <td style="font-family: 'Montserrat', sans-serif; padding: 5px; border-bottom: 1px solid #cccccc;">
                                    ${vote.electionType}
                                </td>
                            </tr>
                        </#items>
                    </#list>
                </table>
            </td>
        </tr>
        <tr>
            <td style="padding: 15px 15px; font-family: 'Montserrat', sans-serif; font-size: 16px; color: #1F3B50; text-align: center; line-height: 25px;">
                <a href="${serverUrl}" style="text-decoration: none; font-family: 'Montserrat', sans-serif; color: #00609F; font-size: 20px; font-weight: bold;">
                    Click here to log your votes
                </a>
            </td>
        </tr>
<#include "/freemarker/footer.ftl">
