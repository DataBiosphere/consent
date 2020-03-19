# DAR Migration

This process moves our current Data Access Request storage from Mongo
to Postgres.

## Stages
* Migrate `dataAccessRequest` collection to new table in postgres
  * https://broadinstitute.atlassian.net/browse/DUOS-226
  * Include migration script
  * We'll store the current `_id` as a referenceId since we're using
    that terminology across the application. 
  * Minimize the impact of this stage by trying to limit the conversions 
    throughout the code. Try to use the original `Document` when possible.
    Use the full `DataAccessRequest` object only if it simplifies things.
  * Migrate away from `Document` to `DataAccessRequest` in follow-on PRs.
  * Remove temporary migration endpoints in follow-on PR.
* Migrate `dataAccessRequestPartials` next.
  * https://broadinstitute.atlassian.net/browse/DUOS-601
* Migrate `counters` next.
  * https://broadinstitute.atlassian.net/browse/DUOS-603
* Migrate `researchPurpose` next. This might be the easiest, it is currently
  unused.
  * https://broadinstitute.atlassian.net/browse/DUOS-602
  
## Workflow Testing
The following workflows have been tested end-to-end

* Export dars from mongo -> import to postgres :heavy_check_mark: 
* Researcher
  * View dar console :heavy_check_mark:
  * Create new dar :heavy_check_mark:
  * Submit new dar :heavy_check_mark:
  * Create partial dar and save :heavy_check_mark:
  * Resubmit partial dar :heavy_check_mark:
  * Review dar :heavy_check_mark:
  * Cancel dar :heavy_check_mark:
* Admin
  * View dar manage page :heavy_check_mark:
  * Create new dar election :heavy_check_mark:
  * Create migrated dar election :heavy_check_mark:
  * Cancel dar election :heavy_check_mark:
  * Create canceled dar election :heavy_check_mark:
  * View dar summary :heavy_check_mark:
  * View dar election :heavy_check_mark:
* Admin - Statistics
  * DUL: All Cases
  * DUL: Reviewed Cases
  * DAR: Should data be granted to this applicant?
    * All Cases 
    * Reviewed Cases
  * DAR: Was the research purpose accurately converted to a structured format? 
    * All Cases 
    * Reviewed Cases
  * DAR: Evaluation of automated matching in comparison with the Data Access Committee decision 
    * Cases reviewed by automated matching 
    * Agreement beween automated maching and Data Access Committee
* Member
  * View dar page (filtered by DAC) :heavy_check_mark:
  * View dar after new election created :heavy_check_mark:
  * Vote on dar :heavy_check_mark:
  * DAR page shows correct votes :heavy_check_mark:
  * Edit vote :heavy_check_mark:
* Chair
  * View dar page
  * Vote on dar
  * Collect votes on dar
  * Final vote on dar
  