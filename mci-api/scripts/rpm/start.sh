#!/bin/sh
nohup java -Dserver.port=$MCI_PORT -DMCI_LOG_LEVEL=$MCI_LOG_LEVEL -jar /opt/mci-registry/lib/mci-registry.war > /dev/null 2>&1  &
echo $! > /var/run/mci-registry/mci-registry.pid
