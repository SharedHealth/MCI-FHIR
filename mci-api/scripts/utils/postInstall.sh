#!/bin/sh

ln -s /opt/mci-fhir/bin/mci-fhir /etc/init.d/mci-fhir
ln -s /opt/mci-fhir/etc/mci-fhir /etc/default/mci-fhir
ln -s /opt/mci-fhir/var /var/run/mci-fhir

if [ ! -e /var/log/mci-fhir ]; then
    mkdir /var/log/mci-fhir
fi

# Add mci-fhir service to chkconfig
chkconfig --add mci-fhir