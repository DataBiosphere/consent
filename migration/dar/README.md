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
  * View dar manage page
  * Create dar election
  * Cancel dar election
  * View dar summary
  * View dar election
* Member
  * View dar page
  * Vote on dar
* Chair
  * View dar page
  * Vote on dar
  * Collect votes on dar
  * Final vote on dar
  