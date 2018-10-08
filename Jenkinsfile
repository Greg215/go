#!groovy

deployment_user = "ubuntu"
node_ip = "54.173.221.212"

ui_repo_url = "ssh://git@github.com:Greg215/go-angular.git"
ui_working_dir = "/home/ubuntu/empa"

workspace_path = "${env.JENKINS_HOME}/workspace/${env.JOB_BASE_NAME}/"
automatic = false

awscli = docker.build('awscli', '-f Dockerfile .')

def ssh_exec(cmd){
  return sh (returnStdout: true,
                       script: """ssh ${deployment_user}@${node_ip} ${cmd}"""
                      ).trim()
}

def green_or_blue(app_name){
  def current = sh(returnStdout: true,
            script: "ssh ${deployment_user}@${node_ip} 'readlink /home/ubuntu/empa/${app_name} | xargs basename 2> /dev/null || echo 'green''"
           ).trim()
  return current.endsWith("green") == true ? "blue" : "green"
}

node {
    checkout scm
    ws("${WORKSPACE}/backend/"){
    cleanWs deleteDirs: true
    stage('Build Backend') {
      git changelog: false, credentialsId: key_guthub, branch: 'master',
      poll: false, url: ui_repo_url
      sh "git checkout ${env.git_path} -b current_src"
      sh "git rev-parse --short HEAD > .git/commit-id"
      commit_id = readFile('.git/commit-id').trim()
      cd ./backend;go build -o api
    }
    
    stage('Deploy Backend') {
      tmp_dir = "/tmp/empa/${env.BUILD_NUMBER}"
      sshagent(ssh_crendentials) {
        ssh_exec "'mkdir -p ${tmp_dir}'"
        sh "scp api ${deployment_user}@${node_ip}:${tmp_dir}/"
        def new_deployment = sh (returnStdout: true,
               script: """ssh ${deployment_user}@${node_ip} bash <<EOF
               if [[ \\\$(basename \\\$(readlink ${working_dir})) = 'empa_backend_green' ]];
               then
                  echo 'empa_backend_blue';
               else
                  echo 'empa_backend_green';
               fi""").trim()
        echo "${new_deployment}"
        deployment_dir = "/home/ubuntu/${new_deployment}"
        ssh_exec "'rm -rf ${deployment_dir}'"
        ssh_exec "'mkdir ${deployment_dir}'"
        ssh_exec "'cp ${tmp_dir}/api ${deployment_dir}'"
        ssh_exec "'rm -rf ${tmp_dir}'"
        ssh_exec "'rm -f ${working_dir}'"
        ssh_exec "'ln -sf ${deployment_dir} ${working_dir}'"
      }
    }
  }
}
