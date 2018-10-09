FROM centos:latest 
RUN yum -y update && yum install -y epel-release openssl python python-devel gcc openssl-devel openssl-libs libffi-devel git && yum install -y python2-pip wget
RUN pip install --upgrade awscli boto boto3 s3cmd python-magic ansible requests

RUN wget https://storage.googleapis.com/golang/go1.9.1.linux-amd64.tar.gz
RUN tar -xvf go1.9.1.linux-amd64.tar.gz
RUN mv go /usr/local
RUN export GOROOT=/usr/local/go
RUN export GOPATH=$HOME/Apps/app1
RUN export PATH=$GOPATH/bin:$GOROOT/bin:$PATH

ENTRYPOINT ['aws']
