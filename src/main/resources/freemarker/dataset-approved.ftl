<#assign pageTitle="Broad Data Use Oversight System - Admin - Dataset Approved Notification">
<#assign greetingHtml="Dear ${dataSubmitterName},">
<#include "/freemarker/header.ftl">
        <tr>
            <td style="padding: 15px 15px 0px; font-family: 'Montserrat', sans-serif; font-size: 16px; color: #1F3B50; text-align: justify; line-height: 25px;">
                <p id="content" style="margin: 0;">
                  Your dataset, <b>${datasetName}</b>, submitted to the <b>${dacName}</b> for management of future data access requests has been <b>accepted</b>
                  and can be found in the DUOS Data Library with this URL:
                  <a href="${serverUrl}dataset/${datasetIdentifier}" target="_blank"
                     style="text-decoration: none; font-family: 'Montserrat', sans-serif; color: #00609F; font-size: 16px; font-weight: bold;">
                    ${datasetIdentifier}.
                  </a>
                </p>
                <p>
                    Please reach out to <a href="mailto:duos-support@broadinstitute.zendesk.com" style="text-decoration: none; font-family: 'Montserrat', sans-serif; color: #00609F;">duos-support@broadinstitute.zendesk.com</a> if you have any questions or concerns.
                </p>
                <p>Kind regards,<br>the DUOS team</p>
            </td>
        </tr>
<#include "/freemarker/footer.ftl">
