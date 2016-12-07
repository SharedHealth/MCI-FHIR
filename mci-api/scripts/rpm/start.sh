#!/bin/sh
nohup java -jar /opt/mci-fhir/lib/mci-fhir.war > /dev/null 2>&1  &
echo $! > /var/run/mci-fhir/mci-fhir.pid
