<#assign pageTitle="DUOS - New data in DUOS today!">
<#assign greetingHtml="Dear ${userName},">
<#-- Email clients require inline CSS, so the shared declarations are defined once and reused. -->
<#assign bodyTextStyle="font-family: 'Montserrat', sans-serif; font-size: 16px; color: #1F3B50; line-height: 25px;">
<#assign headerCellStyle="font-family: 'Montserrat', sans-serif; font-style: italic; font-size: 15px; color: #00609f; border-bottom: 1px solid #cccccc; padding: 5px;">
<#assign dataCellStyle="font-family: 'Montserrat', sans-serif; color: #1F3B50; border-bottom: 1px solid #cccccc; padding: 5px; vertical-align: top;">
<#assign linkStyle="text-decoration: none; font-family: 'Montserrat', sans-serif; color: #00609F; font-weight: 600;">
<#assign badgeStyle="display: inline-block; padding: 1px 10px; border: 1px solid #cde1f0; border-radius: 12px; background-color: #eaf2f9; font-family: 'Montserrat', sans-serif; font-size: 13px; line-height: 20px; color: #00609F; white-space: nowrap;">
<#include "/freemarker/header.ftl">
    <tr>
      <td style="${bodyTextStyle} padding: 15px 15px 0px; text-align: justify;">
        The studies below were registered in DUOS today! Click the Study link to view the study and its datasets, or go to the <a style="${linkStyle}" href="${serverUrl}datalibrary">DUOS Data Library</a> to view all studies.
      </td>
    </tr>
  <#if newStudies?has_content>
    <tr>
      <td id="newStudyCount" style="${bodyTextStyle} padding: 15px 15px 0px; text-align: left; font-weight: 600;">
        <#if newStudies?size == 1>1 new study was<#else>${newStudies?size} new studies were</#if> registered today.
      </td>
    </tr>
    <tr>
      <td style="padding: 10px 15px 0px;">
        <table style="width: 100%; border-collapse: collapse;">
          <thead>
            <tr>
              <th style="${headerCellStyle} text-align: left;">Study Name</th>
              <th style="${headerCellStyle} text-align: left;">Access Type(s)</th>
              <th style="${headerCellStyle} text-align: right;">Number of Datasets</th>
            </tr>
          </thead>
          <tbody>
          <#list newStudies as item>
            <tr>
              <td style="${dataCellStyle} text-align: left;">
                <a style="${linkStyle}" href="${serverUrl}studies/${item.id()?c}">${item.name()}</a>
              </td>
              <td class="access-types" style="${dataCellStyle} text-align: left;">
                <#list item.accessTypes()?split(",") as accessType>
                  <span class="access-type-badge" style="${badgeStyle}">${accessType?trim?cap_first}</span>
                </#list>
              </td>
              <td class="dataset-count" style="${dataCellStyle} text-align: right; white-space: nowrap;">
                ${item.datasetCount()}
              </td>
            </tr>
          </#list>
          </tbody>
        </table>
      </td>
    </tr>
  </#if>
    <tr>
      <td style="${bodyTextStyle} padding: 25px 15px 0px; text-align: left; font-weight: 500;">
        The DUOS team
      </td>
    </tr>
<#include "/freemarker/footer.ftl">
