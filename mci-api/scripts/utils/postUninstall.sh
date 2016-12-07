#!/bin/sh

rm -f /etc/init.d/mci-fhir
rm -f /etc/default/mci-fhir
rm -f /var/run/mci-fhir

#Remove mci-fhir from chkconfig
chkconfig --del mci-fhir || true