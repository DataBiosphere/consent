<#assign pageTitle="Broad Data Use Oversight System - Signing Official - Access to a dataset was ${radarText}approved to your institution">
<#assign greetingHtml="Hello ${userName},">
<#include "/freemarker/header.ftl">
      <tr>
        <td
          style="padding: 0px 15px; font-family: 'Montserrat', sans-serif; font-size: 16px; color: #1F3B50; text-align: justify; line-height: 25px;">
          <p>
            A Data Access Request application ${darCode}, submitted by ${researcherUserName}, was ${radarText}approved for the following dataset(s) with the corresponding data use limitations:
          </p>
          <table style="margin: 15px 0 10px 0; border-collapse: collapse; width: 600px;">
            <tr>
              <th
                style="text-align: left; font-family: 'Montserrat', sans-serif; font-style: italic; font-size: 15px; color: #00609f; border-bottom: 1px solid #cccccc;">
                Dataset ID
              </th>
              <th
                style="text-align: left; font-family: 'Montserrat', sans-serif; font-style: italic; font-size: 15px; color: #00609f; border-bottom: 1px solid #cccccc;">
                Dataset Name
              </th>
            </tr>
            <#list datasets>
              <#items as dataset>
                <tr>
                  <td style="font-family: 'Montserrat', sans-serif; padding: 5px; border-bottom: 1px solid #cccccc;">
                    ${dataset.datasetIdentifier}
                  </td>
                  <td style="font-family: 'Montserrat', sans-serif; padding: 5px; border-bottom: 1px solid #cccccc;">
                    ${dataset.name}
                  </td>
                </tr>
              </#items>
            </#list>
          </table>
        </td>
      </tr>
      <tr>
        <td
          style="padding: 0px 15px; font-family: 'Montserrat', sans-serif; font-size: 16px; color: #1F3B50; text-align: left; line-height: 25px;">
          <p
            style="font-family: 'Montserrat', sans-serif; font-style: italic; font-size: 15px; color: #00609f; font-weight: 600; margin-bottom: 0;">
            Data Use Restrictions:</p>
          <p style="font-family: 'Montserrat', sans-serif; margin-top: 0;">${dataUseRestriction}</p>
          <hr style="border: none; border-bottom: 1px solid #cccccc;">
          <p style="margin-top: 20px 0 0;">Please remember your researcher has attested to the following
            in your data access request:</p>
          <ol style="font-size: 15px; margin: 0px;">
            <li>Data will only be used for approved research.</li>
            <li>Data confidentiality will be protected and the investigator will never make any attempt
              at “re-identification”.
            </li>
            <li>All applicable laws, local institutional policies, and terms and procedures specific to
              the study's data access policy will be followed.
            </li>
            <li>No attempts will be made to identify individual study participants from whom data were
              obtained.
            </li>
            <li>Data will not be sold or shared with third parties.</li>
            <li>The contributing investigator(s) who conducted the original study and the funding
              organizations involved in supporting the original study will be acknowledged in
              publications resulting from the analysis of those data.
            </li>
          </ol>
        </td>
      </tr>
      <tr>
        <td
          style="padding: 0px 15px; font-family: 'Montserrat', sans-serif; font-size: 16px; color: #1F3B50; text-align: left; line-height: 25px;">
          <p>You can review your institution's approved applications and attestation statement via your DUOS Signing
            Official Console.</p>
          <p>If approved for Broad Institute data, ${researcherUserName} should receive invitations to
            access these datasets within the next 24 hours (access will be designated to <a
              style="text-decoration: none; font-family: 'Montserrat', sans-serif; color: #00609F;">${researcherEmail}</a>).
            If you do not receive access in this time frame, please reach out to the Dataset Custodian
            listed on the dataset in the DUOS Dataset Catalog.</p>
          <p>If approved for NIH data, the PI submitting the original Data Access Request application
            will be added to the
            corresponding resources in the AnVIL (anvil.terra.bio) within 24 hours and will need to link
            their AnVIL account to their eRA Commons ID (<a href="https://app.terra.bio/#profile"
              style="text-decoration: none; font-family: 'Montserrat', sans-serif; color: #00609F;">here</a>)
          </p>
          <p>
            Please reach out to <a href="mailto:duos-support@broadinstitute.zendesk.com"
              style="text-decoration: none; font-family: 'Montserrat', sans-serif; color: #00609F;">duos-support@broadinstitute.zendesk.com</a>
            if you have any questions or concerns.
          </p>
          <p>Thank you,<br>the DUOS team</p>
        </td>
      </tr>
      <tr>
        <td
          style="padding: 15px 15px; font-family: 'Montserrat', sans-serif; font-size: 16px; color: #1F3B50; text-align: center; line-height: 25px;">
          <a href="${serverUrl}"
            style="text-decoration: none; font-family: 'Montserrat', sans-serif; color: #00609F; font-size: 20px; font-weight: bold;">
            Please login to view this Data Access Request.
          </a>
        </td>
      </tr>
<#include "/freemarker/footer.ftl">
