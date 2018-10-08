#!groovy
properties([buildDiscarder(logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '10')),
            parameters([string(defaultValue: '0.0.0.0', description: '54.173.221.212', name: 'node1'),
                        string(defaultValue: 'origin/master', description: '', name: 'git_path'),
                        string(defaultValue: 'test_2018', description: 'Name of eco system (test_2017 or test_2018)', name: 'ecosystem')
                       ])
           ])


node {
  cleanWs deleteDirs: true
  checkout scm
  load 'settings.groovy'
  if (env.ecosystem == 'test_2017'){
      load 'test_2017.groovy'
  }else{
      if (env.ecosystem == 'test_2018'){
          load 'test_2018.groovy'
      }else{
        error 'Undefined ecosystem!'
      }
  }
  Utils = load 'scripts.groovy'
  ws("${WORKSPACE}/empa/"){
    stage('Build Backend') {
      git changelog: false, credentialsId: key_guthub, branch: 'master',
      poll: false, url: repo_url
      sh "git checkout ${env.git_path} -b current_src"
      sh "git rev-parse --short HEAD > .git/commit-id"
      commit_id = readFile('.git/commit-id').trim()
      cd ./backend;go build -o api
    }

    stage('Deploy Api'){
      tmp_dir = '/tmp/empa/${env.BUILD_NUMBER}'
      working_dir = empa_working_dir
      sshagent(ssh_crendentials) {
            Utils.ssh_exec "'mkdir -p ${tmp_dir}'"
            sh "scp api ${deployment_user}@${node1}:${tmp_dir}/"
            def new_deployment = sh (returnStdout: true,
                                     script: """ssh ${deployment_user}@${node1} bash <<EOF
                                        if [[ \\\$(basename \\\$(readlink ${working_dir})) = 'empa_backend_green' ]];
                                        then
                                          echo 'empa_backend_blue';
                                        else
                                          echo 'empa_backend_green';
                                        fi""").trim()
            echo "${new_deployment}"
            deployment_dir = "/home/${deployment_user}/${new_deployment}"
            Utils.ssh_exec "'rm -rf ${deployment_dir}'"
            Utils.ssh_exec "'mkdir ${deployment_dir}'"
            Utils.ssh_exec "'cp -R ${tmp_dir}/api ${deployment_dir}'"
            Utils.ssh_exec "'./${deployment_dir}/api &'"
            Utils.ssh_exec "'rm -rf ${tmp_dir}'"
            Utils.ssh_exec "'rm -f ${working_dir}'"
            Utils.ssh_exec "'ln -sf ${deployment_dir} ${working_dir}'"
      }
    }
  }
}

    
