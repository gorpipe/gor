SHORT_NAME = gor

BRANCH = $$(git rev-parse --abbrev-ref HEAD)
COMMIT_HASH = $$(git rev-parse --short HEAD)

help:  ## This help.
	@grep -E '^[a-zA-Z0-9_-]+:.*?#*.*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?#+"}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'

#
# Common build targets - just the most common gradle targets.
#

clean:  ## Clean the build env.
	./gradlew clean

build: ## Create local installation.
	./gradlew installDist

all-test:  ## Run all tests.
	./gradlew test slowTest integrationTest

compile-all-with-warnings:  ## Compile code and tests.
	./gradlew --rerun-tasks --console=plain --warning-mode all clean compileJava compileTestJava

build-doc:  ## Build documentation
	./gradlew :documentation:publishToMavenLocal -Pinclude.documentation

build-doc-os-aarch:  ## Build documentation on osx using aarch
	./gradlew :documentation:publishToMavenLocal -Pinclude.documentation -Psphinx.binaryUrl="https://github.com/trustin/sphinx-binary/releases/download/v0.8.1/sphinx.osx-x86_64"

#
# Local testinga
#

publish-local:  ## Publish libraries locally (mavenLocal), then compile services with -PuseMavenLocal
	./gradlew publishToMavenLocal -x documentation:publishToMavenLocal

publish-maven-central:   ## Publish to maven central
	./gradlew -PpublishToMavenCentral publish

docker-build: build ## Build all docker images
	docker build .


#
# Git helpers.  Releases are created by tagging on GitHub, see CONTRIBUTING.md#release.
#

update-branch:   ## Update the current branch
	git pull
	git submodule update --init --recursive

gitversion:  ## Get current version (derived from git tags)
	@./gradlew -q printVersion | tail -1


#
# Misc
#

dependencies-check-for-updates:  ## Check for available library updates (updates versions.properties)
	./gradlew refreshVersions
