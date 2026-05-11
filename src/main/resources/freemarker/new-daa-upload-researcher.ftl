<#assign pageTitle="Broad Data Use Oversight System - New Data Access Agreement Upload">
<#assign greetingHtml="Dear ${researcherUserName},">
<#include "/freemarker/header.ftl">
    <tr>
      <td style="padding: 15px 15px 0px; font-family: 'Montserrat', sans-serif; font-size: 16px; color: #1F3B50; text-align: justify; line-height: 25px;">
        <p id="content">
          You were previously pre-authorized to request data from the ${dacName} under the
          ${previousDaaName}. The ${dacName} has recently transitioned to using the ${newDaaName}
          which will apply for all future requests to this DAC. In order to request access to data
          from the ${dacName} in the future, you must have your Signing Official pre-authorize
          you under this new agreement on their <a id="serverUrl" href="${serverUrl}" style="text-decoration: none; font-family: 'Montserrat', sans-serif; color: #00609F; font-size: 16px; font-weight: bold;">DUOS DAA Associations page</a>.
        </p>
        <p id="content2">
          Note, this transition to a new Data Access Agreement does not affect previously approved
          data access requests, which will continue to operate under the mutual agreement of the
          Data Access Agreement in use at the time of submission and approval.
        </p>
      </td>
    </tr>
<#include "/freemarker/footer.ftl">
