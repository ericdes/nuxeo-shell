#!/bin/sh

JAVA_OPTS="-Djava.rmi.server.RMIClassLoaderSpi=NuxeoRMIClassLoader -Dsun.lang.ClassLoader.allowArraySyntax=true"
#JAVA_OPTS="$JAVA_OPTS -Xdebug -Xrunjdwp:transport=dt_socket,address=127.0.0.1:8788,server=y,suspend=y"
#JAVA_OPTS="$JAVA_OPTS -Dorg.nuxeo.runtime.1.3.3.streaming.port=3233"

java -cp lib $JAVA_OPTS Launcher launcher.properties "$@"

