def String getGradleArtifactId() {
    return sh(script: './gradlew properties -q | grep "^name:" | awk \'{print $2}\'', returnStdout: true)
}

def String getGradleReleaseVersion() {
    return sh(script: './gradlew properties -q | grep "^version:" | awk \'{print $2}\'', returnStdout: true).split("-")[0]
}

def String getGradleNextDevelopmentVersion() {
    def splittedReleaseVersion = getGradleReleaseVersion().split('\\.')
    splittedReleaseVersion[1] = (splittedReleaseVersion[1].toInteger() + 1).toString()
    return splittedReleaseVersion.join(".") + "-SNAPSHOT"
}

def String getEmailSubject(Boolean success) {
    buildResult = ''
    if (success) {
        buildResult = 'Erfolgreich'
    } else {
        buildResult = 'Fehlgeschlagen'
    }

    return "${env.JOB_NAME} Build #${env.BUILD_NUMBER} ${buildResult}"
}

def String getEmailBody(Boolean success) {
    buildResult = ''
    errorReason = ''
    if (success) {
        buildResult = "${env.JOB_NAME} Build #${env.BUILD_NUMBER} erfolgreich."
    } else {
        buildResult = "${env.JOB_NAME} Build #${env.BUILD_NUMBER} fehlgeschlagen."

        errorLog = currentBuild.rawBuild.getLog(50).findAll {
            line -> line.contains("[ERROR]")
        }.join('\n')
        errorReason = "Fehlergrund:\n${errorLog}"
    }

    startedBy = "Build gestartet von: ${currentBuild.getBuildCauses('hudson.model.Cause$UserIdCause')}"
    wrap([$class: 'BuildUser']) {
        startedBy = "Build gestartet von: ${env.BUILD_USER}, ${env.BUILD_USER_EMAIL}"
    }

    committer = sh(returnStdout: true, script: "git log -1 --pretty=format:'%an'").trim()
    committerEmail = sh(returnStdout: true, script: "git log -1 --pretty=format:'%ae'").trim()
    commitMessage = sh(returnStdout: true, script: "git log -1 --pretty=%B").trim()
    commitHash = sh(returnStdout: true, script: "git log -n 1 --pretty=format:'%h'").trim()
    commitChanges = sh(returnStdout: true, script: 'git diff-tree --no-commit-id --name-status -r HEAD').trim()

    return """
        Salli hä vom Butler!
        ${buildResult}
        ${startedBy}
        ${env.BUILD_URL}

        Commit:
        #${commitHash}, von Mitarbeiter ${committer}, ${committerEmail}
        ${commitMessage}

        Änderungen:
        ${commitChanges}

        ${if (!success) errorReason else ''}
        """
}

def String getCommitHash() {
    commitHash = sh(returnStdout: true, script: "git log -n 1 --pretty=format:'%h'").trim()
    return """${commitHash}"""
}

pipeline {
    agent any
    tools {
        jdk "OpenJDK11.0.3"
    }
    options {
        buildDiscarder(logRotator(numToKeepStr: '5'))
        disableConcurrentBuilds()
        timestamps()
    }
    environment {
        ANDROID_SDK_ROOT="/usr/lib/android-sdk"
    }
    stages {
        stage('Test') {
            steps {
                sh "chmod u+x ./gradlew"
                sh "./gradlew test"
            }
        }
        stage('Deploy Debug Version') {
            when {
                expression {
                    !params.isRelease
                }
            }
            steps {
                sh "./gradlew assembleDebug"
                sh "mv ./app/build/outputs/apk/debug/app-debug.apk /var/www/lukas-scheerer.de/downloads/bubble-penetration.apk"
            }
        }
        stage('Release') {
            when {
                expression {
                    params.isRelease &&
                            params.releaseVersion?.trim() &&
                            params.developmentVersion?.trim() &&
                            params.scmTag?.trim()
                }
            }
            steps {
                withCredentials([usernamePassword(credentialsId: "gitea", passwordVariable: 'GIT_PASSWORD', usernameVariable: 'GIT_USERNAME')]) {
                    sh """
                        git config --global credential.username ${GIT_USERNAME}
                        git config --global credential.helper "!echo password=${GIT_PASSWORD}; echo"
                        ./gradlew release -Prelease.useAutomaticVersion=true -Prelease.releaseVersion=${params.releaseVersion} -Prelease.newVersion=${params.developmentVersion}
                    """
                }
                sh "./gradlew assembleRelease"
                sshagent(credentials: ['ss']) {
                    sh "scp -P 63787 ./app/build/outputs/apk/release/app-release.apk jenkins@router.spicysources.de/var/www/www.spicysources.de/downloads/games/bubble-penetration/bubble-penetration-latest.apk"
                }
            }
        }
    }
    post {
        success {
            mail subject: "${getEmailSubject(true)}", body: "${getEmailBody(true)}", from: 'jenkins@spicysources.de', to: 'lukasscheerer@spicysources.de, davidlink@spicysources.de'
        }
        failure {
            mail subject: "${getEmailSubject(false)}", body: "${getEmailBody(false)}", from: 'jenkins@spicysources.de', to: 'lukasscheerer@spicysources.de, davidlink@spicysources.de'
        }
        always {
            script {
                properties([
                    parameters([
                        booleanParam(name: 'isRelease', defaultValue: false, description: 'Releases the project, creates a tag in the repository and deploys a release artifact.'),
                        string(name: 'releaseVersion', defaultValue: getGradleReleaseVersion(), description: 'Version to use for the released project.'),
                        string(name: 'developmentVersion', defaultValue: getGradleNextDevelopmentVersion(), description: 'Next version to use for development.'),
                    ])
                ])
            }
        }
    }
}