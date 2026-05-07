<#assign pageTitle="Broad Data Use Oversight System - New DAR submitted to your DAC">
<#assign greetingHtml="Hello ${userName},">
<#include "/freemarker/header.ftl">
    <tr>
      <td style="padding: 15px 15px 0px; font-family: 'Montserrat', sans-serif; font-size: 16px; color: #1F3B50; text-align: justify; line-height: 25px;">
        A researcher, ${researcherUserName}, submitted DAR ${darID} for the following datasets:
      </td>
    </tr>
    <#list dacDatasetGroups?keys as dac>
      <tr>
          <td style="padding: 15px 15px 0px; font-family: 'Montserrat', sans-serif; font-size: 16px; color: #1F3B50; text-align: left; line-height: 25px; display: block;">
            <ul style="padding-left: 30px;">
              <li>
                <b>DAC ${dac}:</b>
                <ul>
                  <#list dacDatasetGroups[dac] as dataset>
                    <li>${dataset}</li>
                  </#list>
                </ul>
              </li>
            </ul>
          </td>
      </tr>
    </#list>
    <tr>
      <td style="padding: 15px 15px; font-family: 'Montserrat', sans-serif; font-size: 16px; color: #1F3B50; text-align: center; line-height: 25px;">
        <a href="${serverUrl}"
           style="text-decoration: none; font-family: 'Montserrat', sans-serif; color: #00609F; font-size: 20px; font-weight: bold;">
          Please login to review and open the request for voting by your DAC.
        </a>
      </td>
    </tr>
<#include "/freemarker/footer.ftl">
