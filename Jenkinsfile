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
    environment {
        ANDROID_SDK_ROOT="/usr/lib/android-sdk"
    }
    parameters {
        booleanParam(name: 'isRelease', defaultValue: false, description: 'Releases the project, creates a tag in the repository and deploys a release artifact.')
        string(name: 'releaseVersion', defaultValue: 'X.X.X', description: 'Version to use for the released project.')
        string(name: 'developmentVersion', defaultValue: 'X.X.X-SNAPSHOT', description: 'Next version to use for development.')
        string(name: 'scmTag', defaultValue: 'bubble-penetration-X.X.X', description: 'Name to use for the tag created.')
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
                sh "./gradlew assembleRelease"
                sh "mv ./app/build/outputs/apk/release/app-release.apk /var/www/spicysources.de/downloads/games/bubble-penetration/bubble-penetration-latest.apk"
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
    }
}