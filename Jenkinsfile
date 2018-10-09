#! groovy

properties(parameters([string(defaultValue: '0.0.0.0', description: '54.173.221.212', name: 'node1'),
                       string(defaultValue: 'origin/master', description: '', name: 'git_path'),
                       string(defaultValue: 'test_2018', description: 'Name of eco system (test_2017 or test_2018)', name: 'ecosystem')
                       ]))


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
      sh "'git checkout ${env.git_path} -b current_src' "
      sh "'git rev-parse --short HEAD > .git/commit-id' "
      commit_id = readFile('.git/commit-id').trim()
      cd ./backend;go build -o api
    }
  }
} 
