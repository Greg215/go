FROM buildpack-deps:stretch-scm
RUN apt -y update && apt install -y epel-release openssl python python-devel gcc openssl-devel openssl-libs libffi-devel git && apt install -y python2-pip
RUN pip install --upgrade awscli boto boto3 s3cmd python-magic ansible requests
ENTRYPOINT ['aws']
