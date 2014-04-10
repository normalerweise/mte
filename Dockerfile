
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


# Install APP
ADD ./target/universal/stage /opt/mte
ADD ./resources /opt/mte/resources/

ENV JAVA_OPTS -Xms3096M -Xmx6192M -XX:MaxPermSize=256M -XX:ReservedCodeCacheSize=128M
VOLUME ["/opt/mte/data"]

# APP port
EXPOSE 9000

# Debugging port
EXPOSE 5111

CMD cd /opt/mte && chmod +x ./bin/mte && ./bin/mte -J-server -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=5111 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Djava.rmi.server.hostname=134.155.85.156 -Dcom.sun.management.jmxremote.local.only=false


