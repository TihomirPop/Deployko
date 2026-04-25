def appName = "deployko"
def imageTag = ""

pipeline {
    agent any

    stages {
        stage('Prepare') {
            steps {
                script {
                    imageTag = "${env.REGISTRY_URL}/${appName}:${env.BUILD_NUMBER}-${env.GIT_COMMIT?.take(7) ?: 'latest'}"
                }
            }
        }
        stage('Test') {
            steps {
                sh 'mvn test --no-transfer-progress'
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }
        stage('Dockerize') {
            steps {
                sh """
                    mvn spring-boot:build-image \
                        -DskipTests \
                        -Dspring-boot.build-image.imageName=${imageTag} \
                        --no-transfer-progress
                """
            }
        }
        stage('Push') {
            steps {
                sh "docker push ${imageTag}"
            }
            post {
                always {
                    sh "docker rmi ${imageTag} || true"
                }
            }
        }
    }
    post {
        always {
            cleanWs()
        }
        success {
            echo "Build ${env.BUILD_NUMBER} of ${env.JOB_NAME} succeeded."
        }
        failure {
            echo "Build ${env.BUILD_NUMBER} of ${env.JOB_NAME} failed."
        }
        unstable {
            echo "Build ${env.BUILD_NUMBER} is unstable — some tests failed."
        }
    }
}
