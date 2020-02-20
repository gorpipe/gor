SHORT_NAME = gor

BRANCH = $(git rev-parse --abbrev-ref HEAD)
COMMIT_HASH = $(git rev-parse --short HEAD)
CURRENT_VERSION = $(cat version)

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


update-develop:   ## Update develop and its submodules
	git checkout develop
	git pull
	git submodule update --init --recursive

update-branch:   ## Update the current branch
	git pull
	git submodule update --init --recursive


# Create a release branch and update version number on develop, assumes BRANCH_VERSION is passed in.
# Note we don't update the version number on the branch, but you can do that using release.
create-release-branch: update-develop      ## Create a release branch assumes BRANCH_VERSION is passed in.
	@if [ -z "${BRANCH_VERSION}" ]; then { echo "ERROR:  BRANCH_VERSION should be set! Exiting..."; exit 1; }; fi

	@echo "Creating new release branch ${BRANCH_VERSION}"
	git checkout -b release/v${BRANCH_VERSION}

	./gradlew allDeps --write-locks
	find . -name *.lockfile | grep -v '/build/' | xargs git add
	git commit -m "Creating release branch ${BRANCH_VERSION}, updating dependency locking"

	git push -u origin release/v${BRANCH_VERSION}

update-develop-version: update-develop    ## Update version on the development branch, assumes NEW_VERSION is passed in.
	@if [ -z "${NEW_VERSION}" ]; then { echo "ERROR:  NEW_VERSION should be set! Exiting..."; exit 1; }; fi

	# Update version on develop
	git checkout -b "Update_develop_version_to_${NEW_VERSION}"
	git push -u origin "Update_develop_version_to_${NEW_VERSION}"

	echo "${NEW_VERSION}" > version
	git add version
	git commit -m "Updated version to ${NEW_VERSION} on develop."
	git push

# Update version on new feature branch off the release branch, assumes BRANCH_VERSION and NEW_VERSION is passed in.
# Then create a pull request for merging into the release b
release:  ## Update version on new feature branch off the release branch, assumes BRANCH_VERSION and NEW_VERSION is passed in.
	@if [ -z "${BRANCH_VERSION}" ]; then { echo "ERROR:  BRANCH_VERSION should be set! Exiting..."; exit 1; }; fi
	@if [ -z "${NEW_VERSION}" ]; then { echo "ERROR:  NEW_VERSION should be set! Exiting..."; exit 1; }; fi

	# Check out the branch
	git checkout release/v${BRANCH_VERSION}
	git pull
	git submodule update --init --recursive

	git checkout -b "Release_${NEW_VERSION}_on_${BRANCH_VERSION}"
	git push -u origin "Release_${NEW_VERSION}_on_${BRANCH_VERSION}"

	echo "${NEW_VERSION}" > version
	git add version

	./gradlew allDeps --update-locks com.wuxinextcode.gor:*
	find . -name *.lockfile | grep -v '/build/' | xargs git add

	git commit -m "Created new release ${NEW_VERSION} on ${BRANCH_VERSION}."
	git push