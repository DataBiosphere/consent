<#assign pageTitle="Broad Data Use Oversight System - New Study Registration Confirmation">
<#assign greetingHtml="Hello ${studySubmitterName},">
<#include "/freemarker/header.ftl">
    <tr>
      <td style="padding: 15px 15px 0; font-family: 'Montserrat', sans-serif; font-size: 16px; color: #1F3B50; text-align: justify; line-height: 25px;">
        <p id="content" style="margin: 0;">
          A new study, <b>${studyName}</b> (DUOS Study ID: <b>${studyId?string}</b>), has been submitted.
          <br><br>
          The submission includes the following assets:
        </p>
        <ul>
          <#list studyAssets?keys as assetType>

          <li>
            ${assetType}: ${studyAssets[assetType]?size} <#if studyAssets[assetType]?size == 1>item<#else>items</#if>
          </li>
        </#list>
        </ul>
        <p>
          This submission is pending review and will be published upon approval by a DUOS Admin or DAC.
        </p>
        <p>Kind regards,<br>the DUOS team</p>
      </td>
    </tr>
<#include "/freemarker/footer.ftl">
