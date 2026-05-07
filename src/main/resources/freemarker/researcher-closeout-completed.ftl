<#assign pageTitle="Broad Data Use Oversight System - Researcher - Closeout Complete">
<#assign greetingHtml="Dear ${userName},">
<#include "/freemarker/header.ftl">
    <tr>
      <td style="padding: 0px 15px; font-family: 'Montserrat', sans-serif; font-size: 16px; color: #1F3B50; text-align: left; line-height: 25px;">
        <p id="content">
          The closeout on Data Access Request (DAR) ${darCode} has been approved and your access to
          all datasets in this DAR will be revoked unless you have permission to use that data under
          another DAR.
        </p>
        <p id="warning">By completing this closeout, you have agreed to destroy all copies,
          versions, and derivations of the dataset(s) retrieved from NIH-designated
          controlled-access databases, on both local servers and hardware, and if cloud computing
          was used, delete the data and cloud images from cloud computing provider storage, virtual
          machines, databases, and random access archives, except as required by publication
          practices, institutional policies, or law to retain them.
        </p>
        <p>Thank you,<br>the DUOS team</p>
      </td>
    </tr>
<#include "/freemarker/footer.ftl">
