def call(body) {
    // Define default parameters or configurations
    def config = [:]
    
    // Parse the body closure to override defaults or add custom logic
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()
  
pipeline {
  agent any
    environment {
    GAR_REGISTRY = 'us-central1-docker.pkg.dev'
    PROJECT_ID = 'final-project-453412'
    FOLDER_NAME = 'docker-images'
    IMAGE_NAME = 'frontend-webapp'
    IMAGE_TAG = "${GAR_REGISTRY}/${PROJECT_ID}/${FOLDER_NAME}/${IMAGE_NAME}:${BUILD_NUMBER}"
    //Image tag aloowes you to define the tag of the image and to push the image in the GAR regestry
    // :${BUILD_NUMBER}" alows you to specify the tag for your image 
  }
    stages {
// Cleans the workspace every time you hit a build 
          stage('Clean Workspace') {
              steps {
                  cleanWs() 
              }
            }
//clone your repo from git to your jenkins workspace you need to create private token for git and use that token inside jenkins crediantial        
          stage("git-chekckout") {
              steps {
                git branch: 'main', credentialsId: 'Jenkins-token', url: 'https://github.com/Abhishekpawar1086/Frontend-webapp.git'
              }  
            }
//install all the dependencys using pakage.json file to run this webapplication
          stage('Install Dependencies') {
               steps {
                 echo 'Installing dependencies...'
                 sh 'npm install'
              }
            }
//perform all the test case that are defined in package.json
          stage("npm test") {
               steps {
                 sh"""
                   npm ci
                   """
             }  
           } 
//perform all the test case that are defined in package.json
          stage("lint test") {
              steps {
                sh"""
                  npm run lint
                  """
              }  
            }
//perform all the test case that are defined in package.json
          stage("npm unit test") {
            steps {
              sh"""
                npm run test:unit
                """
             }  
           }
//this stage will build the docker image from you source code using docker file inside jenkinsworkspace
//building image with image tag will let jenkins know where to transfer this image (GAR) in building stage only. For building and pushing only we add ENV variables
//image tag will create a new tag for us using build no of jenkins  
          stage('Build Docker Image') {
              steps {
                  script {
                    dockerImage=docker.build("${IMAGE_TAG}")         
              }
            }
          }
//this stage aloows to to authenticate your genkins with GCP in this you need to download the json file of service account which is having all required access. you need to add that file in jenkins crediantial 
//kind will be secret file and provide Any id (ex service-key-file) and add that json file over here
//you can use pipeline syntax to generate synatacs for with crediantial 
//in this stage you need to provide information related to your keyfile through which you can access your gcp variable can be any thing (ex GCP_KEY)
          stage('Authenticate with GCP') {
              steps {
                  script {
                    withCredentials([file(credentialsId: 'gcp-artifact-registry-key', variable: 'GCP_KEY_FILE')]) {          
                        sh '''
                          gcloud auth activate-service-account --key-file=${GCP_KEY_FILE}                             
                          gcloud auth configure-docker ${GAR_REGISTRY}
                           '''
                        sh '''
                          docker login -u _json_key -p "$(cat '''+GCP_KEY_FILE+''')" https://'''+GAR_REGISTRY+'''
                          docker push '''+IMAGE_TAG+'''
                           '''
              }  
            }
          }
        }
      }
    }
}
  
