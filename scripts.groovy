awscli = docker.build('awscli', '-f Dockerfile .')

@NonCPS
def finder(text, pattern) {
  return text.findAll(pattern)
}

@NonCPS
def jsonFilter(def json, def key, def value) {
  return json.find { it -> it[key] == value }
}

def ssh_exec(cmd){
  return sh (returnStdout: true,
                       script: """ssh ${deployment_user}@${edge_node} ${cmd}"""
                      ).trim()
}

def get_instance_data(manager_id){
  return sh (returnStdout: true,
                       script: "aws ec2 describe-instances --filters Name=instance-id,Values=${manager_id}"
                      ).trim()
}

def find_running_environments(tags){
  purposes = []
  for (i = 0; i < tags.size(); i++) {
    manager_data = readJSON text: get_instance_data(tags[i][0][0])
    instance = manager_data.Reservations[0].Instances[0]
    if (instance.State.Code == 16){
      purposes.push(jsonFilter(instance.Tags, 'Key', 'purpose').Value)
    }
  }
  return purposes
}

def find_edge_node_ip(machines){
  for (i = 0; i < machines.size(); i++) {
    instance = machines[i].Instances[0]
    role = jsonFilter(instance.Tags, 'Key', 'CDHRole').Value
    if (instance.State.Code == 16 && role == 'edge'){
      return instance.PublicIpAddress
    }
  }
  return null
}

def run_on_awscli(def commands, def envs = []){
  awscli.inside('-u root') {
          checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: bigindex_infra_branch]],
          doGenerateSubmoduleConfigurations: false,
          extensions: [[$class: 'CleanBeforeCheckout']], submoduleCfg: [],
          userRemoteConfigs: [[credentialsId: repo_credentials,
          url: 'ssh://git@bitbucket.org/datatenor420/dt-infra.git']]]
          withCredentials([string(credentialsId: ansible_creds_id, variable: 'ansible_vault_pass')]) {
            sh "echo '${ansible_vault_pass}' >> ${env.WORKSPACE}/.vault-pass.txt"
		      }
          withCredentials([[$class: 'AmazonWebServicesCredentialsBinding',
            accessKeyVariable: 'AWS_ACCESS_KEY_ID',
            credentialsId: jenkins_creds_id,
            secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
              withEnv(["AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID}",
              		   "AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY}",
                       "AWS_DEFAULT_REGION=${aws_region}",
                       "ANSIBLE_HOST_KEY_CHECKING=False"] + envs){
                sshagent(stack_crenetials) {
    			          commands()
			          }
           }
          }
        }
}

def get_deployment_options(app, repo, message=''){
  def app_deployment = input message: "Do you want to deploy ${app}?",
    parameters: [booleanParam(defaultValue: true, description: "I want to deploy ${app} ${message}", name: 'Yes')]
  if (app_deployment == true){
    ws("${workspace_path}/${app}/"){
      cleanWs deleteDirs: true
      stage("Get available ${app} builds"){
        def deploy_option = input message: "Which type of deployment you want?",
          parameters:  [choice(name: 'Type of deployment: ', choices: 'tags\nbranches', description: '')]
        git changelog: false, credentialsId: repo_credentials, poll: false, url: repo
        if (deploy_option == 'tags'){
          def last_tags = sh(returnStdout: true, script: "git for-each-ref --sort=-taggerdate --format '%(refname:short)' refs/tags --count=5").trim()
          last_tags = last_tags.split("\n").collect { return it.toString().trim() }
          source_name = input message: "Which version of ${app} did you want to deploy?",
            parameters: [choice(name: 'Tag name', choices: last_tags.join("\n"), description: 'Choose from available tags')]
          source = "tags"
        }else{
          def branches = sh(returnStdout: true, script: "git branch -r").trim()
          branches = branches.split("\n").collect { return it.trim().replace('origin/', '') }
          source_name = input message: "Which branch of ${app} did you want to deploy?",
            parameters: [choice(name: 'Branch name', choices: branches.join("\n"), description: 'Choose from available branches', defaultValue: 'master')]
          source = "origin"
        }
        return [type: source, name: source_name, path: "${source}/${source_name}", command: "git checkout ${source}/${source_name} -b src_${source_name}"]
      }
    }
  }
}

def manage_service(def app_path, def working_dir, def script, def returnStatus=false, def params=""){
  def script_path = "${working_dir}/${app_path}/${script}"
  def command = """ssh nbi@${edge_node} bash -c "'${enable_bash_profile} && [ -f ${script_path} ] && ${script_path} ${params} || echo \\\$?'";"""
  if (returnStatus){
    return sh(returnStatus: true, script: command)
  }else{
  	return sh(returnStdout: true, script: command)
  }
}

def green_or_blue(app_name){
  def current = sh(returnStdout: true,
            script: "ssh ${deployment_user}@${edge_node} 'readlink ${working_dir}/${app_name} | xargs basename 2> /dev/null || echo 'green''"
           ).trim()
  return current.endsWith("green") == true ? "blue" : "green"
}

def start_batch(def batch_name, def parameters, def command = null){
  def params = parameters.findAll { k,v -> v != null && v != '' }.collect {k,v -> "--${k} ${v}"}.join(' ')
  if(command != null){
    params = "${command} ${params}"
  }
  def app_path = sh(returnStdout: true,
    script: "ssh nbi@${edge_node} find ${core_working_dir} -type l -name \'${batch_name}*\'").trim()
  manage_service(app_path, "", "start-yarn.sh", false, params)
}

def deploy_package(app, tmp_dir, working_dir){
  def new_or_old = sh(returnStdout: true, script: "ssh ${deployment_user}@${edge_node} 'test -L ${working_dir}/${app.short_name} && echo 1 || echo 0'").trim()
  def is_new = new_or_old == "1" ? true : false
  ssh_exec("'mkdir -p ${tmp_dir}'")
  sh "scp ${app.path} ${deployment_user}@${edge_node}:${tmp_dir}/"
  sh """ssh ${deployment_user}@${edge_node} bash <<EOF
            cd ${tmp_dir};
            find -name '*.zip' -exec sh -c 'unzip -d "\\\${1%.*}" "\\\$1"' _ {} \\\\;
            rm -f *.zip; """

  def app_name = is_new == true ? app.short_name : app.name
  def new_color = green_or_blue(app_name)
  def new_deployment = new_color == "green" ? "${app.name}_green" : "${app.name}_blue"
  def deployment_dir = "${working_dir}/${new_deployment}"
  sh """ssh ${deployment_user}@${edge_node} "rm -rf ${working_dir}/${app.short_name}*_${new_color}; mkdir -p ${deployment_dir}";"""
  sh """ssh ${deployment_user}@${edge_node} "cp -R ${tmp_dir}/${app.name}/* ${deployment_dir}";"""
  sh """ssh ${deployment_user}@${edge_node} "rm -rf ${tmp_dir}";"""
  def script_suffix = app.name ==~ /^listener(.*)$/ ? '-yarn' : ''
  def is_managable = app.name ==~ /^(.*)(listener|service)(.*)$/ ? true : false
  if (is_managable) manage_service(app_name, working_dir, "stop${script_suffix}.sh")
  ssh_exec("'rm -f ${working_dir}/${app_name}'")
  ssh_exec("'ln -sf ${deployment_dir} ${working_dir}/${app.short_name}'")
  if (is_managable){
    def state = manage_service(app.short_name, working_dir, "start${script_suffix}.sh")
    state = state[-1]
    try{
      state = state.toInteger()
      echo "Service ${app.name} don't start correctly"
    }catch(err){
        echo "App started succesfully"
    }
  }
  if (app.name ==~ /^(.*)-service-(.*)$/){
    def status = manage_service(app.short_name, working_dir, "status.sh", true)
    if (status.toInteger() > 0) error "script failed"
  }
}

return this
