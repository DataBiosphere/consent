<#assign pageTitle="Broad Data Use Oversight System - Researcher - A researcher was ${radarText}approved for your dataset">
<#assign greetingHtml="Hello ${dataDepositorName},">
<#include "/freemarker/header.ftl">
        <tr>
            <td id="content" style="padding: 0px 15px; font-family: 'Montserrat', sans-serif; font-size: 16px; color: #1F3B50; text-align: justify; line-height: 25px;">
                <p>
                    ${darCode} submitted by <a style="text-decoration: none; font-family: 'Montserrat', sans-serif; color: #00609F;">${researcherEmail}</a> was ${radarText}approved by the DAC for the following datasets:
                </p>
                <table style="margin: 15px 0 10px 0; border-collapse: collapse; width: 600px;">
                    <tr>
                        <th style="text-align: left; font-family: 'Montserrat', sans-serif; font-style: italic; font-size: 15px; color: #00609f; border-bottom: 1px solid #cccccc;">
                            Dataset ID
                        </th>
                        <th style="text-align: left; font-family: 'Montserrat', sans-serif; font-style: italic; font-size: 15px; color: #00609f; border-bottom: 1px solid #cccccc;">
                            Dataset Name
                        </th>
                    </tr>
                    <#list datasets>
                        <#items as dataset>
                            <tr>
                                <td style="font-family: 'Montserrat', sans-serif; padding: 5px; border-bottom: 1px solid #cccccc;">
                                    ${dataset.identifier()}
                                </td>
                                <td style="font-family: 'Montserrat', sans-serif; padding: 5px; border-bottom: 1px solid #cccccc;">
                                    ${dataset.name()}
                                </td>
                            </tr>
                        </#items>
                    </#list>
                </table>
            </td>
        </tr>
        <tr>
            <td style="padding: 15px 15px 0px; font-family: 'Montserrat', sans-serif; font-size: 16px; color: #1F3B50; text-align: justify; line-height: 25px;">
                <p>If you have not set up your dataset(s) for automated access, please provide access to these individuals manually (ex. by adding them to the access list for the corresponding Terra Workspaces or Snapshots)</p>
                <p>We recommend you provide access to these datasets within the next 24 hours.</p>
                <p>Please reach out to <a href="mailto:duos-support@broadinstitute.zendesk.com" style="text-decoration: none; font-family: 'Montserrat', sans-serif; color: #00609F;">duos-support@broadinstitute.zendesk.com</a> if you have any questions or concerns.</p>
                <p>Thank you,<br>the DUOS team</p>
            </td>
        </tr>
<#include "/freemarker/footer.ftl">
