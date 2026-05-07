<#assign pageTitle="Broad Data Use Oversight System - Signing Official - New Progress Report Submitted From Your Institution">
<#assign greetingHtml="Hello ${userName},">
<#include "/freemarker/header.ftl">
      <tr>
        <td
          style="padding: 15px 15px 0px; font-family: 'Montserrat', sans-serif; font-size: 16px; color: #1F3B50; text-align: justify; line-height: 25px;">
          A researcher from your institution, ${researcherUserName}, submitted Progress Report ${darCode} for the
          following datasets:
        </td>
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
      <tr>
        <td
            style="padding: 15px 15px; font-family: 'Montserrat', sans-serif; font-size: 16px; color: #1F3B50; text-align: center; line-height: 25px;">
          <a href="${serverUrl}"
            style="text-decoration: none; font-family: 'Montserrat', sans-serif; color: #00609F; font-size: 20px; font-weight: bold;">
            Please login to view this Progress Report.
          </a>
        </td>
      </tr>
<#include "/freemarker/footer.ftl">
