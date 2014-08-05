
FROM ubuntu:12.04

# make sure the package repository is up to date
#RUN echo "deb http://archive.ubuntu.com/ubuntu precise main universe" > /etc/apt/sources.list
RUN apt-get update

# Avoid disturbing warnings in apt-get install
RUN apt-get install -y dialog

# We need to unzip later
RUN apt-get install -y unzip

# Fake a fuse install
RUN apt-get install -y --force-yes libfuse2
RUN cd /tmp ; apt-get download fuse
RUN cd /tmp ; dpkg-deb -x fuse_* .
RUN cd /tmp ; dpkg-deb -e fuse_*
RUN cd /tmp ; rm fuse_*.deb
RUN cd /tmp ; echo -en '#!/bin/bash\nexit 0\n' > DEBIAN/postinst
RUN cd /tmp ; dpkg-deb -b . /fuse.deb
RUN cd /tmp ; dpkg -i /fuse.deb

# Actually install java	
RUN apt-get install -y openjdk-7-jre

ADD ./target/universal/mte-1.0-SNAPSHOT.zip /opt/
RUN unzip /opt/mte-1.0-SNAPSHOT.zip -d /opt/mte

ADD ./resources /opt/mte/mte-1.0-SNAPSHOT/resources/

VOLUME ["/opt/mte/mte-1.0-SNAPSHOT/data"]

EXPOSE 9000

CMD cd /opt/mte/mte-1.0-SNAPSHOT && chmod +x ./bin/mte && ./bin/mte
#ENTRYPOINT /opt/mte/mte-1.0-SNAPSHOT/bin/mte

