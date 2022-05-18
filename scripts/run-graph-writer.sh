#!/bin/bash
# ****************************************************************************
# * run graph writing server
# ****************************************************************************

echo starting GraphWriter server
pwd
CLASSPATH=lib/GraphWriter-1.0.jar:$(<classpathFile);
#echo CLASSPATH: ${CLASSPATH}

java -version
java -ea  -Djava.net.preferIPv6Addresses=false -Djava.net.preferIPv4Stack=true -classpath ${CLASSPATH}  org.texai.graphwriter.GraphWriter
