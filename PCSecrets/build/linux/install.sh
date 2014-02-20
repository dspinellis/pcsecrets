#!/bin/bash

# install PCSecrets on Ubuntu

# to be executed in the unzipped directory

while true; do
    read -p "Do you wish to install PCSecrets?" yn
    case $yn in
        [Yy]* ) break;;
        [Nn]* ) exit;;
        * ) echo "Please answer yes or no.";;
    esac
done

INSTALLDIR=/usr/share/pcsecrets
DESKTOPAPPDIR=/usr/share/applications

chmod +x pcsecrets.sh
chmod +x uninstall.sh
mkdir ${INSTALLDIR} 2>/dev/nul
cp pcsecrets.jar ${INSTALLDIR}
cp pcsecrets.sh ${INSTALLDIR}
cp pcsecrets.desktop ${DESKTOPAPPDIR}
cp pcsecrets96x96.png ${INSTALLDIR}
cp doc.html ${INSTALLDIR}
cp legal.txt ${INSTALLDIR}
cp history.txt ${INSTALLDIR}
cp notes.txt ${INSTALLDIR}
cp build.properties ${INSTALLDIR}
cp uninstall.sh ${INSTALLDIR}

echo "Install complete."
