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

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import com.ceperman.pcsecrets.SecurityUtils.CipherInfo;
import com.ceperman.utils.Bytes;

/**
 * PCSecrets main class
 * 
 * @author Chris Wood
 */

public class PCSecrets {
	private static final Logger logger = Logger.getLogger(PCSecrets.class.getName());
	
	private SecretsProperties props;
	private StoredSecretsCollection storedSecretsCollection;
	private MainWindow mainWindow;
	private JList<HostSecret> jSecretsList;	
	private InputForm inputForm;
	
	/* message area */
	private JTextArea areaMsg;
	
	private boolean initOK;
	
	/**
	 * Constructor
	 * 
	 * This processes any args, initiates logging and sets up the properties.
	 * 
	 * Logging level can be set using the /l level arg
	 * PCSecrets dir can be set using /d (relative) or /D (absolute) arg
	 * 
	 * @param args
	 */
	public PCSecrets(String[] args) {
	   String logParmLevel = null;
	   String dir = null;
	   boolean dirFull = false;
	   
	   for (int i = 0; i < args.length; i++) {
         if (args[i].startsWith("/") && args.length > i+1) {
            if (args[i].equalsIgnoreCase("/l")) {
               logParmLevel = args[i+1].toUpperCase();
            } else if (args[i].equalsIgnoreCase("/d")) {
               dir = args[i+1];
               if (args[i].equals("/D")) {
                  dirFull = true;
               }
            }
            i++;
         } else {
            String errorMsg = MessageFormat.format(Messages.getString("PCSecrets.argerror"), args[i]);
            System.out.println(errorMsg);
            return;
         }
      }
	   
	   /*
	    * Logging cannot be properly initialised until the props are accessed to
	    * find the logging level
	    */
		props = SecretsProperties.getInitialInstance(dir, dirFull);
		initLogging(logParmLevel);
		logger.log(Level.INFO, "Args: " + Arrays.toString(args));
		// log any buffered records
		List<LogRecord> logRecords = props.getLogRecords();
		for (LogRecord logRecord : logRecords) {
         logger.log(logRecord);
      }
		props.logSystemProperties();
		SecurityUtils.checkBCProvider();
		
		initOK = true;
	}

	   /**
    * Start main execution here.
    * 
    * If there is no saved secrets file then assumed this to be the first time through. Request the (initial) password.
    * Otherwise request the password with an option to reset it.
    * 
    * If the secrets file already exists then ciphers are created from data in the file header, and the secrets are
    * decrypted. Otherwise fresh ciphers are created in the same manner as secrets-for-android. 
    * - create secrets file if not already existing 
    * - create save/load ciphers 
    * - adjust and show main window
    * 
    * The password has to be saved in some form so decryption ciphers for as yet unseen salt/round combinations can be
    * created. The password is held as a byte array wherever possible as strings are potentially more discoverable
    * (maybe!).
    * 
    * @throws IOException
    */
	public void execute() throws IOException {
	   if (!initOK) return;
	   try {
         for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
            logger.log(Level.FINE, "Installed L&F: " + info.getClassName() + " " + info.getName());
         }
         logger.log(Level.FINE, "System L&F: " + UIManager.getSystemLookAndFeelClassName());
         logger.log(Level.FINE, "Cross-platform L&F: " + UIManager.getCrossPlatformLookAndFeelClassName());
         String lookAndFeelClassName = props.getProperty(Constants.LOOK_AND_FEEL);
         UIManager.setLookAndFeel(lookAndFeelClassName);
         logger.log(Level.FINE, "L&F set to " + lookAndFeelClassName);
      } catch (ClassNotFoundException e) {
         try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            logger.log(Level.SEVERE,
                        "PCSecrets: prefs L&F not found, setting cross-platform L&F "
                                    + UIManager.getCrossPlatformLookAndFeelClassName());
         } catch (Exception e1) {
            // ignore
         }
      } catch (Exception e) {
         logger.log(Level.SEVERE, "PCSecrets: Error setting new look and feel - " + e.toString());
      }
		/* create the stored secrets collection */
		storedSecretsCollection = new StoredSecretsCollection();
		String secretsLocation = props.getProperty(Constants.SECRETS_DIR) + props.getProperty(Constants.SECRETS_FILENAME);
		logger.log(Level.INFO, "Secrets file: " + secretsLocation);
		storedSecretsCollection.setSourceName(secretsLocation);
		mainWindow = new MainWindow(storedSecretsCollection, props);
		
		File secretsFile = new File(storedSecretsCollection.getSourceName());
		boolean firstTime = !secretsFile.exists();
		
		/* get the password, exit if the user cancels */
		byte[] pswd = null;
		if (firstTime) {
		   pswd = UIUtils.getConfirmedPassword(mainWindow, "UIUtils.passwordtitle", "UIUtils.provideinitialpassword");
		} else {
		   pswd = UIUtils.getPassword(mainWindow, Messages.getString("UIUtils.enterpassword"));
		}
		if (pswd == null) {
			logger.log(Level.INFO, "User cancelled password request");
			System.exit(0);
		}
		storedSecretsCollection.setPswdBytes(pswd);
		
		/* if first time, create initial ciphers */
		if (firstTime) {
		   secretsFile.createNewFile();
		   // check if short key will be used
		   int retval = SecurityUtils.checkEncryptionKeyLength();
		   if (retval == 1) {
            JOptionPane.showMessageDialog(mainWindow, Messages.getString("PCSecrets.shortkeymsg"),
                        Messages.getString("PCSecrets.shortkeymsgtitle"), JOptionPane.WARNING_MESSAGE);
		   }
			boolean restartNeeded = saveInitial();
			if (restartNeeded) {
				UIUtils.restartDialog(mainWindow);
				logger.log(Level.INFO, "Restart required after init with two passwords");
				System.exit(0);
			}
		} else { /* if not first time, use password to decrypt existing file */
		   /* try to load the file, if fails assume wrong password */
         boolean loaded = storedSecretsCollection.load();
         while (!loaded) {
            logger.log(Level.INFO, "Decryption failed with password entered");
            /* request the password again, offer RESET option. Exit if the user cancels */
            int retval = UIUtils.getOrResetPassword(mainWindow);
            if (retval == UIUtils.CANCEL_OPTION) {
               logger.log(Level.INFO, "User cancelled password request");
               System.exit(0);
            }
            if (retval == UIUtils.RESET_OPTION) {
               int ret = JOptionPane.showConfirmDialog(mainWindow,
                           Messages.getString("UIUtils.confirmresettext"), Messages.getString("UIUtils.confirmresettitle"),
                           JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
               if (ret != 0) {
                  logger.log(Level.INFO, "User cancelled reset password request");
                  System.exit(0);
               }

               pswd = UIUtils.getResetPassword(mainWindow);
               if (pswd == null) {
                  logger.log(Level.INFO, "User cancelled reset password request");
                  System.exit(0);
               }
               logger.log(Level.INFO, "User reset password");
               boolean restartNeeded = saveInitial();
               if (restartNeeded) {
                  UIUtils.restartDialog(mainWindow);
                  logger.log(Level.INFO, "Restart required after init with two passwords");
                  System.exit(0);
               }
               break;
            } else {
               /* try to load the file again */
               loaded = storedSecretsCollection.load();
            }
         }
         
         storedSecretsCollection.setChanged(false); // reset changed indicator after load
         mainWindow.getAreaMsg().setText(MessageFormat.format(Messages.getString("DataHandler.loaded"), storedSecretsCollection.getSize()));
		}

		/* Place main window in centre of screen */
		Dimension maxWindow = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().getSize();
		/* ensure window not too big for screen (in case notebook) */
		
		Dimension windowSize = mainWindow.getPreferredSize();
		windowSize.height = Math.min(windowSize.height, maxWindow.height);
		mainWindow.setSize(windowSize);
		mainWindow.setLocation(maxWindow.width/2 - windowSize.width/2, maxWindow.height/2 - windowSize.height/2);		
		
		/* pack the window and display it */
		mainWindow.pack();
		mainWindow.setVisible(true);
	}

	/**
	 * Main entry point
	 * 
	 * @param args
	 */
	public static void main(final String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
			   try {
			      PCSecrets pcSecrets = new PCSecrets(args);
			      pcSecrets.execute();
			   } catch (IOException e) {
			      final String msg = e.getMessage();
			      JOptionPane.showMessageDialog(null, msg);
			      logger.log(Level.SEVERE, msg);
			      System.exit(0);
			   } catch (Throwable t) {
			      logger.log(Level.SEVERE, t.toString(), t);
			      System.exit(0);
			   }
			}
		});
	}
	
	/*
	 * Initialisation of the stored secrets file, using the initial password(s).
	 * 
	 * There may be one or two passwords. If a second password is provided, then
	 * the empty collection is encrypted using a cipher based on this, and stored
	 * as the undecrypted section of the collection. The collection is then saved
	 * as per regular processing for one password. The second cipher must use the
	 * same salt and rounds created for the first.
	 * 
	 * If two passwords are provided then the user must restart the program and
	 * select one to use for the current execution.
    * 
    * A section of junk data is always included to make the presence of a second
    * password impossible to determine from the data length. The junk section is
    * inaccessible, it cannot be removed or resized. For more info, see
    * StoredSecretsCollection.
	 * 
	 * @return true if two passwords supplied, false otherwise
	 * @throws IOException
	 */
	private boolean saveInitial() throws IOException {
		/* possibly split password data into two passwords, removing any leading spaces */
		byte[][] passwords = Bytes.splitOnce(storedSecretsCollection.getPswdBytes());
		/* handle the first (or only) password */
		CipherInfo cipherInfo = SecurityUtils.createCiphers(passwords[0], null);
		/* define the cipher text area for the second password - this will be
		 * empty if no second password is supplied */
		byte[] encrypted2 = new byte[0];
		
		if (passwords.length == 2) {  // handle second password
			/* create the undecryptable cipher text */
			passwords[1] = Bytes.trim(passwords[1]); // ensure second pswd trimmed of spaces
			// create cipher for second password using same salt and rounds
			// store it temporarily so the collection will use it for the second secrets set
			// (it will be reset later)
			storedSecretsCollection.setCipherInfo(SecurityUtils.createCiphers(passwords[1], cipherInfo.parms));
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			SecurityUtils.writeSecurityHeader(cipherInfo.parms, baos);
			storedSecretsCollection.writeEncryptedData(baos);
			encrypted2 = baos.toByteArray();
		}
		byte[] junk = createJunk(cipherInfo.parms.salt); // create junk section, use salt as random seed
		logger.log(Level.FINE, "saveInitial: junk length " + junk.length);
		byte[] undecryptableAll = new byte[encrypted2.length + junk.length];
		System.arraycopy(encrypted2, 0, undecryptableAll, 0, encrypted2.length);
		System.arraycopy(junk, 0, undecryptableAll, encrypted2.length, junk.length);
		storedSecretsCollection.setUndecryptedBytes(Bytes.reverseBits(undecryptableAll));
		storedSecretsCollection.setCipherInfo(cipherInfo); // set correct cipher set for first pswd
		storedSecretsCollection.save();
		return passwords.length == 2;
	}
	
	/*
	 * Create random data of random length between 2k and 10k
	 * @return byte[]
	 */
	private byte[] createJunk(byte[] seed) {
		byte[] junkBytes = null;
		Random rand = new Random(Bytes.getLong(seed));
		junkBytes = new byte[rand.nextInt(8192) + 2048];
		rand.nextBytes(junkBytes);
		return junkBytes;
	}
	
	/*
	 * Initialise logging.
	 * 
	 * If a logging level is provided, also set the log level.
	 * 
	 * If init fails, just continue without logging.
	 */
	private void initLogging(String parmLevel) {
		String logParms = "handlers=java.util.logging.FileHandler, java.util.logging.ConsoleHandler \n" +
		  				  "java.util.logging.FileHandler.formatter=java.util.logging.SimpleFormatter";
		try {
			LogManager.getLogManager().readConfiguration(new ByteArrayInputStream(logParms.getBytes()));
			setLoggingLevel(parmLevel);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * Set logging level after properties initialisation
	 * 
	 * This requires the creation of our own root logger to set the log level on.
	 * Setting this on the global root logger would affect logging from all classes, 
	 * not just ours. No only would this cause unwanted logging, but for log levels
	 * below FINE causes real performance problems because of logging from system
	 * components.
	 */
	private void setLoggingLevel(String parmLevel) {
      String loglevel = parmLevel != null ? parmLevel : 
                  (props != null ? props.getProperty(Constants.LOG_LEVEL) : "");
		if (loglevel.length() > 0) {
			Logger myRootLogger = Logger.getLogger("com.ceperman");
			myRootLogger.setLevel(Level.parse(loglevel));
			myRootLogger.setUseParentHandlers(false);
			LogManager.getLogManager().addLogger(myRootLogger);
			Handler handler = null;
			try {
				handler = new java.util.logging.FileHandler();
				handler.setLevel(Level.parse(loglevel));
				myRootLogger.addHandler(handler);
				handler = new java.util.logging.ConsoleHandler();
				handler.setLevel(Level.parse(loglevel));
				myRootLogger.addHandler(handler);
				logger.log(Level.INFO, "Log level set to " + loglevel);
			} catch (Exception e) {
				logger.log(Level.WARNING, "Problem setting log level: " + e.getMessage());
				System.out.println("Problem setting log level: " + e.getMessage());
			}			
		}
	}

	/**
	 * @return the areaMsg
	 */
	public JTextArea getAreaMsg() {
		return areaMsg;
	}

	/**
	 * @return the storedSecretsCollection
	 */
	public SecretsCollection getListModel() {
		return storedSecretsCollection;
	}
	
	/**
	 * @return the jSecretsList
	 */
	public JList<HostSecret> getjSecretsList() {
		return jSecretsList;
	}
	
	/**
	 * @return the inputForm
	 */
	public InputForm getInputForm() {
		return inputForm;
	}
}
