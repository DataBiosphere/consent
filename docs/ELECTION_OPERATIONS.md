# DAR Election Operations

## Ownership

DAC Chairpersons own opening and canceling DAR collection elections. Admin access is read-only for
these actions: Admins cannot use the collection election or cancellation APIs, and Admin collection
summaries do not advertise `Open` or `Cancel` actions.

Researchers may cancel only their own DAR collection before an election exists. Once DAC review has
started, cancellation belongs to the Chairperson for each affected DAC.

## Exceptional cleanup

When an election needs exceptional cleanup, DUOS Support should:

1. Identify the affected DAR collection and datasets.
2. Escalate the request to the Chairperson of each governing DAC.
3. Have the Chairperson use the normal DAC workflow to cancel or reopen the relevant elections.
4. If no active Chairperson can perform the action, coordinate with the DAC owner to appoint a
   replacement Chairperson before retrying the normal workflow.
5. Escalate suspected data corruption or a workflow defect to the Data Team as an incident. Any
   direct data remediation must use the team's reviewed, audited operational process; Admin API
   access is not a fallback.

This keeps election decisions with the governing DAC while preserving a support path for cases that
previously relied on Admin actions.
