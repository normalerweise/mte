FROM zeeburg.informatik.uni-mannheim.de:5000/ubuntu_java:latest


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


