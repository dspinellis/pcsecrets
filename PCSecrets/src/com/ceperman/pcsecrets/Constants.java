/**
 * Copyright 2013 Chris Wood
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ceperman.pcsecrets;

/**
 * Definition of project-wide constants
 * 
 * @author Chris Wood
 */
public class Constants {
  /* commands */
	final static String EXPORT = "export";
	final static String IMPORT = "import";
	final static String PREFERENCES = "preferences";
	final static String DEVICES = "devices";
	final static String EXIT = "exit";
	final static String HELP = "help";
	final static String ABOUT = "about";
	final static String SYNC = "sync";
	final static String SAVEALL = "saveall";
	final static String CREATE = "create";
	final static String UPDATE = "update";
	final static String DELETE = "delete";
	final static String CLEAR = "clear";
	final static String FINISH = "finish";
	final static String COMMIT = "commit";
	final static String MERGED = "merged";
	final static String COPYTOPC = "copytopc";
	final static String OK = "ok";
	final static String APPLY = "apply";
	final static String CANCEL = "cancel";
	final static String COMPLETE = "complete";
	final static String DEFAULTS = "defaults";
	final static String FAILED = "failed";
	final static String REFRESH = "refresh";
	final static String SHOW = "show";
	final static String SELECT = "select";
	final static String CHANGEPSWD = "changepswd";
	final static String REGENCIPHERS = "regenciphers";
	final static String SYSINFO = "sysinfo";
	
	/* property changes */
	final static String PHONE_RECEIVE = "phonereceive";
	final static String PHONE_SEND = "phonesend";
	final static String SYNC_COMPLETE = "synccomplete";
	
	/* Secrets CSV column names */
	final static String COL_DESCRIPTION = "Description";
	final static String COL_USERNAME = "Username";
	final static String COL_PASSWORD = "Password";
	final static String COL_EMAIL = "Email";
	final static String COL_NOTES= "Note";
	final static String COL_TIMESTAMP= "Timestamp";
	
	/* properties */
	final static String DEFAULT_DIR = "defaultDir";
	final static String SECRETS_DIR = "secretsDir";
	final static String SECRETS_FILENAME = "secretsFileName";
	final static String LOG_LEVEL = "logLevel";
	final static String SERVERPORT = "serverPort";
	final static String CLIENTPORT = "clientPort";
	final static String SYNCUSESAMEPSWD = "syncUseSamePswd";
	final static String SUPPRESS_SYNCDIALOG = "suppressSyncDialog";
	final static String KEYLENGTH = "keyLength";
	final static String MAXKEYLENGTH = "maxKeyLength";
	final static String BACKUP_ENABLED = "backupEnabled";
	final static String BACKUP_DIR = "backupDir";
	final static String MAX_BACKUP_COUNT = "maxBackups";
	final static String KEY_SETUP_TIME = "keySetupTime";
	final static String LOOK_AND_FEEL = "lookAndFeel";
}
