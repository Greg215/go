FROM centos:latest 
RUN yum -y update && yum install -y epel-release openssl python python-devel gcc openssl-devel openssl-libs libffi-devel git && yum install -y python2-pip wget
RUN pip install --upgrade awscli boto boto3 s3cmd python-magic requests

ENV GOLANG_VERSION 1.7
ENV GOLANG_DOWNLOAD_URL https://golang.org/dl/go$GOLANG_VERSION.linux-amd64.tar.gz
ENV GOLANG_DOWNLOAD_SHA256 702ad90f705365227e902b42d91dd1a40e48ca7f67a2f4b2fd052aaa4295cd95
ENV GOPATH /go
ENV PATH $GOPATH/bin:/usr/local/go/bin:$PATH

RUN yum install -y g++                                              \
                  libc6-dev                                        \
                  make                                             \
                  python-setuptools                                \
 && easy_install supervisor                                        \
 && mkdir -p /var/log/supervisor                                   \
 && echo_supervisord_conf > /etc/supervisord.conf                  \
 && curl -fsSL "$GOLANG_DOWNLOAD_URL" -o golang.tar.gz             \
 && echo "$GOLANG_DOWNLOAD_SHA256  golang.tar.gz" | sha256sum -c - \
 && tar -C /usr/local -xzf golang.tar.gz                           \
 && rm golang.tar.gz                                               \
 && mkdir -p "$GOPATH/src" "$GOPATH/bin" && chmod -R 777 "$GOPATH" \
 && git clone https://github.com/docker-library/golang.git         \
 && cd golang                                                      \
 && sh update.sh                                                   \
 && cp go-wrapper /usr/local/bin/

ENTRYPOINT ['aws']
