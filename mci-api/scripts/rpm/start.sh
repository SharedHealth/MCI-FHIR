#!/bin/sh
nohup java -jar /opt/mci-registry/lib/mci-registry.war > /dev/null 2>&1  &
echo $! > /var/run/mci-registry/mci-registry.pid
