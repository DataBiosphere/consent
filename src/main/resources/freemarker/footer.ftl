<!-- Shared footer to close the common layout started by header.ftl -->
      <tr>
        <td style="padding: 30px 15px 10px; border-top: 1px solid #D9E2EC; text-align: center;
                       font-family: 'Montserrat', sans-serif; font-size: 12px; color: #8492A6;
                       line-height: 20px;">
          <p style="margin: 0 0 6px;">
            You are receiving this email because you have an account in the
            <a href="https://duos.org" style="color: #00609F; text-decoration: none;">
              Broad Data Use Oversight System (DUOS)
            </a>.
          </p>
          <p style="margin: 0 0 6px;">
            Broad Institute &bull; Merkin Building, 415 Main Street, Cambridge, MA 02142
          </p>
          <#if sendGridUnsubscribeGroupId?? && sendGridUnsubscribeGroupId?has_content>
            <p style="margin: 0;">
              <a href="<%asm_group_unsubscribe_raw_url%>"
                 style="color: #00609F; text-decoration: underline; font-size: 12px;">
                Unsubscribe from DUOS email notifications
              </a>
            </p>
          <#else>
            <p style="margin: 0;">
              To manage DUOS email notifications, please sign in to <a href="https://duos.org/profile" style="color: #00609F; text-decoration: none;">DUOS</a>.
            </p>
          </#if>
        </td>
      </tr>
    </table>
  </div>
</body>
</html>

