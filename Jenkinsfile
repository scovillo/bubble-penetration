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
        PROD_HOST="router.spicysources.de"
        PROD_HOST_PORT="63787"
        PROD_HOST_USERNAME="jenkins"
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
                sshagent(credentials: ['ss']) {
                    sh """
                        ssh -p ${env.PROD_HOST_PORT} -o StrictHostKeyChecking=no -l ${env.PROD_HOST_USERNAME} ${env.PROD_HOST} uname -a
                        scp -P ${env.PROD_HOST_PORT} ./app/build/outputs/apk/debug/app-debug.apk ${env.PROD_HOST_USERNAME}@${env.PROD_HOST}:/var/www/www.lukas-scheerer.de/downloads/bubble-penetration.apk
                    """
                }
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
                    sh """
                        ssh -p ${env.PROD_HOST_PORT} -o StrictHostKeyChecking=no -l ${env.PROD_HOST_USERNAME} ${env.PROD_HOST} uname -a
                        scp -P ${env.PROD_HOST_PORT} ./app/build/outputs/apk/release/app-release.apk ${env.PROD_HOST_USERNAME}@${env.PROD_HOST}:/var/www/www.spicysources.de/downloads/games/bubble-penetration/bubble-penetration-latest.apk
                    """
                }
            }
        }
    }
    post {
        fixed {
            buildMail(['lukasscheerer@spicysources.de, davidlink@spicysources.de'], true)
        }
        failure {
            buildMail(['lukasscheerer@spicysources.de, davidlink@spicysources.de'], false)
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
