# Contributing to GOR

For developers of GOR, people who want to contribute to the GOR codebase or documentation, or people who want to install from source and make local changes to their copy of GOR.

For this to work you'll need to have merge rights to master otherwise te push from release script will fail.

## Reporting an Issue
TBD

## Build
TBD

## Release

This section is for developers of GOR and describes how releases are created from our Gitlab environment.

The steps are as follows:

1. Review the operation dashboard.
2. Update the relevant milestone.
3. Create the release.
4. Close the milestone.
5. Notify Software Development
6. Create a release on GitHub (optional)
7. Create a release of Gor Services (optional)

Each one is described in more detail below.

Notes on versioning:  
1. GOR uses semantic versioning, with `<major>.<minor>.<patch>` format.  
2. We use `-SNAPSHOT` postfix to indicate development version.  
3. We update the version at the time of release so for example `10.2.3-SNAPSHOT` is a development version on top on released version `10.2.3`.  Next version will be `10.2.4`,`10.3.0` or `11.0.0`

### Reviewing Operations Dashboard

Before creating a GOR release, make sure that there are no issues in the Operations Dashboards related to GOR: https://gitlab.com/-/operations

### Updating the Milestone

After last release there should be milestone created for this release under [>Issues>Milestones](https://gitlab.com/wuxi-nextcode/wxnc-gor/gor/-/milestones).

Review all merge requests since the last release and make sure that they along with their underlying issues are tagged with the milestone for the upcoming release.

Also, review all issues with merge requests that have been merged and make sure that they are tagged with the appropriate milestone ([Closed issues without milestone](https://gitlab.com/wuxi-nextcode/wxnc-gor/gor/-/issues/?sort=created_date&state=closed&milestone_title=None&first_page_size=20))

Make sure that the issues are bulked edited and make sure that the status is Closed, tagged with **Status:Done** and milestone set to the current milestone. After saving goto the milestone and make sure that all issues are tagged with either **Feature Request** or **Bug**. If not then edit the issue to include the correct tag as this is reflected in the release notes.

### Creating a Release
When creating the actual release we can either release directly from master (now preferred) or create release branch and release from there.
Using a release branch (with dependency locks) is preferred only if we expect that we will be creating patches on very old releases.

#### Create Release Directly from Master - Now Preferred

Make sure have an open milestone with all the issues, then call:

`make release-milestone-from-master MILESTONE=1.1.1`

this will update the version, create release notes, create the release by tagging, and close the milestone.

In case of an error you need to roll back the release.
  1. Goto the [tags](https://gitlab.com/wuxi-nextcode/wxnc-gor/gor/-/tags) and remove the tag with the current version
  2. Goto the [milestone](https://gitlab.com/wuxi-nextcode/wxnc-gor/gor/-/milestones?sort=due_date_desc&state=closed) and reopen the curent milestone.
  3. Fix any issue related to error
  4. Rerun the make script
     
This can also be done manually.

#### Create a Patch Directly from Master - Now Preferred

Exactly like creating a release except only the patch number should change when updating the version number.

#### Create Release From a Release Branch - Older

Open a shell within in the folder where the GOR repository lies and enter the following with the release version correctly updated:

1. `make create-release-branch BRANCH_VERSION=1.1`
2. `make update-master-version NEW_VERSION=1.2.0-SNAPSHOT`  This creates a branch with the changes thats need to be merged manually!!
3. Open a browser and [create a tag on the release branch](https://gitlab.com/wuxi-nextcode/wxnc-gor/gor/-/tags), put the release notes in the Release Notes field.  Alternatively we can tag using  `make release-from-release BRANCH_VERSION=1.1`.

#### Create Patch From Release Branch - Older

For patches we have two cases:
- We will usually have the change made to master and then cherry picked to the release branches.  In this case we will have only the pull request to master, but we still have to mark the issue with all the releases the fix went into.
- Sometimes we must create pull requests for the patch versions too and in that case the issue will have multiple branches and pull request.

After updating the release branch, open a shell within in the folder where the GOR repository lies and enter the following with the release version correctly updated:

1. `make update-release-version BRANCH_VERSION=1.1 NEW_VERSION=1.1.1`
2. Open a browser and [create a tag on the release branch](https://gitlab.com/wuxi-nextcode/wxnc-gor/gor/-/tags), put the release notes in the Release Notes field.  Alternatively we can tag using  `make release-from-release BRANCH_VERSION=1.1`.

#### Closing Out Milestone - Older

If creating the release was successful, update the milestone by adding a description, setting Due Date to today and press the "Close milestone" button. Then create a new milestone for the next release.

### Notify Software Development

Notify software deveplopment that a release has been creating by posting on the #sdev that the release has been created with the features listed under the milestone. Include links to the milestone and the GitHub release.

### Creating a Release on GitHub

The release branch and tag for the release should automatically be mirrored to the GOR repository on [GitHub](https://github.com/gorpipe/gor). A release on GitHub is created going through the following steps:

- Under [Releases](https://github.com/gorpipe/gor/releases), press button "Draft a new release"
- Select the tag for the new release and set that as the release title as well
- In the description, use the same description as used in the milestone in GitLab
- For the binaries go into [Package Registry](https://gitlab.com/wuxi-nextcode/wxnc-gor/gor/-/packages) and under org/gorpipe/gor-tools for the release download file `gor-scripts-1.1.0-dist.zip`
- Attach file `gor-scripts-1.1.0-dist.zip` by dragging it to the "Attach binaries" box
- After uplading the binaries is complete, press the "Publish release" button

Make sure to update the release version as appropriate.

### Create a Release of GOR Services

To update our customer environments, we will need to create a new release of GOR Services including this GOR release. That process is documented in the [GOR Services repo](https://gitlab.com/wuxi-nextcode/wxnc-gor/gor-services/-/blob/master/CONTRIBUTING.md).

