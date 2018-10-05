#!groovy

awscli = docker.build('awscli', '-f Dockerfile .')

def ssh_exec(cmd){
  return sh (returnStdout: true,
                       script: """ssh ${deployment_user}@${edge_node} ${cmd}"""
                      ).trim()
}

def green_or_blue(app_name){
  def current = sh(returnStdout: true,
            script: "ssh ${deployment_user}@${edge_node} 'readlink ${working_dir}/${app_name} | xargs basename 2> /dev/null || echo 'green''"
           ).trim()
  return current.endsWith("green") == true ? "blue" : "green"
}

deployment_user = "ubuntu"

ui_repo_url = "ssh://git@github.com:Greg215/go-angular.git"
ui_working_dir = "/home/ubuntu/empa"

workspace_path = "${env.JENKINS_HOME}/workspace/${env.JOB_BASE_NAME}/"
automatic = false

node {
  cleanWs deleteDirs: true
  checkout scm


