<#assign pageTitle="Broad Data Use Oversight System - Data Access Committee - Access to a dataset was Rule Automated DAR (RADAR) approved">
<#assign greetingHtml="Hello ${userName},">
<#include "/freemarker/header.ftl">
      <tr>
        <td
          style="padding: 0px 15px; font-family: 'Montserrat', sans-serif; font-size: 16px; color: #1F3B50; text-align: justify; line-height: 25px;">
          <p>
            A Data Access Request application ${darCode}, submitted by ${researcherUserName}, was Rule Automated DAR (RADAR) approved for the following dataset(s) with the corresponding data use limitations:
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
        <td
          style="padding: 0px 15px; font-family: 'Montserrat', sans-serif; font-size: 16px; color: #1F3B50; text-align: left; line-height: 25px;">
          <p>
            Data Access Committee Chair people can enable or disable RADAR approval rules DUOS.
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
<#include "/freemarker/footer.ftl">
