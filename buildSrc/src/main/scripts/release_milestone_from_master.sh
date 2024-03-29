#!/bin/bash
# -----------------------------
# Script to release given a milestone number.
#
#  release_milestone_from_master.sh <project id> <milestone> [<update version>]
#
# The script:
#
# 1. Validates that milestone exists and is open.
# 2. if <update version> is true then set the VERSION file equal to the milestone and pushes it.
# 3. Creates release notes from the milestone.
# 4. Creates a release (tags the the current master branch)
# 5. Closes the milestone.
# -----------------------------

set -e

PROJECT_ID=$1
MILESTONE=$2
DRY_RUN=${3:-FALSE}

if [ -z "${PROJECT_ID}" ]; then { echo "ERROR: PROJECT_ID should be passed in! Exiting..."; exit 1; }; fi
if [ -z "${MILESTONE}" ]; then { echo "ERROR: MILESTONE should be passed in! Exiting..."; exit 1; }; fi

TOKEN=$(cat ~/.gradle/gradle.properties | grep -e '^gitlab_token=' | cut -d'=' -f2)
MILESTONE_JSON=$(curl --silent --header "PRIVATE-TOKEN: ${TOKEN}" "https://gitlab.com/api/v4/projects/${PROJECT_ID}/milestones?title=${MILESTONE}" | sed  's/\n/ /g')
MILESTONE_ID=$(printf "%s" ${MILESTONE_JSON} | jq '.[].id')
MILESTONE_STATE=$(printf "%s" ${MILESTONE_JSON} | jq '.[].state' | sed -e 's/^"//' -e 's/"$//')
MILESTONE_DESCRIPTION=$(printf "%s" ${MILESTONE_JSON} | jq '.[].description' | sed -e 's/^"//' -e 's/"$//')

if [ -z "${MILESTONE_ID}" ]; then { echo "Milestone ${MILESTONE} does not exists, create the milestone and update the issues.";  exit 1; }; fi
if [[ "${MILESTONE_STATE}" != "active" ]]; then { echo "Milestone ${MILESTONE} has to be open, make sure you have the right milestone.";  exit 1; }; fi

#
# Update the version
#
if [[ "${DRY_RUN}" != "TRUE" ]]; then
  # Check out the release branch
  git checkout master
  git pull

  # Update the version numbers
  echo "${MILESTONE}" > VERSION
  git add VERSION

  # Commit and push to the branch
  git commit -m "Updated version to ${MILESTONE}"
  git push
else
  echo "Release version: ${MILESTONE}"
fi

#
# Tag
#

RELEASE_NOTES=""
RELEASE_NOTES=${RELEASE_NOTES}$( [ ! -z ${MILESTONE_DESCRIPTION} ] && printf "### Description:\\\\r\\\\n%s\\\\r\\\\n" "${MILESTONE_DESCRIPTION}" || printf "")
RELEASE_NOTES=${RELEASE_NOTES}$(curl --silent --header "PRIVATE-TOKEN: ${TOKEN}" "https://gitlab.com/api/v4/projects/${PROJECT_ID}/milestones/${MILESTONE_ID}/issues" \
   | jq -r '.[]  | select( .labels | contains(["TechDebt"]) | not) | "\( .title | (split("\"") | join(" "))) (\(.labels[0]))"' \
   | awk 'BEGIN {ORS=""; print "### Issues\\r\\n"} {print "* " $0 "\\r\\n"} END {print ""}')
printf "%s" $RELEASE_NOTES
printf "\n"

DATA_JSON='{ "name": "'"v${MILESTONE}"'", "tag_name": "'"v${MILESTONE}"'", "ref": "master", "description": "'"${RELEASE_NOTES}"'", "milestones": ["'"${MILESTONE}"'"] }'

printf "%s" $DATA_JSON | jq
if [[ "${DRY_RUN}" != "TRUE" ]]; then
  curl  --silent --header 'Content-Type: application/json' --header "PRIVATE-TOKEN: ${TOKEN}" --data "${DATA_JSON}" --request POST "https://gitlab.com/api/v4/projects/${PROJECT_ID}/releases" | jq
fi
#
# Close the milestone.
#
if [[ "${DRY_RUN}" != "TRUE" ]]; then
  curl --verbose --header "PRIVATE-TOKEN: ${TOKEN}" -X PUT "https://gitlab.com/api/v4/projects/${PROJECT_ID}/milestones/${MILESTONE_ID}?state_event=close"
fi
#
# Update the version
#
# Comment this out for now, we are experimenting with just let the VERSION file refer to latest released version.
#if [[ "${DRY_RUN}" != "TRUE" ]]; then
#  # Update the version numbers
#  NEW_VERSION="$(buildSrc/src/main/scripts/version.sh ${MILESTONE} feature)-SNAPSHOT"
#  echo ${NEW_VERSION} > VERSION
#  git add VERSION
#
#  # Commit and push to the branch
#  git commit -m "Updated version to ${NEW_VERSION}"
#  git push
#else
#  echo "New snapshot version: $(buildSrc/src/main/scripts/version.sh ${MILESTONE} feature)-SNAPSHOT"
#fi