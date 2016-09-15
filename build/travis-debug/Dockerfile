# Simulate our Maven build on Travis-CI
# WORKDIR ends up as /home/travis/build
# Source directory should be mounted here

FROM quay.io/travisci/travis-jvm

# Install any additional packages
#RUN apt-get autoclean && apt-get update && DEBIAN_FRONTEND=noninteractive apt-get install -y \
#RUN apt-get autoclean && DEBIAN_FRONTEND=noninteractive apt-get install -y \
RUN apt-get -yq update && DEBIAN_FRONTEND=noninteractive apt-get -yq --no-install-suggests --no-install-recommends --force-yes install \
  metacity \
  tightvncserver

# download and update Cloud SDK; update must be performed from outside
RUN mkdir -p /opt
WORKDIR /opt
ENV GOOGLE_CLOUD_SDK_HOME=/opt/google-cloud-sdk
RUN wget https://dl.google.com/dl/cloudsdk/channels/rapid/downloads/google-cloud-sdk-118.0.0-linux-x86_64.tar.gz \
  && tar -xzf google-cloud-sdk-118.0.0-linux-x86_64.tar.gz \
  && $GOOGLE_CLOUD_SDK_HOME/bin/gcloud components update --quiet \
  && $GOOGLE_CLOUD_SDK_HOME/bin/gcloud components install app-engine-java --quiet
ENV PATH $GOOGLE_CLOUD_SDK_HOME/bin:$PATH

# COPY always done as root, so files owned by root?
ENV HOME=/home/travis
COPY setup.sh $HOME/setup.sh
RUN chmod +r+x $HOME/setup.sh
COPY password.txt $HOME/password.txt
RUN chmod +r $HOME/setup.sh

# Builds are done using the `travis` user
USER travis
ENV USER=travis
ENV PATH /usr/local/maven/bin:$PATH
WORKDIR $HOME

# Set default password for VNC
RUN cat password.txt password.txt | vncpasswd

# Expose VNC port
EXPOSE 5901
ENV DISPLAY :1

# Create empty .m2 directory in case the user wants to
# mount their own repository there
RUN mkdir -p $HOME/.m2

# Presumed to be a volume mounted at /home/travis/build
WORKDIR $HOME/build

# VNC needs to be restarted on each launch
ENTRYPOINT ["/home/travis/setup.sh"]
CMD ["mvn", "-Ptravis", "--fail-at-end", "verify"]
