PCSecrets 1.2.0 2015-03-19

New in this version:
  - inactivity timeout feature added
  - network access now delayed until sync function is requested, so if this
    feature isn't used, no network access is required.

IMPORTANT: if you are updating from version 1.1.1, read below!

PCSecrets 1.1.2 2014-04-18

New in this version:
  - fixed bug001 problem using passwords longer than 9 characters
  - fixed bug002 loop if file permissions prevent deletion of old backup file
  
Note for existing users: if you currently have a password longer than 9 characters
you MUST CHANGE IT TO ONE OF 9 CHARACTERS OR LESS BEFORE updating to this version.
If you don't do this you won't be able to decrypt your secrets any more!

After installing you can change the password back to the original.