#!/bin/bash

# uninstall PCSecrets on Ubuntu

while true; do
    read -p "Do you wish to uninstall PCSecrets? (this will not remove your secrets)" yn
    case $yn in
        [Yy]* ) break;;
        [Nn]* ) exit;;
        * ) echo "Please answer yes or no.";;
    esac
done

INSTALLDIR=/usr/share/pcsecrets
DESKTOPAPPDIR=/usr/share/applications

rm ${DESKTOPAPPDIR}/pcsecrets.desktop
rm ${INSTALLDIR}/*.*
rmdir ${INSTALLDIR}

echo "If you want to remove your secrets completely, delete the .pcsecrets directory"

echo "Uninstall complete."
