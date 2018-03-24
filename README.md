# PCSecrets-GitHub

Desktop client for the
[Secrets for Android](https://github.com/rogerta/secrets-for-android/) 
password manager.

This is a fork of the [PCSecrets](http://pcsecrets.sourceforge.net),
written by Chris Wood, that introduces the following enhancements.

* An Ant build file that compiles the project from source
* The ability to run under Java 9
* An updated version of the BouncyCastle cryptography library

## Building
Run `ant` in the `build` directory.

## Running
Run the `build/pcsecrets/pcsecrets.jar` file. Depending on your
environment, you can do this by double-clicking on the file,
or through a command such as `java -jar pcsecrets.jar`.
