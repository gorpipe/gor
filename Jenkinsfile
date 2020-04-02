#!/usr/bin/env groovy
node {
    def buildImage = docker.image('nextcode/builderimg-java:latest')
    stage('Pull build image') {
        buildImage.pull()
    }
    stage('SCM Checkout') {
        checkout scm
    }

    def gradleHome = "${WORKSPACE}/.gor_gradle_home"
    stage('Prepare build environment') {
        sh "mkdir -p ${gradleHome}"
    }

    def gradleOpts = "-g ${gradleHome} --console=plain"
    def curlArtifactoryCredentials = ""

    def keystorePath = "${WXNC_CODESIGN_KEYSTORE_PATH}"
    def keystoreDir = keystorePath.substring(0, keystorePath.lastIndexOf("/"))
    def keystoreFile = keystorePath.substring(keystorePath.lastIndexOf("/"))
    def containerKeystoreDir = "/opt/nextcode/codesign"

    withCredentials([usernamePassword(credentialsId: 'CodesignKeystore', usernameVariable: 'KEYSTORE_ALIAS', passwordVariable: 'KEYSTORE_PASS'),
                     usernamePassword(credentialsId: 'ArtifactoryBuild', usernameVariable: 'ARTIFACTORY_USER', passwordVariable: 'ARTIFACTORY_PASSWORD'),
                     string(credentialsId: 'GOR_SONARQUBE_API_KEY', variable: 'SONARQUBE_API_KEY'),
                     string(credentialsId: 'BITBUCKET_TEAM_API_KEY', variable: 'BITBUCKET_TEAM_API_KEY'),]) {
        gradleOpts += " -Partifactory_user=${ARTIFACTORY_USER} -Partifactory_password=${ARTIFACTORY_PASSWORD}"
        gradleOpts += " -Pkeystore_file=${containerKeystoreDir}${keystoreFile} -Pkeystore_pass=${KEYSTORE_PASS}"

        // We only need the following when building pull requests
        if (env.CHANGE_ID) {
            gradleOpts += " -Dsonar.bitbucket.repoSlug=gor -Dsonar.bitbucket.accountName=nextcode-health -Dsonar.bitbucket.teamName=nextcode-health"
            gradleOpts += " -Dsonar.bitbucket.apiKey=${BITBUCKET_TEAM_API_KEY} -Dsonar.bitbucket.branchName=${BRANCH_NAME} -Dsonar.bitbucket.pullRequestId=${CHANGE_ID}"
            gradleOpts += " -Dsonar.exclusions=**/*.png,**/*.gif,**/*.GIF,**/*.ttf,**/*.otf,**/*.eot,**/Thumbs.db"
            gradleOpts += " -Dsonar.dependencyCheck.xmlReportPath=${WORKSPACE}/build/reports/dependency-check-report.xml"
            gradleOpts += " -Dsonar.dependencyCheck.htmlReportPath=${WORKSPACE}/build/reports/dependency-check-report.html"
            gradleOpts += " -Dsonar.login=${SONARQUBE_API_KEY} -Dsonar.analysis.mode=preview"
        }

        curlArtifactoryCredentials = "-u ${ARTIFACTORY_USER}:${ARTIFACTORY_PASSWORD}"
    }

    buildImage.inside("-u ${DOCKER_UID}:${DOCKER_GID} --env TZ=UTC --dns 10.3.1.10 --dns 10.3.1.11 --dns-search nextcode.local --volume=/var/run/docker.sock:/var/run/docker.sock --volume=${keystoreDir}:${containerKeystoreDir}") {
        stage('Build') {
            sh "./gradlew ${gradleOpts} clean classes testClasses installDist"
        }

        try {
            stage('Unit Tests') {
                sh "./gradlew ${gradleOpts} --continue test"
            }

            stage('Slow Unit Tests') {
                sh "./gradlew ${gradleOpts} --continue slowTest"
            }

            // If this is a pull request, we run our security scan
            /* Temporarily disable security scan GOP-233.
            if (env.CHANGE_ID) {
                stage('Code Security Scan') {
                    sh "./gradlew ${gradleOpts} --continue :dependencyCheckAggregate -x test -x slowTest :sonar"
                }
            }
            */

            // We only publish if tests ran successfully
            def publishTo = ""
            def isSnapshotBuild = false
            def version = readFile('VERSION').trim()
            def dockerTags = [version]

            if (env.BRANCH_NAME == "master") {
                publishTo = "libs-snapshot-local"
                // On master we just push the latest build as a snapshot each time we build
                isSnapshotBuild = true
                dockerTags << env.BRANCH_NAME
            }  else if (env.BRANCH_NAME.contains("release/")) {
                publishTo = "libs-staging-local"
            }

            // Must be initialized to true, set to false if artifact already exists
            // and it's existence matters for the branch.
            // Used to control push to Artifactory and Docker Hub.
            def doPublish = true

            // We only publish if we have a defined publishTo target
            if (publishTo.length() > 0) {

                // We only run integration tests on branches that are publishable
                stage('Integration Tests') {
                    sh "./gradlew ${gradleOpts} --continue integrationTest"
                }

                // We only create documentation on branches that are publishable.
                stage('Documentation') {
                    sh "./gradlew ${gradleOpts} :documentation:jar"
                }

                // Given that the integration tests ran fine, we'll go ahead and attempt to publish

                stage('Publish') {

                    // If set, then we only push a new artifact if no previous build on the same version has been submitted.
                    if (!isSnapshotBuild) {
                        echo "Checking if it is ok to publish version " + version + " to " + publishTo
                        def shOutput = sh(
                                script: "curl -s ${curlArtifactoryCredentials} \"https://dl.nextcode.com/artifactory/api/search/versions?g=com.nextcode.gor&a=gor-scripts&r=" + publishTo + "\"",
                                returnStdout: true)

                        echo shOutput

                        // Attempt to publish if version was not found
                        if (!shOutput.contains("\"" + version + "\"")) {
                            doPublish = true
                        } else {
                            // we found a version that matches our version, so do not publish
                            echo "Existing version found. Publish to Artifactory will not happen!"
                            doPublish = false
                        }
                    }

                    // Deploy to Artifactory if everything is ok
                    if (doPublish) {
                        echo "Publishing " + version + " to " + publishTo + " in Artifactory"

                        sh "./gradlew ${gradleOpts} -Partifactory_repo=" + publishTo + " artifactoryPublish -x test -x slowTest -x integrationTest"

                        def sha1 = sh(script: "git rev-parse HEAD", returnStdout: true).trim()

                        // Always tag with a version
                        build job: '../TagBranch', wait: false, parameters: [
                                [$class: 'StringParameterValue', name: 'git_sha', value: sha1],
                                [$class: 'StringParameterValue', name: 'tag', value: 'v' + version],
                                [$class: 'StringParameterValue', name: 'message', value: 'Tagging with version from Jenkins2'],
                                [$class: 'StringParameterValue', name: 'repo', value: 'gor']
                        ]
                    }
                }

            }
        } finally {
            stage('Publish Test Reports') {
                step([$class: 'JUnitResultArchiver', testResults: '**/build/test-results/*/TEST-*.xml'])
                step([$class       : 'JacocoPublisher',
                      execPattern  : '**/build/jacoco/*.exec',
                      classPattern : '**/build/classes/java/main,**/build/classes/scala/main',
                      sourcePattern: '**/src/main/java,**/src/main/scala',
                ])
            }
        }
    }
}
