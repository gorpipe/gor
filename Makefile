SHORT_NAME = gor

BRANCH = $(shell git rev-parse --abbrev-ref HEAD)
COMMIT_HASH = $(shell git rev-parse --short HEAD)
CURRENT_VERSION = $(shell cat VERSION)
CURRENT_TAG_VERSION = "v${CURRENT_VERSION}"

help:  ## This help.
	@grep -E '^[a-zA-Z0-9_-]+:.*?#*.*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?#+"}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'

#
# Common build targets - just the most common gradle targets.
# 

clean:  ## Clean the build env.
	./gradlew clean

build: ## Create local installation.
	./gradlew installDist

test:  ## Run all tests.
	./gradlew test slowTest integrationTest

#
# Local testing
#

publish-local:  ## Publish libraries locally (mavenLocal), then compile services with -PuseMavenLocal
	./gradlew publishToMavenLocal

docker-build: build ## Build all docker images
	docker build .


#
# Release targets
#


update-master:   ## Update master and its submodules
	git checkout master
	git pull
	git submodule update --init --recursive

update-branch:   ## Update the current branch
	git pull
	git submodule update --init --recursive


update-master-version: update-master    ## Update version on the master branch, assumes NEW_VERSION is passed in.
	@if [ -z "${NEW_VERSION}" ]; then { echo "ERROR:  NEW_VERSION should be set! Exiting..."; exit 1; }; fi

	# Update version on master
	git checkout -b "Update_master_version_to_${NEW_VERSION}"
	git push -u origin "Update_master_version_to_${NEW_VERSION}"

	echo "${NEW_VERSION}" > VERSION
	git add VERSION
	git commit -m "Updated version to ${NEW_VERSION} on master."
	git push


#
#  Old style of realeasing.
#  Create release branch,  tag
#

# Create a release branch and update version number on master, assumes BRANCH_VERSION is passed in.
# Note we don't update the version number on the branch, but you can do that using release.
create-release-branch-old: update-master      ## DEPRECATED: 	Create a release branch assumes BRANCH_VERSION is passed in.
	@if [ -z "${BRANCH_VERSION}" ]; then { echo "ERROR:  BRANCH_VERSION should be set! Exiting..."; exit 1; }; fi

	@echo "Creating new release branch ${BRANCH_VERSION}"
	git checkout -b release/v${BRANCH_VERSION}

	./gradlew allDeps --write-locks
	find . -name '*.lockfile' | grep -v '/build/' | xargs git add
	git commit -m "Creating release branch ${BRANCH_VERSION}, updating dependency locking"

	git push -u origin release/v${BRANCH_VERSION}

# Update version on new feature branch off the release branch, assumes BRANCH_VERSION and NEW_VERSION is passed in.
# Then create a pull request for merging into the release b
release-old:  ## DEPRECATED: Update version on new feature branch off the release branch, assumes BRANCH_VERSION and NEW_VERSION is passed in.
	@if [ -z "${BRANCH_VERSION}" ]; then { echo "ERROR:  BRANCH_VERSION should be set! Exiting..."; exit 1; }; fi
	@if [ -z "${NEW_VERSION}" ]; then { echo "ERROR:  NEW_VERSION should be set! Exiting..."; exit 1; }; fi

	# Check out the branch
	git checkout release/v${BRANCH_VERSION}
	git pull
	git submodule update --init --recursive

	git checkout -b "Release_${NEW_VERSION}_on_${BRANCH_VERSION}"
	git push -u origin "Release_${NEW_VERSION}_on_${BRANCH_VERSION}"

	echo "${NEW_VERSION}" > VERSION
	git add VERSION

	./gradlew allDeps --update-locks com.wuxinextcode.gor:*
	find . -name '*.lockfile' | grep -v '/build/' | xargs git add

	git commit -m "Created new release ${NEW_VERSION} on ${BRANCH_VERSION}."
	git push


#
# Release from release branch.
#

# Create a release branch with library locks and update version info.
create-release-branch: update-master  ## Create a release branch, assumes BRANCH_VERSION is passed in.
	@if [ -z "${BRANCH_VERSION}" ]; then { echo "ERROR:  BRANCH_VERSION should be set! Exiting..."; exit 1; }; fi

	# Create the release branch.
	@echo "Creating new release branch release/v${BRANCH_VERSION}"
	git checkout -b release/v${BRANCH_VERSION}

	# Create the library locks
	./gradlew allDeps --write-locks
	find . -name '*.lockfile' | grep -v '/build/' | xargs git add
	git commit -m "Creating release branch ${BRANCH_VERSION}, updating dependency locking"
	git push -u origin release/v${BRANCH_VERSION}

	echo "${BRANCH_VERSION}.0" > VERSION
	git add VERSION

	# Commit and push to the branch
	git commit -m "Updating version to ${BRANCH_VERSION}.0 on release/v${BRANCH_VERSION}."
	git push

	# Must also Call update-master-version.


update-release-version:  ## Update version on the development branch, assumes BRANCH_VERSION, NEW_VERSION is passed in.
	@if [ -z "${BRANCH_VERSION}" ]; then { echo "ERROR: BRANCH_VERSION should be set! Exiting..."; exit 1; }; fi
	@if [ -z "${NEW_VERSION}" ]; then { echo "ERROR:  NEW_VERSION should be set! Exiting..."; exit 1; }; fi

	# Check out the release branch
	git checkout release/v${BRANCH_VERSION}
	git pull
	git submodule update --init --recursive

	# Update the version numbers
	echo "${NEW_VERSION}" > VERSION
	git add VERSION

	# Commit and push to the branch
	git commit -m "Updated version to ${NEW_VERSION} on on release/v${BRANCH_VERSION}."
	git push


release-from-release:  ## Release from the given release branch.  Assumes BRANCH_VERSION is passed in.
	@if [ -z "${BRANCH_VERSION}" ]; then { echo "ERROR: BRANCH_VERSION should be set! Exiting..."; exit 1; }; fi

	# Check out release the branch
	git checkout release/v${BRANCH_VERSION}
	git pull

	git tag -a ${CURRENT_TAG_VERSION} -m "Releasing gor-services ${CURRENT_TAG_VERSION}"
	git push origin $(CURRENT_TAG_VERSION)

release-from-master:  ## Release from master.
	git checkout master
	git pull

	git tag -a ${CURRENT_TAG_VERSION} -m "Releasing gor-services ${CURRENT_TAG_VERSION}"
	git push origin $(CURRENT_TAG_VERSION)

#
# Release directly from main.
#

# TBD

check-for-library-updates:  ## Check for available library updates
	./gradlew dependencyUpdates