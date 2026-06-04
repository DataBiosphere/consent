<#assign pageTitle="DUOS - New data in DUOS today!">
<#assign greetingHtml="Dear ${userName},">
<#include "/freemarker/header.ftl">
    <tr>
      <td style="padding: 15px 15px 0px; font-family: 'Montserrat', sans-serif; font-size: 16px; color: #1F3B50; text-align: justify; line-height: 25px;">
        The studies below were registered in DUOS today! Click the Study link to view the study and its datasets, or go to the <a href="${serverUrl}datalibrary">DUOS Data Library</a>  to view all studies.
      </td>
    </tr>
    <tr>
      <td>
    <#if newStudies?has_content>
      <table style="padding: 15px 15px 0px; font-family: 'Montserrat', sans-serif; font-size: 16px; color: #1F3B50; text-align: justify; line-height: 25px;">
        <thead style="padding: 15px 15px 0px; font-family: 'Montserrat', sans-serif; font-size: 16px; color: #1F3B50; text-align: justify; line-height: 25px;">
        <tr>
          <th style="padding: 15px 15px 0px; font-family: 'Montserrat', sans-serif; font-size: 16px; color: #1F3B50; text-align: justify; line-height: 25px;">Study Name</th>
          <th style="padding: 15px 15px 0px; font-family: 'Montserrat', sans-serif; font-size: 16px; color: #1F3B50; text-align: justify; line-height: 25px;">Access Type</th>
          <th style="padding: 15px 15px 0px; font-family: 'Montserrat', sans-serif; font-size: 16px; color: #1F3B50; text-align: justify; line-height: 25px;">Number of Datasets</th>
        </tr>
        </thead>
        <tbody>
      <#list newStudies as item>
        <tr><td style="padding: 15px 15px 0px; font-family: 'Montserrat', sans-serif; font-size: 16px; color: #1F3B50; text-align: justify; line-height: 25px;"><a href="${serverUrl}studies/${item.id()?c}">${item.name()}</a></td><td style="padding: 15px 15px 0px; font-family: 'Montserrat', sans-serif; font-size: 16px; color: #1F3B50; text-align: justify; line-height: 25px;">${item.accessTypes()}</td><td style="padding: 15px 15px 0px; font-family: 'Montserrat', sans-serif; font-size: 16px; color: #1F3B50; text-align: justify; line-height: 25px;">${item.datasetCount()}</td></tr>
      </#list>
        </tbody>
      </table>
    </#if>
      </td>
    </tr>
    <tr>
      <td style="padding: 15px 15px 0px; font-family: 'Montserrat', sans-serif; font-size: 16px; color: #1F3B50; text-align: left; line-height: 25px; display: block; font-weight: 500;">
        </br>
        </br>
        The DUOS team
      </td>
    </tr>
<#include "/freemarker/footer.ftl">
