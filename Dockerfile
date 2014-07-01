FROM zeeburg.informatik.uni-mannheim.de:5000/ubuntu_java:latest

ENV JAVA_OPTS -Xms3096M -Xmx6192M
VOLUME ["/opt/mte/data"]

# APP port
EXPOSE 9000
EXPOSE 5678

# Install APP
ADD ./resources /opt/mte/resources/
ADD ./target/universal/stage /opt/mte

CMD cd /opt/mte && rm -f RUNNING_PID && chmod +x ./bin/mte && ./bin/mte -J-server -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=5678 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.local.only=false -Djava.rmi.server.hostname=zeeburg.informatik.uni-mannheim.de -J-javaagent:/opt/mte/lib/jamm-0.2.6.jar


