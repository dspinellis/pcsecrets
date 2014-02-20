Installation

PCSecrets can be installed and run on any system that supports Java.

At minimum, the jar file should be extracted from the zip file and placed in a
suitable directory and executed using the java command. On *nix systems it will
first need to be marked executable:

  chmod +x pcsecrets.jar
  java -jar pcsecrets.jar
  
Java 1.6+ is required, 1.7 is recommended.

For a few systems, some installation support is provided:

For Windows, an installer is available. Download and execute
  pcsecrets-installer.exe instead of using this zip file.
  
For Ubuntu, rudimentary install and uninstall scripts are provided here, and
  described below.
  
You can install over an existing installation without affecting existing secrets.


Ubuntu (and derivatives)

Install

In the directory containing the unzipped files:

  chmod +x install.sh
  sudo ./install.sh

(Root privileges are needed to copy the files to /usr/share.)
A desktop entry is also created for the applications menu.

Uninstall

  sudo /usr/share/pcsecrets/uninstall.sh
  
Uninstall does not remove the pcsecrets data directory.