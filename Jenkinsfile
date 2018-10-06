#!groovy

awscli = docker.build('awscli', '-f Dockerfile .')

def ssh_exec(cmd){
  return sh (returnStdout: true,
                       script: """ssh ubuntu@54.173.221.212 ${cmd}"""
                      ).trim()
}

def green_or_blue(app_name){
  def current = sh(returnStdout: true,
            script: "ssh ubuntu@54.173.221.212 'readlink /home/ubuntu/empa/${app_name} | xargs basename 2> /dev/null || echo 'green''"
           ).trim()
  return current.endsWith("green") == true ? "blue" : "green"
}

deployment_user = "ubuntu"

ui_repo_url = "ssh://git@github.com:Greg215/go-angular.git"
ui_working_dir = "/home/ubuntu/empa"

workspace_path = "${env.JENKINS_HOME}/workspace/${env.JOB_BASE_NAME}/"
automatic = false

node {
  checkout scm
  ws("${WORKSPACE}/backend/"){
  cleanWs deleteDirs: true
  stage('Build backend') {
    git changelog: false, credentialsId: key_ubuntu, branch: 'master',
    poll: false, url: ui_repo_url
    sh "git checkout ${env.git_path} -b current_src"
    sh "git rev-parse --short HEAD > .git/commit-id"
    commit_id = readFile('.git/commit-id').trim()
    cd ./backend;go build -o api
    
  stage('Remove old api') {
    sshagent(ssh_crendentials) {
    
