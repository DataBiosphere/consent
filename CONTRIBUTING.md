# Contributing

## Coding Style and Guides
* [IntelliJ for Power Users](https://github.com/DataBiosphere/terra/blob/main/docs/dev-guides/intellij.md)
* [Java Style Guide](https://github.com/DataBiosphere/terra/blob/main/docs/dev-guides/java-style.md)
  * We strive to adhere to the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html) where possible
  * We use Codacy for static analysis

## Opening a Pull Request

You most likely want to do your work on a feature branch based on develop. 
There is no explicit naming convention; we usually use some combination of 
the JIRA issue number and something alluding to the work we're doing.

When you open a pull request, add the JIRA issue number and a risk assessment 
(e.g. `[DUOS-1234][risk=low]`) to the PR title. This will make a reference 
from JIRA to the GitHub issue. Add a brief description of your changes above 
the PR checkbox template.

This is also a good opportunity to check that the acceptance criteria for your 
JIRA ticket exists and is met. Check with your PO if you have any questions 
there. You should also fill out a summary to go in the release notes and some 
instructions on what QA should be looking at.

The checkboxes in the PR are important reminders to you, the developer. Please 
be conscientious and run through them when you open a PR.

## PR approval process

If your PR is particularly complex it can be helpful to add some commentary, 
either in the description or line-by-line in the GitHub PR view. In the 
latter case, consider whether those comments should be in the code itself.

You should get review from two people (either through GitHub's request-review 
feature, or by assigning the PR to them); one of them should probably be your 
tech lead, though ask. Do chase your reviewers (or find others) if they're 
slow; we don't like to let PRs linger. If you get PR feedback it's back to 
you to address it and then nudge your reviewers for re-review.

Your PR is ready to merge when all of the following things are true:

1. At least one, preferably two, reviewers have thumbed (or otherwise approved) your PR
2. If your change is user-facing, your PO has seen it and signed off
3. All tests pass

## API changes

All changes to the API _must_ be documented in Swagger.


### Breaking API changes

We strive to minimize breaking changes to the API, but sometimes they're unavoidable. In such cases we need to let our API consumers know ahead of time so their code doesn't suddenly stop working.

If you're making breaking changes to the API, do the following:

0. Try really hard not to make a breaking change to the API.
1. Check in with Comms to explain the change and the impact on users.
2. Write an email to users explaining the change and send it least a few days BEFORE the release goes out. You'll need to explain the change and what users need to do to update their code. Get Comms signoff on the wording.
3. Get someone Suitable to send the email to api-users@firecloud.org.

## FISMA documentation changes

If you're making changes to authentication, authorization, encryption, or audit 
trails, you should check in with AppSec to see if our security documentation 
should be updated.
