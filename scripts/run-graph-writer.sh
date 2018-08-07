#!/bin/sh
# ****************************************************************************
# * run graph writing server
# ****************************************************************************

echo starting GraphWriter server
pwd

LIB=lib
CLASSPATH=$LIB/GraphWriter-1.0.jar
CLASSPATH=$CLASSPATH:$LIB/disruptor-3.3.6.jar
CLASSPATH=$CLASSPATH:$LIB/junit-4.7.jar
CLASSPATH=$CLASSPATH:$LIB/log4j-1.2.17.jar

java -version
java -ea  -Djava.net.preferIPv6Addresses=false -Djava.net.preferIPv4Stack=true -classpath $CLASSPATH org.texai.graphwriter.GraphWriter