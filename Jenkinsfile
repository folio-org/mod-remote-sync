

pipeline {

  environment {
    ORG_GRADLE_PROJECT_appName = 'mod-remote-sync'
    GRADLEW_OPTS = '--console plain --no-daemon'
    BUILD_DIR = "${env.WORKSPACE}/service"
    MD = "${env.WORKSPACE}/service/build/resources/main/okapi/ModuleDescriptor.json"
    doKubeDeploy = true

  }

  options {
    timeout(30)
    buildDiscarder(logRotator(numToKeepStr: '30'))
  }

  agent {
    node {
      label 'jenkins-agent-java17'
    }
  }

  stages {
    stage ('Setup') {
      steps {
        dir(env.BUILD_DIR) {
          script {
            def foliociLib = new org.folio.foliociCommands()
            def gradleVersion = foliociLib.gradleProperty('appVersion')

            env.name = env.ORG_GRADLE_PROJECT_appName
        
            // if release 
            if ( foliociLib.isRelease() ) {
              // make sure git tag and version match
              if ( foliociLib.tagMatch(gradleVersion) ) {
                env.isRelease = true 
                env.dockerRepo = 'folioorg'
                env.version = gradleVersion
              }
              else { 
                error('Git release tag and Maven version mismatch')
              }
            }
            else {
              env.dockerRepo = 'folioci'
              env.version = "${gradleVersion}.${env.BUILD_NUMBER}"
            }
          }
        }
        sendNotifications 'STARTED'  
      }
    }

    stage('Gradle Build') { 
      steps {
        dir(env.BUILD_DIR) {
          sh "./gradlew $env.GRADLEW_OPTS assemble"
        }
      }
    }
   
    stage('Build Docker') {
      steps {
        dir(env.BUILD_DIR) {
          sh "./gradlew $env.GRADLEW_OPTS -PdockerRepo=${env.dockerRepo} buildImage"
        }
        // debug
        sh "cat $env.MD"
      } 
    }

    stage('Publish Docker Image') { 
      when { 
        anyOf {
          branch 'master'
          expression { return env.isRelease }
        }
      }
      steps {
        script {
          docker.withRegistry('https://index.docker.io/v1/', 'DockerHubIDJenkins') {
            sh "docker tag ${env.dockerRepo}/${env.name}:${env.version} ${env.dockerRepo}/${env.name}:latest"
            sh "docker push ${env.dockerRepo}/${env.name}:${env.version}"
            sh "docker push ${env.dockerRepo}/${env.name}:latest"
          }
        }
      }
    }

    stage('Publish Module Descriptor') {
      when {
        anyOf { 
          branch 'master'
          expression { return env.isRelease }
        }
      }
      steps {
        script {
          sh "mv $MD ${MD}.orig"
          sh """
          cat ${MD}.orig | jq '.launchDescriptor.dockerImage |= \"${env.dockerRepo}/${env.name}:${env.version}\" |
              .launchDescriptor.dockerPull |= \"true\"' > $MD
          """
        }
        postModuleDescriptor(env.MD)
      }
    }

  } // end stages

  post {
    always {
      dockerCleanup()
      sendNotifications currentBuild.result 
    }
  }
}
         

