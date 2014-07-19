#!/bin/bash

#
# Usage: build version_type
#
# version__type is version_build or temp_build (default)
#

# clean and build the project first !!!

while true; do
    read -p "Did you clean and build first?" yn
    case $yn in
        [Yy]* ) break;;
        [Nn]* ) exit;;
        * ) echo "Please answer yes or no.";;
    esac
done

if [ $# -eq 0 ];   then
   btype="temp_build"
else
   btype=$1   
fi

cd ~/git/PCSecrets/PCSecrets/build

# The ant build create the zip for general use (and with a simple installer for
# Ubuntu)

ant $btype

# Windows installer is created here

cd ~/launch4j
export PATH=~/mingw32/bin:${PATH}
./launch4j ~/git/PCSecrets/PCSecrets/build/windows/pcsecrets-launch4j.xml
cd ~/git/PCSecrets/PCSecrets/build/windows
makensis pcsecrets.nsi

