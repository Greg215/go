FROM centos:latest
RUN yum -y update && yum install -y epel-release openssl python python-devel gcc openssl-devel openssl-libs libffi-devel git && yum install -y python2-pip
RUN pip install --upgrade awscli boto boto3 s3cmd python-magic ansible requests
ENTRYPOINT ['aws']
