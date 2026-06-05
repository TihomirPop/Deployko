def appName = "deployko"
def imageTag = ""
def imageVersion = ""

pipeline {
    agent any

    stages {
        stage('Prepare') {
            steps {
                script {
                    imageVersion = "${env.BUILD_NUMBER}-${env.GIT_COMMIT?.take(7) ?: 'latest'}"
                    imageTag = "${env.REGISTRY_URL}/${appName}:${imageVersion}"
                    echo "Docker image tag: ${imageTag}"
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
        stage('Publish Event') {
            steps {
                script {
                    rabbitMQPublisher(
                        rabbitName: 'rabbitmq',
                        exchange: 'ci-events',
                        routingKey: 'pipeline.completed',
                        data: groovy.json.JsonOutput.toJson([
                            event: 'pipeline_completed',
                            status: 'success',
                            service: appName,
                            tag: imageVersion,
                            repo: env.GIT_URL,
                            buildNumber: env.BUILD_NUMBER as Integer
                        ]),
                        conversion: false,
                        toJson: false
                    )
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
