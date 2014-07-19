PCSecrets 1.1.3 2014-07-18

New in this version:
  - changed the way the PCSecrets directory path is determined on Windows.
    Previously the special "Application Data" construct was used but this
    appeared not to work on some Win7 systems. Now uses the environment
    variable APPDATA value.
  - add /l [level] cmd line option for logging
  - don't interpret clipboard copy as a change to secret data - was triggering
    "entry has been updated" message

IMPORTANT: if you are updating from version 1.1.1, read below!

PCSecrets 1.1.2 2014-04-18

New in this version:
  - fixed bug001 problem using passwords longer than 9 characters
  - fixed bug002 loop if file permissions prevent deletion of old backup file
  
Note for existing users: if you currently have a password longer than 9 characters
you MUST CHANGE IT TO ONE OF 9 CHARACTERS OR LESS BEFORE updating to this version.
If you don't do this you won't be able to decrypt your secrets any more!

After installing you can change the password back to the original.