def get_ecosystem(env){
  if (env.ecosystem == 'test_2017'){
      return 'test_2017.groovy'
  }else{
      if (env.ecosystem == 'test_2018'){
          return 'test_2018.groovy'
      }else{
        error 'Undefined ecosystem!'
      }
  }
}
