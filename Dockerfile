FROM tomcat:9.0-jre8-temurin-focal

MAINTAINER normandoad@gmail.com

USER root

# Service name
ENV SERVICE_NAME proxy-ml

# WAR filename
ENV WAR_FILENAME ${SERVICE_NAME}.war

# Add WAR file
COPY "target/${WAR_FILENAME}" "webapps/${WAR_FILENAME}"

RUN apt-get update
RUN apt-get -y dist-upgrade
RUN apt-get -y autoremove
RUN apt-get install -y zip

# Extract war into deploy folder
RUN    unzip "webapps/${WAR_FILENAME}" -d "webapps/${SERVICE_NAME}"
RUN rm -f "webapps/${WAR_FILENAME}"

# Expose necessary port
EXPOSE 8080
