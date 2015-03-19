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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog.ModalityType;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.WindowConstants;

/**
 * Secrets preferences. Handles properties and dialog to
 * customize them.
 * 
 * This class is intended to be a singleton, hence a private
 * constructor.
 * 
 * @author Chris Wood
 */
public class SecretsProperties implements ActionListener {
	private static Logger logger = Logger.getLogger(SecretsProperties.class.getName());
	
	private static final String VERSION_BUNDLE_NAME = "com.ceperman.pcsecrets.version";
	
	@SuppressWarnings("javadoc")
   public static SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss");
	
	/* fixed properties */
	private static final String secretsFileName = "secrets.dat";
	private static final String propsFileName = "pcsecrets.props";
   
   private static final String DEFAULT_PCS_DIR = "pcsecrets"; // default subdir name
   private static final String DEFAULT_PCS_DIR_WIN = "PCSecrets";
   
   /** Logged system properties */
   public static final String[] loggedProps = {
      "java.runtime.name",
      "java.vm.vendor",
      "java.version",
      "java.runtime.version",
      "sun.arch.data.model",
      "java.class.path",
      "java.home",
      "user.dir",
      "user.home",
      "user.country",
      "user.language",
      "os.name",
      "os.version",
      "os.arch"
      };
   
   /* supported languages */
   private static String[] languages = { "SecretsProperties.languagedef", "SecretsProperties.languageen", "SecretsProperties.languagefr" };
   private static String[] locales = { "default", "en", "fr" };
   
   /* these are accessed from static methods */
   private static String pcsecretsDir;
   private static boolean fullPath;
	
	/* customizable properties */
	private static Properties defaultProps = new Properties();
  
	private static SecretsProperties theInstance;
	private Properties props = new Properties();
   private File propsFile;
   private List<LogRecord> logRecords = new ArrayList<LogRecord>();
	
	private Frame parent;
	private PropertyChangeSupport pcs;
	
	/* map class names to L&F string descriptors */
	static final Map<String, String> lookandfeelInfo = new HashMap<String, String>();
   /* L&F lookup maps */
	static final Map<String, String> landfNameToClass = new HashMap<String, String>();
	static final Map<String, String> landfClassToName = new HashMap<String, String>();
	
	static String defaultLAFClassName = UIManager.getCrossPlatformLookAndFeelClassName();
	private static final String NIMBUS = "Nimbus"; // name of default LAF (if installed)
	
	/* Dialog fields */
	private JDialog dialog;
	private JTextField fieldServerPort;
	private JTextField fieldClientPort;
	private JTextField fieldBackupDir;
	private JRadioButton useCurrentPswdButton;
	private JRadioButton promptForPswdButton;
	private JRadioButton logSevereButton;
	private JRadioButton logWarningButton;
	private JRadioButton logInfoButton;
	private JRadioButton logFineButton;
	private JCheckBox enableBackupCheckBox = new JCheckBox();
	private JCheckBox enableTimeoutCheckBox = new JCheckBox();
	private JCheckBox saveOnTimeoutCheckBox = new JCheckBox();
	private JCheckBox suppressSyncDialogCheckBox = new JCheckBox();
	private JTextField fieldBackupCount;
	private JButton selectDirButton;
   private JTextField fieldKeySetupTime;
   private JComboBox<String> fieldLookAndFeels;
   private JTextArea lAndFDescription = new JTextArea();
   private JTextField fieldTimeoutTime;
   private JComboBox<String> fieldLanguages;
	
	/* Accessible for enable/disable */
	private JLabel fieldBackupDirDesc;
	private JLabel fieldBackupCountText;
	
	private JLabel fieldTimeoutTimeText;
	private JLabel fieldSaveOnTimeoutText;
	
	/* Actions */
	private static final String SELECT_BACKUP_DIR = "selectBackupDir";
	private static final String ENABLE_BACKUP = "enableBackup";
	
	/**
	 * Constructor
	 * 
	 * Ensure properties file exists.
	 * Load in existing properties if any.
	 * 
	 * The Log is not available during the constructor, so log messages are
	 * buffered.
	 * 
	 * @param dir properties file directory (from command line arg)
	 * @param fullPath true = full path
	 */
	private SecretsProperties(String pcsDir, boolean fullPath) {
	   if (pcsDir != null && pcsDir.length() > 0) {
	      pcsecretsDir = pcsDir;
	      SecretsProperties.fullPath = fullPath;
	   } else {
	      pcsecretsDir = DEFAULT_PCS_DIR;
	   }
      
      // setup LAF info map
      lookandfeelInfo.put("javax.swing.plaf.metal.MetalLookAndFeel", "SecretsProperties.metalinfo");
      lookandfeelInfo.put("com.sun.java.swing.plaf.motif.MotifLookAndFeel", "SecretsProperties.motifinfo");
      lookandfeelInfo.put("com.sun.java.swing.plaf.gtk.GTKLookAndFeel", "SecretsProperties.gtkinfo");
      lookandfeelInfo.put("com.sun.java.swing.plaf.windows.WindowsLookAndFeel", "SecretsProperties.wininfo");
      lookandfeelInfo.put("com.sun.java.swing.plaf.windows.WindowsClassicLookAndFeel", "SecretsProperties.winclassicinfo");
      // the following is documented by Oracle but not what you see on Mac OS X 10.6.8 at least
      lookandfeelInfo.put("com.sun.java.swing.plaf.mac.MacLookAndFeel", "SecretsProperties.macinfo");
      // on Mac OS X 10.6.8 at least
      lookandfeelInfo.put("com.apple.laf.AquaLookAndFeel", "SecretsProperties.macinfo");
      // Sun/Oracle prior to 1.6.10
      lookandfeelInfo.put("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel", "SecretsProperties.nimbusinfo");
      // OpenJDK & Sun/Oracle 1.6.10+
      lookandfeelInfo.put("javax.swing.plaf.nimbus.NimbusLookAndFeel", "SecretsProperties.nimbusinfo");
      
      // setup LAF name <-> class maps
      for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
         landfNameToClass.put(info.getName(), info.getClassName());
         landfClassToName.put(info.getClassName(), info.getName());
         if (info.getName().equalsIgnoreCase(NIMBUS)) {
            defaultLAFClassName = info.getClassName();
         }
      }
	   
	   defaultProps = createDefaultProps();
	   String path = createFullPathDir();
	   logRecords.add(new LogRecord(Level.INFO, "PCSecrets path: " + path));
	   String fileName = path + propsFileName;
	   /* ensure dir exists */
	   File dir = new File(createFullPathDir());
	   if (!dir.exists()) {
	      dir.mkdir();
	   }
	   props = new Properties(defaultProps);
	   propsFile = new File(fileName);
	   FileReader fr = null;
	   try {
	      if (propsFile.createNewFile()) {
	         saveProperties();
	         logRecords.add(new LogRecord(Level.FINE, "Created properties file: " + fileName));
	      }
	      fr = new FileReader(propsFile);
	      props.load(fr);
	      logRecords.add(new LogRecord(Level.FINE, "Loaded properties file: " + fileName));
	   } catch (IOException e) {
	      logRecords.add(new LogRecord(Level.WARNING, "IOException loading properties file: " + fileName + " - " + e.getMessage()));
	      logRecords.add(new LogRecord(Level.WARNING, "Defaults will be used"));
	   }
	   String language = props.getProperty(Constants.LANGUAGE);
	   for (int i = 0; i < locales.length; i++) {
         if (locales[i].equals(language)) {
            Locale.setDefault(new Locale(locales[i]));
         }
      }
	   /* replace the language name with its translation */
	   for (int i = 0; i < languages.length; i++) {
         languages[i] = Messages.getString(languages[i]);
      }
	}

   private Properties createDefaultProps() {
      Properties defaultProps = new Properties();
      defaultProps.put(Constants.SECRETS_DIR, createFullPathDir());
      defaultProps.put(Constants.SECRETS_FILENAME, secretsFileName);
      defaultProps.put(Constants.LOG_LEVEL, "WARNING");
      defaultProps.put(Constants.SERVERPORT, "9100"); // local port
      defaultProps.put(Constants.CLIENTPORT, "9101"); // remote port - on the phone (this is the server port)
      defaultProps.put(Constants.SYNCUSESAMEPSWD, "true");
      defaultProps.put(Constants.SUPPRESS_SYNCDIALOG, "true");
      defaultProps.put(Constants.KEYLENGTH, "256");
      defaultProps.put(Constants.MAXKEYLENGTH, "256");
      defaultProps.put(Constants.BACKUP_ENABLED, "false");
      defaultProps.put(Constants.BACKUP_DIR, createDefaultBackupDir());
      defaultProps.put(Constants.MAX_BACKUP_COUNT, "5");
      defaultProps.put(Constants.KEY_SETUP_TIME, "1000");
      defaultProps.put(Constants.LOOK_AND_FEEL, defaultLAFClassName);
      defaultProps.put(Constants.LANGUAGE, "default");
      defaultProps.put(Constants.TIMEOUT_ENABLED, "false");
      defaultProps.put(Constants.TIMEOUT_TIME, "30");
      defaultProps.put(Constants.SAVE_ON_TIMEOUT, "true");
      defaultProps.put(Constants.LANGUAGE, "default");
      return defaultProps;
   }
  
  /**
   * Create and return the singleton instance of this class.
   * If the instance already exists, it is set to null and a
   * NullPointerException is thrown.
   * @param dir properties file directory
   * @param dirFull true = full path, false = PCSecrets dir only
   * @return SecretsProperties
   */
  public static synchronized SecretsProperties getInitialInstance(String dir, boolean dirFull) {
    if (theInstance == null) {
      theInstance = new SecretsProperties(dir, dirFull);
    } else {
      theInstance = null;
      throw new NullPointerException();
    }
    return theInstance;
  }
	
	/**
	 * Return the singleton instance of this class.
	 * If the instance does not exist, a NullPointerException is thrown.
	 * @return SecretsProperties
	 */
	public static synchronized SecretsProperties getInstance() {
		if (theInstance == null) {
			throw new NullPointerException();
		}
		return theInstance;
	}
	
	/**
	 * Log system properties
	 */
	public void logSystemProperties() {
      logger.log(Level.INFO, "System properties:");
      Properties props = System.getProperties();
      for (int i = 0; i < loggedProps.length; i++) {
         logger.log(Level.INFO, loggedProps[i] + " = " + props.getProperty(loggedProps[i]));
      }
	}

	/* Dialog actions (accept APPLY) do NOT change the property settings.
	 */
	@Override
   public void actionPerformed(ActionEvent event) {
      logger.log(Level.INFO, "command is " + event.getActionCommand());
      if (event.getActionCommand().equals(Constants.OK)) {
         applyChanges();
         dialog.dispose();
      } else if (event.getActionCommand().equals(Constants.APPLY)) {
         applyChanges();
      } else if (event.getActionCommand().equals(Constants.DEFAULTS)) {
         setDefaults();
      } else if (event.getActionCommand().equals(Constants.CANCEL)) {
         dialog.dispose();
      } else if (event.getActionCommand().equals(ENABLE_BACKUP)) {
         enableBackupFields(enableBackupCheckBox.isSelected());
      } else if (event.getActionCommand().equals(SELECT_BACKUP_DIR)) {
         selectBackupDir();
      }
   }

	/**
	 * Get the preference value for the key provided
	 * @param key
	 * @return value or default
	 */
	public String getProperty(String key) {
		return props.getProperty(key);
	}

   /**
    * Get the default preference value for the key provided
    * @param key
    * @return default
    */
   public String getDefaultProperty(String key) {
      return defaultProps.getProperty(key);
   }
	
	/**
	 * Update the preference value for the key provided
	 * @param key
	 * @param value
	 */
	public void updateProperty(String key, String value) {
		props.setProperty(key, value);
		saveProperties();
	}
	
	/**
	 * Is platform Windows?
	 * @return true if yes, false otherwise
	 */
	public static boolean isWindows() {
	   String osName = System.getProperty("os.name", "unknown").toLowerCase();
      return (osName.indexOf("windows") >= 0);
	}
   
   /**
    * Is platform Mac?
    * @return true if yes, false otherwise
    */
   public static boolean isMac() {
      String osName = System.getProperty("os.name", "unknown").toLowerCase();
      return (osName.startsWith("mac") || osName.startsWith("darwin"));
   }
   
   /**
    * Is platform Linux?
    * This method is for determining UI characteristics. As such, we make the
    * bold and questionable assumption that anything that isn't Windows or Mac
    * is Linux. This will therefore include all *nixes.
    * @return true if yes, false otherwise
    */
   public static boolean isLinux() {
      return (!(isWindows() || isMac()));
   }

	/**
	 * Build and show the preferences dialog 
	 * @param parent
	 */
	public void showDialog(Frame parent) {
	   this.parent = parent;
		dialog = new JDialog();
		dialog.setTitle(Messages.getString("SecretsProperties.title"));
		dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		dialog.setModalityType(ModalityType.APPLICATION_MODAL);
		
		// initialize support for firing property changes at the main window
		if (pcs == null) {
		   pcs = new PropertyChangeSupport(this);
		   pcs.addPropertyChangeListener((PropertyChangeListener)parent);
		}
		
		Container cp = dialog.getContentPane();
		cp.setLayout(new BoxLayout(cp, BoxLayout.PAGE_AXIS));
		
		JTabbedPane tabbedPane = new JTabbedPane();
		cp.add(tabbedPane);
		
		/* create panes for associated properties */
		tabbedPane.addTab(Messages.getString("SecretsProperties.synctitle"), null,createSyncPane()); 
		tabbedPane.addTab(Messages.getString("SecretsProperties.backuptitle"), null,createBackupPane());
		tabbedPane.addTab(Messages.getString("SecretsProperties.keysetuptitle"), null,createKeySetupPane());
		tabbedPane.addTab(Messages.getString("SecretsProperties.lookandfeeltitle"), null,createLookAndFeelPane());   
      tabbedPane.addTab(Messages.getString("SecretsProperties.logtitle"), null, createLogPane());
      tabbedPane.addTab(Messages.getString("SecretsProperties.timeouttitle"), null, createTimeoutPane());
      tabbedPane.addTab(Messages.getString("SecretsProperties.languagetitle"), null, createLanguagePane());
    
		/* create the bottom buttons */
		JPanel actionButtons = new JPanel(new FlowLayout());
		JButton jButtonOk = new JButton(Messages.getString("PCSecrets.ok"));
		jButtonOk.setToolTipText(Messages.getString("SecretsProperties.oktooltip"));
		jButtonOk.setActionCommand(Constants.OK);
		jButtonOk.setMnemonic(KeyEvent.VK_K);
		jButtonOk.addActionListener(this);
		actionButtons.add(jButtonOk);
		JButton jButtonApply = new JButton(Messages.getString("PCSecrets.apply"));
		jButtonApply.setToolTipText(Messages.getString("SecretsProperties.applytooltip"));
		jButtonApply.setActionCommand(Constants.APPLY);
		jButtonApply.setMnemonic(KeyEvent.VK_A);
		jButtonApply.addActionListener(this);
		actionButtons.add(jButtonApply);
		JButton jButtonDefaults = new JButton(Messages.getString("SecretsProperties.defaults"));
		jButtonDefaults.setToolTipText(Messages.getString("SecretsProperties.defaultstooltip"));
		jButtonDefaults.setActionCommand(Constants.DEFAULTS);
		jButtonDefaults.setMnemonic(KeyEvent.VK_D);
		jButtonDefaults.addActionListener(this);
		actionButtons.add(jButtonDefaults);
		JButton jButtonCancel = new JButton(Messages.getString("PCSecrets.cancel"));
		jButtonCancel.setToolTipText(Messages.getString("SecretsProperties.canceltooltip"));
		jButtonCancel.setActionCommand(Constants.CANCEL);
		jButtonCancel.setMnemonic(KeyEvent.VK_C);
		jButtonCancel.addActionListener(this);
		actionButtons.add(jButtonCancel);
		cp.add(actionButtons);
		
      dialog.pack();
		dialog.setLocationRelativeTo(parent);
		dialog.setVisible(true);
	}

	/**
	 * Create pane for sync properties
	 */
	private JPanel createSyncPane() {
	   JPanel syncPane = new JPanel();
	   syncPane.setLayout(new BorderLayout());

	   /* sync panel */
	   /* create titled border with inner and outer spacing */
	   syncPane.setBorder(BorderFactory.createCompoundBorder(
	               BorderFactory.createEmptyBorder(5,5,5,5),
	               BorderFactory.createCompoundBorder(
	                           BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.gray), Messages.getString("SecretsProperties.synctitle")), 
	                           BorderFactory.createEmptyBorder(5,5,5,5))));
	   /* create values panel */
	   JPanel portsPanel = new JPanel();
	   syncPane.add(portsPanel, BorderLayout.NORTH);
	   portsPanel.setLayout(new GridLayout(2, 2));
	   portsPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,50));

	   JLabel labelDescr = new JLabel(Messages.getString("SecretsProperties.serverport"), JLabel.LEFT);
	   portsPanel.add(labelDescr);

	   fieldServerPort = new JTextField(getProperty(Constants.SERVERPORT));
	   fieldServerPort.setColumns(6);
	   portsPanel.add(fieldServerPort);

	   JLabel labelId = new JLabel(Messages.getString("SecretsProperties.clientport"), JLabel.LEFT);
	   portsPanel.add(labelId);

	   fieldClientPort = new JTextField(getProperty(Constants.CLIENTPORT));
	   fieldClientPort.setColumns(6);
	   portsPanel.add(fieldClientPort);

	   JPanel radioPanel = new JPanel(new BorderLayout());
	   syncPane.add(radioPanel, BorderLayout.CENTER);

	   // create the sync pswd radio buttons.
	   useCurrentPswdButton = new JRadioButton(Messages.getString("SecretsProperties.usecurrent"));
	   promptForPswdButton = new JRadioButton(Messages.getString("SecretsProperties.prompt"));
	   if ((getProperty(Constants.SYNCUSESAMEPSWD)).equals("true")) {
	      useCurrentPswdButton.setSelected(true);
	   } else {
	      promptForPswdButton.setSelected(true);
	   }

	   ButtonGroup radioButtons = new ButtonGroup();
	   radioButtons.add(useCurrentPswdButton);
	   radioButtons.add(promptForPswdButton);

	   radioPanel.add(useCurrentPswdButton, BorderLayout.NORTH);
	   radioPanel.add(promptForPswdButton, BorderLayout.SOUTH);

	   /* create the show dialog checkbox */
	   JPanel syncDialogCheckboxArea = new JPanel(new FlowLayout(FlowLayout.LEFT));
	   syncPane.add(syncDialogCheckboxArea, BorderLayout.SOUTH);
	   syncDialogCheckboxArea.add(suppressSyncDialogCheckBox);
	   syncDialogCheckboxArea.add(new JLabel(Messages.getString("SecretsProperties.suppresssyncdialog"), JLabel.LEFT));
	   suppressSyncDialogCheckBox.setSelected(getProperty(Constants.SUPPRESS_SYNCDIALOG).equals("true"));
	   return syncPane;
	}

	/**
	 * Create pane for log properties
	 */
   private JPanel createLogPane() {
      ButtonGroup radioButtons;
      /* log panel */
      JPanel logPane = new JPanel(new BorderLayout());
      JPanel logContentPane = new JPanel();
      logContentPane.setLayout(new BoxLayout(logContentPane, BoxLayout.PAGE_AXIS));
      logPane.add(logContentPane, BorderLayout.CENTER);
      /* create titled border with inner and outer spacing */
      logPane.setBorder(BorderFactory.createCompoundBorder(
                  BorderFactory.createEmptyBorder(5, 5, 5, 5),
                  BorderFactory.createCompoundBorder(
                              BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.gray),
                                          Messages.getString("SecretsProperties.logtitle")),
                              BorderFactory.createEmptyBorder(5, 5, 5, 5))));
      logSevereButton = new JRadioButton(Messages.getString("SecretsProperties.logsevere"));
      logWarningButton = new JRadioButton(Messages.getString("SecretsProperties.logwarn"));
      logInfoButton = new JRadioButton(Messages.getString("SecretsProperties.loginfo"));
      logFineButton = new JRadioButton(Messages.getString("SecretsProperties.logfine"));
      if ((getProperty(Constants.LOG_LEVEL)).equals("SEVERE")) {
         logSevereButton.setSelected(true);
      } else if ((getProperty(Constants.LOG_LEVEL)).equals("WARNING")) {
         logWarningButton.setSelected(true);
      } else if ((getProperty(Constants.LOG_LEVEL)).equals("INFO")) {
         logInfoButton.setSelected(true);
      } else if ((getProperty(Constants.LOG_LEVEL)).equals("FINE")) {
         logFineButton.setSelected(true);
      }

      radioButtons = new ButtonGroup();
      radioButtons.add(logSevereButton);
      radioButtons.add(logWarningButton);
      radioButtons.add(logInfoButton);
      radioButtons.add(logFineButton);

      logSevereButton.setAlignmentX(Component.LEFT_ALIGNMENT);
      logContentPane.add(logSevereButton);
      logWarningButton.setAlignmentX(Component.LEFT_ALIGNMENT);
      logContentPane.add(logWarningButton);
      logInfoButton.setAlignmentX(Component.LEFT_ALIGNMENT);
      logContentPane.add(logInfoButton);
      logFineButton.setAlignmentX(Component.LEFT_ALIGNMENT);
      logContentPane.add(logFineButton);
      return logPane;
   }

   /**
    * Create pane for backup properties
    */
   private JPanel createBackupPane() {
      /* backup panel */
      JPanel backupPane = new JPanel(new BorderLayout());
      /* create titled border with inner and outer spacing */
      backupPane.setBorder(BorderFactory.createCompoundBorder(
                  BorderFactory.createEmptyBorder(5, 5, 5, 5),
                  BorderFactory.createCompoundBorder(
                              BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.gray),
                                          Messages.getString("SecretsProperties.backuptitle")),
                              BorderFactory.createEmptyBorder(5, 5, 5, 5))));
      /* enable backup checkbox */
      JPanel enableBackupCheckboxArea = new JPanel(new FlowLayout(FlowLayout.LEFT));
      backupPane.add(enableBackupCheckboxArea, BorderLayout.NORTH);
      enableBackupCheckboxArea.add(enableBackupCheckBox);
      enableBackupCheckBox.setActionCommand(ENABLE_BACKUP);
      enableBackupCheckBox.addActionListener(this);
      enableBackupCheckboxArea.add(new JLabel(Messages.getString("SecretsProperties.enablebackup"), JLabel.LEFT));
      /* backup options pane */
      JPanel backupOptionsPane = new JPanel(new BorderLayout());
      backupPane.add(backupOptionsPane, BorderLayout.CENTER);
      backupOptionsPane.setBorder(BorderFactory.createCompoundBorder(
                  BorderFactory.createEmptyBorder(5, 5, 5, 5),
                  BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.gray),
                              BorderFactory.createEmptyBorder(5, 5, 5, 5))));
      fieldBackupDirDesc = new JLabel(Messages.getString("SecretsProperties.backupdirname"), JLabel.LEFT);
      backupOptionsPane.add(fieldBackupDirDesc, BorderLayout.NORTH);
      JPanel dirPane = new JPanel(new FlowLayout(FlowLayout.LEFT));
      fieldBackupDir = new JTextField(getProperty(Constants.BACKUP_DIR));
      fieldBackupDir.setColumns(25);
      dirPane.add(fieldBackupDir);
      selectDirButton = new JButton(Messages.getString("SecretsProperties.selectdir"));
      selectDirButton.setActionCommand(SELECT_BACKUP_DIR);
      selectDirButton.setMnemonic(KeyEvent.VK_L);
      selectDirButton.addActionListener(this);
      dirPane.add(selectDirButton);
      backupOptionsPane.add(dirPane, BorderLayout.CENTER);
      JPanel backupCountPane = new JPanel(new FlowLayout(FlowLayout.LEFT));
      fieldBackupCountText = new JLabel(Messages.getString("SecretsProperties.backupcounttext"), JLabel.LEFT);
      backupCountPane.add(fieldBackupCountText);
      fieldBackupCount = new JTextField(getProperty(Constants.MAX_BACKUP_COUNT));
      fieldBackupCount.setColumns(2);
      backupCountPane.add(fieldBackupCount);
      backupOptionsPane.add(backupCountPane, BorderLayout.SOUTH);
      enableBackupCheckBox.setSelected(getProperty(Constants.BACKUP_ENABLED).equals("true"));
      enableBackupFields(enableBackupCheckBox.isSelected()); // enable/disable fields
      return backupPane;
   }

   /**
    * Create pane for timeout properties
    */
   private JPanel createTimeoutPane() {
      /* timeout panel */
      JPanel timeoutPane = new JPanel(new BorderLayout());
      /* create titled border with inner and outer spacing */
      timeoutPane.setBorder(BorderFactory.createCompoundBorder(
                  BorderFactory.createEmptyBorder(5, 5, 5, 5),
                  BorderFactory.createCompoundBorder(
                              BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.gray),
                                          Messages.getString("SecretsProperties.timeouttitle")),
                              BorderFactory.createEmptyBorder(5, 5, 5, 5))));
      /* enable timeout checkbox */
      JPanel enableTimeoutCheckboxArea = new JPanel(new FlowLayout(FlowLayout.LEFT));
      timeoutPane.add(enableTimeoutCheckboxArea, BorderLayout.NORTH);
      enableTimeoutCheckboxArea.add(enableTimeoutCheckBox);
      enableTimeoutCheckBox.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent event) {
            enableTimeoutFields(enableTimeoutCheckBox.isSelected());
         }
      });
      enableTimeoutCheckboxArea.add(new JLabel(Messages.getString("SecretsProperties.enabletimeout"), JLabel.LEFT));
      /* timeout options pane */
      JPanel timeoutOptionsPane = new JPanel(new BorderLayout());
      timeoutPane.add(timeoutOptionsPane, BorderLayout.CENTER);
      timeoutOptionsPane.setBorder(BorderFactory.createCompoundBorder(
                  BorderFactory.createEmptyBorder(5, 5, 5, 5),
                  BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.gray),
                              BorderFactory.createEmptyBorder(5, 5, 5, 5))));
      
      JPanel timeoutTimePane = new JPanel(new FlowLayout(FlowLayout.LEFT));
      fieldTimeoutTimeText = new JLabel(Messages.getString("SecretsProperties.idletimetext"), JLabel.LEFT);
      timeoutTimePane.add(fieldTimeoutTimeText);
      fieldTimeoutTime = new JTextField(getProperty(Constants.TIMEOUT_TIME));
      fieldTimeoutTime.setColumns(3);
      timeoutTimePane.add(fieldTimeoutTime);
      timeoutOptionsPane.add(timeoutTimePane, BorderLayout.NORTH);
      /* save on timeout checkbox */
      JPanel saveOnTimeoutCheckboxArea = new JPanel(new FlowLayout(FlowLayout.LEFT));
      timeoutOptionsPane.add(saveOnTimeoutCheckboxArea, BorderLayout.CENTER);
      saveOnTimeoutCheckboxArea.add(saveOnTimeoutCheckBox);
      fieldSaveOnTimeoutText = new JLabel(Messages.getString("SecretsProperties.saveontimeout"), JLabel.LEFT);
      saveOnTimeoutCheckboxArea.add(fieldSaveOnTimeoutText);
      
      enableTimeoutCheckBox.setSelected(getProperty(Constants.TIMEOUT_ENABLED).equals("true"));
      saveOnTimeoutCheckBox.setSelected(getProperty(Constants.SAVE_ON_TIMEOUT).equals("true"));
      enableTimeoutFields(enableTimeoutCheckBox.isSelected()); // enable/disable fields
      return timeoutPane;
   }

   /**
    * Create pane for language setting
    */
   private JPanel createLanguagePane() {
      /* language panel */
      JPanel languagePane = new JPanel(new BorderLayout());
      /* create titled border with inner and outer spacing */
      languagePane.setBorder(BorderFactory.createCompoundBorder(
                  BorderFactory.createEmptyBorder(5, 5, 5, 5),
                  BorderFactory.createCompoundBorder(
                              BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.gray),
                                          Messages.getString("SecretsProperties.languagetitle")),
                              BorderFactory.createEmptyBorder(5, 5, 5, 5))));
      JPanel fieldsPane = new JPanel(new FlowLayout(FlowLayout.LEFT));
      languagePane.add(fieldsPane, BorderLayout.NORTH);
      JLabel labelDescr = new JLabel(Messages.getString("SecretsProperties.languagestxt"), JLabel.LEFT);
      fieldsPane.add(labelDescr);
      
      fieldLanguages = new JComboBox<String>(languages);
      fieldsPane.add(fieldLanguages);
      for (int i = 0; i < locales.length; i++) {
         if ((locales[i]).equals(getProperty(Constants.LANGUAGE))) {
            fieldLanguages.setSelectedIndex(i);
            break;
         }
      }
      JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
      JLabel infoTxt = new JLabel(Messages.getString("SecretsProperties.langinfotxt"), JLabel.LEFT);
      infoPanel.add(infoTxt);
      languagePane.add(infoPanel, BorderLayout.CENTER);
      
      return languagePane;
   }
   
   /**
    * Create pane for key setup properties
    */
   private JPanel createKeySetupPane() {
      JPanel keySetupPane = new JPanel();
      keySetupPane.setLayout(new BoxLayout(keySetupPane, BoxLayout.PAGE_AXIS));
      /* create titled border with inner and outer spacing */
      keySetupPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(5, 5, 5, 5),
                BorderFactory.createCompoundBorder(
                            BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.gray),
                                        Messages.getString("SecretsProperties.keysetuptitle")),
                            BorderFactory.createEmptyBorder(5, 5, 5, 5))));
      JPanel fields1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
      fields1.add(new JLabel(Messages.getString("SecretsProperties.keysetuptime"), JLabel.LEFT));
      fieldKeySetupTime = new JTextField(getProperty(Constants.KEY_SETUP_TIME));
      fieldKeySetupTime.setColumns(6);
      fields1.add(fieldKeySetupTime);
      keySetupPane.add(fields1);
      
      JPanel fields2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
      fields2.add(new JLabel(Messages.getString("SecretsProperties.keylengthinuse"), JLabel.LEFT));
      JLabel fieldKeyLength = new JLabel(getProperty(Constants.KEYLENGTH), JLabel.LEFT);
      fields2.add(fieldKeyLength);
      keySetupPane.add(fields2);
      
      JPanel fields3 = new JPanel(new FlowLayout(FlowLayout.LEFT));
      fields3.add(new JLabel(Messages.getString("SecretsProperties.keylengthmax"), JLabel.LEFT));
      JLabel fieldKeyLengthMax = new JLabel(getProperty(Constants.MAXKEYLENGTH), JLabel.LEFT);
      fields3.add(fieldKeyLengthMax);
      keySetupPane.add(fields3);
      return keySetupPane;
   }
   
   /**
    * Create pane for look and feel properties
    */
   private JPanel createLookAndFeelPane() {
      JPanel lookAndFeelPane = new JPanel(new BorderLayout());
      JPanel lookAndFeelContentPane = new JPanel();
      lookAndFeelContentPane.setLayout(new BoxLayout(lookAndFeelContentPane, BoxLayout.PAGE_AXIS));
      lookAndFeelPane.add(lookAndFeelContentPane, BorderLayout.CENTER);
      /* create titled border with inner and outer spacing */
      lookAndFeelPane.setBorder(BorderFactory.createCompoundBorder(
                  BorderFactory.createEmptyBorder(5, 5, 5, 5),
                  BorderFactory.createCompoundBorder(
                              BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.gray),
                                          Messages.getString("SecretsProperties.lookandfeeltitle")),
                              BorderFactory.createEmptyBorder(5, 5, 5, 5))));
      JPanel fieldsPane = new JPanel(new FlowLayout(FlowLayout.LEFT));
      JLabel labelDescr = new JLabel(Messages.getString("SecretsProperties.lookandfeelavailable"), JLabel.LEFT);
      fieldsPane.add(labelDescr);
      /* enumerate look and feels */
      String currentLandFClassName = getProperty(Constants.LOOK_AND_FEEL);
      String currentLandFName = landfClassToName.get(currentLandFClassName);
      Vector<String> listLandF = new Vector<String>();
      for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
         listLandF.add(info.getName());
      }
      fieldLookAndFeels = new JComboBox<String>(listLandF);
      fieldLookAndFeels.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent event) {
            if (fieldLookAndFeels.getSelectedIndex() > -1) {
               lAndFDescription.setText(getLAFInfo(landfNameToClass.get(fieldLookAndFeels.getSelectedItem())));
            }
         }
      });
      fieldsPane.add(fieldLookAndFeels);
      for (int i = 0; i < listLandF.size(); i++) {
         if ((listLandF.get(i)).equals(currentLandFName)) {
            fieldLookAndFeels.setSelectedIndex(i);
            break;
         }
      }
      String landDefaultText = MessageFormat.format(Messages.getString("SecretsProperties.lookandfeelnative"),
                  landfClassToName.get(UIManager.getSystemLookAndFeelClassName()));
      lookAndFeelPane.add(new JLabel(landDefaultText), BorderLayout.SOUTH);
      lookAndFeelPane.add(fieldsPane, BorderLayout.NORTH);
      lAndFDescription.setBackground(lookAndFeelPane.getBackground());
      lAndFDescription.setWrapStyleWord(true);
      lAndFDescription.setLineWrap(true);
      lAndFDescription.setEditable(false);
      lAndFDescription.setText(getLAFInfo(currentLandFClassName));
      lookAndFeelPane.add(lAndFDescription, BorderLayout.CENTER);
      return lookAndFeelPane;
   }
   
   private String getLAFInfo(String className) {
      if (lookandfeelInfo.containsKey(className)) {
         return Messages.getString(lookandfeelInfo.get(className));
      } else {
         return Messages.getString("SecretsProperties.unknownlaf");
      }
   }
	
	/*
	 * Apply changes in the dialog to the properties
	 * Save properties if they have changed
	 */
   private void applyChanges() {
      boolean changed = false;
      if (!fieldServerPort.getText().equals(getProperty(Constants.SERVERPORT))) {
         props.setProperty(Constants.SERVERPORT, fieldServerPort.getText());
         changed = true;
      }
      if (!fieldClientPort.getText().equals(getProperty(Constants.CLIENTPORT))) {
         props.setProperty(Constants.CLIENTPORT, fieldClientPort.getText());
         changed = true;
      }
      if (getProperty(Constants.SYNCUSESAMEPSWD).equals("true") && promptForPswdButton.isSelected()) {
         props.setProperty(Constants.SYNCUSESAMEPSWD, "false");
         changed = true;
      } else if (getProperty(Constants.SYNCUSESAMEPSWD).equals("false") && useCurrentPswdButton.isSelected()) {
         props.setProperty(Constants.SYNCUSESAMEPSWD, "true");
         changed = true;
      }
      if (!getProperty(Constants.SUPPRESS_SYNCDIALOG)
                  .equals(suppressSyncDialogCheckBox.isSelected() ? "true" : "false")) {
         props.setProperty(Constants.SUPPRESS_SYNCDIALOG, suppressSyncDialogCheckBox.isSelected() ? "true" : "false");
      }
      /* log settings */
      if ((logSevereButton.isSelected() && (!(getProperty(Constants.LOG_LEVEL)).equals("SEVERE")))) {
         props.setProperty(Constants.LOG_LEVEL, "SEVERE");
         changed = true;
      } else if ((logWarningButton.isSelected() && (!(getProperty(Constants.LOG_LEVEL)).equals("WARNING")))) {
         props.setProperty(Constants.LOG_LEVEL, "WARNING");
         changed = true;
      } else if ((logInfoButton.isSelected() && (!(getProperty(Constants.LOG_LEVEL)).equals("INFO")))) {
         props.setProperty(Constants.LOG_LEVEL, "INFO");
         changed = true;
      } else if ((logFineButton.isSelected() && (!(getProperty(Constants.LOG_LEVEL)).equals("FINE")))) {
         props.setProperty(Constants.LOG_LEVEL, "FINE");
         changed = true;
      }
      /* backup settings */
      if (!getProperty(Constants.BACKUP_ENABLED).equals(enableBackupCheckBox.isSelected() ? "true" : "false")) {
         props.setProperty(Constants.BACKUP_ENABLED, enableBackupCheckBox.isSelected() ? "true" : "false");
         changed = true;
      }
      if (!getProperty(Constants.BACKUP_DIR).equals(fieldBackupDir.getText())) {
         if (validateDir(fieldBackupDir.getText())) {
            props.setProperty(Constants.BACKUP_DIR, fieldBackupDir.getText());
            changed = true;
         } else {
            fieldBackupDir.setText(defaultProps.getProperty(Constants.BACKUP_DIR));
         }
      }
      if (!getProperty(Constants.MAX_BACKUP_COUNT).equals(fieldBackupCount.getText())) {
         props.setProperty(Constants.MAX_BACKUP_COUNT, fieldBackupCount.getText());
         changed = true;
      }
      /* key setup settings */
      if (!getProperty(Constants.KEY_SETUP_TIME).equals(fieldKeySetupTime.getText())) {
         props.setProperty(Constants.KEY_SETUP_TIME, fieldKeySetupTime.getText());
         changed = true;
      }
      /* look and feel settings */
      String newLookAndFeel = (String)fieldLookAndFeels.getSelectedItem();
      String newLookAndFeelClassName = landfNameToClass.get(newLookAndFeel);
      if (!getProperty(Constants.LOOK_AND_FEEL).equals(newLookAndFeelClassName)) {
         props.setProperty(Constants.LOOK_AND_FEEL, newLookAndFeelClassName);
         try {
            UIManager.setLookAndFeel(newLookAndFeelClassName);
            SwingUtilities.updateComponentTreeUI(parent);
            SwingUtilities.updateComponentTreeUI(dialog);
            parent.pack();
            dialog.pack();
         } catch (Exception e) {
            logger.log(Level.SEVERE, "SecretsProperties: Error setting new look and feel - " + e.toString());
         }
         changed = true;
      }
      /* timeout settings */
      if (!getProperty(Constants.TIMEOUT_ENABLED).equals(enableTimeoutCheckBox.isSelected() ? "true" : "false")) {
         props.setProperty(Constants.TIMEOUT_ENABLED, enableTimeoutCheckBox.isSelected() ? "true" : "false");
         pcs.firePropertyChange(Constants.TIMEOUT_ENABLED, null, null); // values are not used
         changed = true;
      }
      if (!getProperty(Constants.TIMEOUT_TIME).equals(fieldTimeoutTime.getText())) {
         props.setProperty(Constants.TIMEOUT_TIME, fieldTimeoutTime.getText());
         changed = true;
      }
      if (!getProperty(Constants.SAVE_ON_TIMEOUT).equals(saveOnTimeoutCheckBox.isSelected() ? "true" : "false")) {
         props.setProperty(Constants.SAVE_ON_TIMEOUT, saveOnTimeoutCheckBox.isSelected() ? "true" : "false");
         changed = true;
      }
      /* language setting */
      int selectedIndex = fieldLanguages.getSelectedIndex();
      if (!getProperty(Constants.LANGUAGE).equals(languages[selectedIndex])) {
         props.setProperty(Constants.LANGUAGE, locales[selectedIndex]);
         if (selectedIndex > 0) {
            Locale.setDefault(new Locale(locales[selectedIndex]));
         } else {
            Locale.setDefault(Locale.getDefault());
         }
         changed = true;
      }
      
      if (changed) {
         saveProperties();
      }
   }
	
	/*
	 * Set default values into the dialog
	 */
	private void setDefaults() {
		fieldServerPort.setText(defaultProps.getProperty(Constants.SERVERPORT));
		fieldClientPort.setText(defaultProps.getProperty(Constants.CLIENTPORT));
		if (defaultProps.getProperty(Constants.SYNCUSESAMEPSWD).equals("true")) {
			useCurrentPswdButton.setSelected(true);
		} else {
			promptForPswdButton.setSelected(true);
		}
		logWarningButton.setSelected(true);
		enableBackupCheckBox.setSelected(
		        defaultProps.getProperty(Constants.BACKUP_ENABLED).equals("true") ? true : false);
		suppressSyncDialogCheckBox.setSelected(
            defaultProps.getProperty(Constants.SUPPRESS_SYNCDIALOG).equals("true") ? true : false);
		fieldBackupDir.setText(defaultProps.getProperty(Constants.BACKUP_DIR));
		fieldBackupCount.setText(defaultProps.getProperty(Constants.MAX_BACKUP_COUNT));
		enableBackupFields(enableBackupCheckBox.isSelected()); // enable/disable fields
		fieldKeySetupTime.setText(defaultProps.getProperty(Constants.KEY_SETUP_TIME));
		String defaultLookAndFeel = landfClassToName.get(defaultProps.getProperty(Constants.LOOK_AND_FEEL));
		fieldLookAndFeels.setSelectedIndex(0);
		for (int i = 0; i < fieldLookAndFeels.getItemCount(); i++) {
         if ((fieldLookAndFeels.getItemAt((i)).equals(defaultLookAndFeel))) {
            fieldLookAndFeels.setSelectedIndex(i);
            break;
         }
      }
		enableTimeoutCheckBox.setSelected(
		              defaultProps.getProperty(Constants.TIMEOUT_ENABLED).equals("true") ? true : false);
		saveOnTimeoutCheckBox.setSelected(
                  defaultProps.getProperty(Constants.SAVE_ON_TIMEOUT).equals("true") ? true : false);
		fieldTimeoutTime.setText(defaultProps.getProperty(Constants.TIMEOUT_TIME));
		enableTimeoutFields(enableTimeoutCheckBox.isSelected()); // enable/disable fields
	}
	
	/*
	 * Save the (modified) properties
	 */
	private void saveProperties() {
		removeDefaults();
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(propsFile);
			String version = ResourceBundle.getBundle(VERSION_BUNDLE_NAME).getString("version");
			props.store(fos, "PCSecrets version " + version);
		} catch (IOException e) {
			logger.log(Level.WARNING, "IOException saving properties file: " + e.getMessage());
			logger.log(Level.WARNING, "Defaults will be used");
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
	}
	
	/*
	 * Remove from the properties collection any values
	 * which are defaults.
	 * 
	 * We don't want to save default values - apart from being a waste
	 * of space, it means that if the program changes default values,
	 * they may have no effect because of a stored override.
	 */
	private void removeDefaults() {
	  Enumeration<?> names = props.propertyNames();
	  while(names.hasMoreElements()) {
	    String propName = (String)names.nextElement();
	    if (getProperty(propName).equals(defaultProps.getProperty(propName))) {
	      props.remove(propName);
	    }
	  }
	}

	/*
	 * Create the full path to the secrets directory. A command line override
	 * can provide a relative (PCSecrets subdir) name or full path alternative.
	 * 
	 * Default examples:
	 * - on *nix, /home/chris/.pcsecrets/
	 * - on windows:
	 *   pre-Vista: c:\documents and settings\chris\application data\pcsecrets\
	 *   Vista, 7: c:\Users\chris\appdata\pcsecrets\
	 * 
	 * On Windows, if the default has not been overridden, use a more suitable
	 * upper/lower case dir name i.e. PCSecrets
	 */
	private static String createFullPathDir() {
		StringBuffer fileName = new StringBuffer();
		if (!fullPath) { // relative path
		   if (!isWindows()) { // not windows, make dir hidden
	         fileName.append(System.getProperty("user.home") + File.separator).append(".");
	      } else {
	         fileName.append(System.getenv("APPDATA")).append(File.separator);
	         if (pcsecretsDir.equals(DEFAULT_PCS_DIR)) {
	            pcsecretsDir = DEFAULT_PCS_DIR_WIN;
	         }
	      }
		}
		fileName.append(pcsecretsDir);
		if (!fileName.toString().endsWith(File.separator)) {
		   fileName.append(File.separator);
		}
		return fileName.toString();
   }

   /*
    * Create the default backup directory. This is the backup subdir of the secrets directory
    */
   private static String createDefaultBackupDir() {
      StringBuffer dirName = new StringBuffer(createFullPathDir());
      dirName.append("backup").append(File.separator);
      return dirName.toString();
   }

   /**
    * Select backup directory
    */
   private void selectBackupDir() {
      String dirName = null;
      final JFileChooser fc = new JFileChooser(fieldBackupDir.getText());
      fc.setDialogTitle(Messages.getString("SecretsProperties.selectbackupdirtitle"));
      fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

      int rc = fc.showOpenDialog(dialog);
      if (rc == JFileChooser.APPROVE_OPTION) {
         dirName = fc.getSelectedFile().getPath() + File.separator;
         logger.log(Level.INFO, "SecretsProperties: Backup dir name is " + dirName);
         fieldBackupDir.setText(dirName);
      }
   }
  
  /**
   * Validate directory
   */
  private boolean validateDir(String dirName) {
    boolean validity = false;
    File dir = new File(dirName);
    if (dir.canWrite()) {
      validity = true;
      logger.log(Level.FINE, "SecretsProperties: Backup dir validity OK");
    } else {
      logger.log(Level.FINE, "SecretsProperties: Backup dir validity failed");
    }
    return validity;
  }
	
	/**
	 * Enable/disable backup fields
	 * @param enable
	 */
  private void enableBackupFields(boolean enable) {
     fieldBackupDirDesc.setEnabled(enable);
     fieldBackupDir.setEnabled(enable);
     selectDirButton.setEnabled(enable);
     fieldBackupCountText.setEnabled(enable);
     fieldBackupCount.setEnabled(enable);
  }
  
  /**
   * Enable/disable timeout fields
   * @param enable
   */
 private void enableTimeoutFields(boolean enable) {
    fieldTimeoutTimeText.setEnabled(enable);
    fieldTimeoutTime.setEnabled(enable);
    fieldSaveOnTimeoutText.setEnabled(enable);
    saveOnTimeoutCheckBox.setEnabled(enable);
 }
	
	/**
    * @return the logRecords
    */
   public List<LogRecord> getLogRecords() {
      return logRecords;
   }

   /**
	 * For testing only
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		SecretsProperties instance = 
		       SecretsProperties.getInitialInstance(args.length > 0 ? args[0] : null, false);
		JFrame frame = new JFrame();
		instance.showDialog(frame);
	}
}