FROM folioci/alpine-jre-openjdk11:latest
MAINTAINER Ian.Ibbotson@k-int.com
VOLUME /tmp
ENV VERTICLE_FILE mod-remote-sync.war
ENV VERTICLE_HOME /
COPY service/build/libs/mod-remote-sync-*.*.*.jar mod-remote-sync.war
EXPOSE 8080/tcp
