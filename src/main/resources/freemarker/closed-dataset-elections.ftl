<#assign pageTitle="Broad Data Use Oversight System - Closed Dataset Elections">
<#assign greetingHtml="Hello Admin,">
<#include "/freemarker/header.ftl">
        <tr>
            <td style="padding: 0px 15px 15px; font-family: 'Montserrat', sans-serif; font-size: 16px; color: #1F3B50; text-align: justify; line-height: 25px;">
                <p>
                    The following Dataset(s) elections finished its reviewing process by the Data Owners, or were closed by the system automatically because the allowed voting time expired.
                </p>
                <table style="margin: 15px 0 10px 0; border-collapse: collapse; width: 600px;">
                    <tr>
                        <th style="text-align: left; font-family: 'Montserrat', sans-serif; font-style: italic; font-size: 15px; color: #00609f; border-bottom: 1px solid #cccccc;">
                            Data Access<br>Request ID
                        </th>
                        <th style="text-align: left; font-family: 'Montserrat', sans-serif; font-style: italic; font-size: 15px; color: #00609f; border-bottom: 1px solid #cccccc;">
                            Amount of Datasets
                        </th>
                        <th style="text-align: left; font-family: 'Montserrat', sans-serif; font-style: italic; font-size: 15px; color: #00609f; border-bottom: 1px solid #cccccc;">
                            Election Result
                        </th>
                    </tr>
                    <#list closedElections>
                        <#items as election>
                            <tr>
                                <td style="font-family: 'Montserrat', sans-serif; padding: 5px; border-bottom: 1px solid #cccccc;">
                                    ${election.darId}
                                </td>
                                <td style="font-family: 'Montserrat', sans-serif; padding: 5px; border-bottom: 1px solid #cccccc;">
                                    ${election.numberOfDatasets}
                                </td>
                                <td style="font-family: 'Montserrat', sans-serif; padding: 5px; border-bottom: 1px solid #cccccc;">
                                    ${election.dsElectionResult}
                                </td>
                            </tr>
                        </#items>
                    </#list>
                </table>
            </td>
        </tr>
<#include "/freemarker/footer.ftl">
