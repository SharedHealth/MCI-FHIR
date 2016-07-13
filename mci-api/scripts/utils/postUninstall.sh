#!/bin/sh

rm -f /etc/init.d/mci-registry
rm -f /etc/default/mci-registry
rm -f /var/run/mci-registry

#Remove mci-registry from chkconfig
chkconfig --del mci-registry || true