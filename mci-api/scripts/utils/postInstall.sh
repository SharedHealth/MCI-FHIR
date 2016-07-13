#!/bin/sh

ln -s /opt/mci-registry/bin/mci-registry /etc/init.d/mci-registry
ln -s /opt/mci-registry/etc/mci-registry /etc/default/mci-registry
ln -s /opt/mci-registry/var /var/run/mci-registry

if [ ! -e /var/log/mci-registry ]; then
    mkdir /var/log/mci-registry
fi

# Add mci-registry service to chkconfig
chkconfig --add mci-registry