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
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.URI;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.Timer;
import javax.swing.border.EtchedBorder;

/**
 * Main UI module
 * 
 * @author Chris Wood
 */
@SuppressWarnings("serial")
public class MainWindow extends PropertyChangeWindow implements ActionListener  {
	private static Logger logger = Logger.getLogger(MainWindow.class.getName());
	
	private StoredSecretsCollection listModel;
	private SecretsProperties props;

	private JList<HostSecret> jSecretsList;
	private InputForm inputForm;
	private JTextArea areaMsg;
	
	private SyncDeviceCollection syncDevices;
	
	private SyncDialog syncDialog;
	private DeviceSecretsCollection phoneSecrets;
	private InputPhone inputPhone;
	private OutputPhone outputPhone;
	private OutputErrorPhone outputErrorPhone;
	
	private String syncCounts;
	
	private UDPDiscovery udpDiscovery;
   private DatagramSocket socket;
   private final int discoveryPort = 53165;
   
   private Timer idleTimer;
   private static int TIMER_INTERVAL = 1000; // millisecs
   private Date timeoutDate;
   private boolean idleTimeoutHasOccurred;
	
	/**
	 * Constructor
	 * @param listModel 
	 * @param props 
	 */
	public MainWindow(StoredSecretsCollection listModel, SecretsProperties props) {
		this.listModel = listModel;
		this.props = props;
		
		setTitle(Messages.getString("PCSecrets.title"));
		List<Image> icons = new ArrayList<Image>();
      icons.add(new ImageIcon(this.getClass().getResource("/resources/pcsecrets36x36.png")).getImage());
      icons.add(new ImageIcon(this.getClass().getResource("/resources/pcsecrets48x48.png")).getImage());
      icons.add(new ImageIcon(this.getClass().getResource("/resources/pcsecrets96x96.png")).getImage());
      setIconImages(icons);
      
		createUI();
		setIdleTimer();
		
		jSecretsList.addListSelectionListener(new MainFormListener(this, inputForm));
		addWindowListener(new UnsavedChangesHandler());
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		syncDevices = listModel.getSyncDevices();
		
		try {
         socket = new DatagramSocket(discoveryPort, InetAddress.getByName("0.0.0.0"));
      } catch (Exception e) {
         JOptionPane.showMessageDialog(MainWindow.this, Messages.getString("MainWindow.alreadyrunning"),
                     Messages.getString("MainWindow.alreadyrunningtitle"), JOptionPane.ERROR_MESSAGE);
         throw new IllegalStateException("PCSecrets is already running");
      }
		
		/* initial testing - add test data using CTRL-SHIFT-T */
		Action insertTestData = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
		    HostSecret secret = new HostSecret("A secret", "ceperman", "password", "ceperman@gmail.com", "");
		    MainWindow.this.listModel.addOrUpdate(secret);
				secret = new HostSecret("Another secret", "woodc", "aabbcc", "chris@cwsoft.co.uk", "The quick brown fox jumps over the lazy dog etc etc.");
				MainWindow.this.listModel.addOrUpdate(secret);
				secret = new HostSecret("BBBB secret", "woodc", "xxyyzz", "chris@cwsoft.co.uk", "The quick brown fox jumps over the lazy dog etc etc.");
				MainWindow.this.listModel.addOrUpdate(secret);
				secret = new HostSecret("MMMM secret", "woodc", "xxyyzz", "chris@cwsoft.co.uk", "The quick brown fox jumps over the lazy dog etc etc.");
				MainWindow.this.listModel.addOrUpdate(secret);
				secret = new HostSecret("QQQQQ secret", "woodc", "xxyyzz", "chris@cwsoft.co.uk", "The quick brown \nfox jumps over the \nlazy dog etc etc. \nThe quick brown fox \njumps over the lazy dog etc etc. \nThe \nquick \nbrown \nfox \njumps \nover \nthe \nlazy \ndog \netc \netc.");
				MainWindow.this.listModel.addOrUpdate(secret);
				areaMsg.setText("Test data has been added");
		    }
		};
		getRootPane().getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK),
		                            "Insert-Test-Data");
		getRootPane().getActionMap().put("Insert-Test-Data",
				insertTestData);
	}

  /**
   * Create the main user interface window
   */
  private void createUI() {
		/* menus */
		JMenuBar jmenubar = UIUtils.createMenuBar(this);
		setJMenuBar(jmenubar);
		
		/* content */		
		JPanel jSecretsPane = new JPanel(new BorderLayout());
		jSecretsPane.setBorder(BorderFactory.createEmptyBorder(5,5,0,0));		
		JPanel jSecretsEdit = new JPanel(new BorderLayout());
		
		/* split pane to hold list and form views */
		JSplitPane jSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, jSecretsPane, jSecretsEdit);
		jSplitPane.setResizeWeight(0.5);
		getContentPane().add(jSplitPane, BorderLayout.CENTER);
		
		/* list view */
		JLabel heading = new JLabel(Messages.getString("MainWindow.secretstitle"));
		jSecretsPane.add(heading, BorderLayout.NORTH);
		jSecretsList = new JList<HostSecret>(listModel);
		JScrollPane jScrollPane = new JScrollPane(jSecretsList);
		jScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		jSecretsPane.add(jScrollPane, BorderLayout.CENTER);
		jSecretsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		/* list buttons */
		JPanel listButtons = new JPanel(new FlowLayout());
		JButton jButtonSaveAll = new JButton(Messages.getString("PCSecrets.saveall"));
		jButtonSaveAll.setToolTipText(Messages.getString("PCSecrets.savealltooltip"));
		jButtonSaveAll.setActionCommand(Constants.SAVEALL);
		jButtonSaveAll.setMnemonic(KeyEvent.VK_S);
		jButtonSaveAll.addActionListener(this);
		listButtons.add(jButtonSaveAll);
		JButton jButtonSync = new JButton(Messages.getString("PCSecrets.sync"));
		jButtonSync.setToolTipText(Messages.getString("PCSecrets.synctooltip"));
		jButtonSync.setActionCommand(Constants.SYNC);
		jButtonSync.setMnemonic(KeyEvent.VK_Y);
		jButtonSync.addActionListener(this);
		listButtons.add(jButtonSync);
		jSecretsPane.add(listButtons, BorderLayout.SOUTH);
		
		/* form view */
		inputForm = new InputForm();
		jSecretsEdit.add(inputForm, BorderLayout.CENTER);
		
		/* form buttons */
		jSecretsEdit.add(UIUtils.createInputFormButtons(this), BorderLayout.SOUTH);
		
		/* msg area */
		areaMsg = new JTextArea();
		areaMsg.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
		areaMsg.setRows(1);
		areaMsg.setEditable(false);
		getContentPane().add(areaMsg, BorderLayout.SOUTH);
		
		jScrollPane.setPreferredSize(new Dimension(300, 400));
      jSecretsEdit.setPreferredSize(new Dimension(400, 400));
  }

	/* Perform main window actions here
	 * Actions are:
	 * CLEAR - clear the input form
	 * CREATE - create new secret from data in input form
	 * UPDATE - update existing secret from data in input form
	 * DELETE - delete secret identified by input form
	 * SAVEALL - save secrets
	 * SYNC - invoke sync process
	 * EXPORT - export secrets
	 * IMPORT - import secrets
	 * PREFERENCES - invoke preferences dialog
	 * DEVICES - invoke devices dialog
	 * CHANGEPSWD - change password
	 * ABOUT - about panel
	 * EXIT - exit
	 * 
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed(ActionEvent event) {
		
		/* stuff to do if not timeout tick */
		if (!event.getActionCommand().equals(Constants.TIMEOUT_TICK)) {
		   logger.log(Level.FINE, "command is " + event.getActionCommand());
		   areaMsg.setText(""); // clear msg area
		   inputForm.setChanged(false); // ignore if form data was changed
		}
		
		/* handle individual actions */
		if (event.getActionCommand().equals(Constants.CLEAR)) {
			inputForm.clear();
			jSecretsList.clearSelection();
		} else if (event.getActionCommand().equals(Constants.CREATE)) {
			if (listModel.contains(inputForm.getFieldDescr().getText())) {
				areaMsg.setText(Messages.getString("PCSecrets.dupadd"));
			} else if (inputForm.getFieldDescr().getText().trim().length() == 0) {
					areaMsg.setText(Messages.getString("PCSecrets.missingkey"));
			} else {
				HostSecret newSecret = inputForm.getSecretFromInput();
				int index = listModel.addOrUpdate(newSecret);
				jSecretsList.setSelectedIndex(index);
				areaMsg.setText(MessageFormat.format(Messages.getString("PCSecrets.added"), inputForm.getFieldDescr()
						.getText()));
			}
		} else if (event.getActionCommand().equals(Constants.UPDATE)) {
			if (!(listModel.contains(inputForm.getFieldDescr().getText()))) {
				areaMsg.setText(Messages.getString("PCSecrets.nosecret"));
			} else {
				HostSecret newSecret = inputForm.getSecretFromInput();
				listModel.addOrUpdate(newSecret);
				inputForm.setChanged(false);
				areaMsg.setText(MessageFormat.format(Messages.getString("PCSecrets.updated"), inputForm.getFieldDescr().getText()));
			}
		} else if (event.getActionCommand().equals(Constants.DELETE)) {
			if (!(listModel.contains(inputForm.getFieldDescr().getText()))) {
				areaMsg.setText(Messages.getString("PCSecrets.nosecret"));
			} else {
			   int index = jSecretsList.getSelectedIndex();
			   String selectedDescription = inputForm.getFieldDescr().getText();
			   syncDevices.addDeleted(selectedDescription);
			   listModel.setChanged(true);
			   listModel.delete(selectedDescription);
			   int itemCount = listModel.getSize();
			   if (itemCount > 0) { // select next in list or last
			      jSecretsList.setSelectedIndex(index == itemCount /* at end */ ? index-1 : index);
			   }
			   areaMsg.setText(MessageFormat.format(Messages.getString("PCSecrets.deleted"), selectedDescription));
			}
		} else if (event.getActionCommand().equals(Constants.SAVEALL)) {
		   saveSecrets();
		   areaMsg.setText(MessageFormat.format(Messages.getString("DataHandler.saved"), listModel.getSize()));
		   listModel.setChanged(false); // clear the changed indicator after save
		} else if (event.getActionCommand().equals(Constants.SYNC)) {
	      /* start the UDP discovery thread if not already started */
		   if (udpDiscovery == null) {
		      udpDiscovery = new UDPDiscovery();
	         udpDiscovery.start();
		   }
		  /* the sync dialog is launched when device input is received */
			int port = Integer.parseInt(props.getProperty(Constants.SERVERPORT));
			byte[] syncPswd = null;
			if (props.getProperty(Constants.SYNCUSESAMEPSWD).equals("true")) {
				syncPswd = listModel.getPswdBytes();
			} else {
				syncPswd = UIUtils.getPassword(this, Messages.getString("UIUtils.syncPswdTxt"));
			}
			if (syncPswd == null) {
			  areaMsg.setText(Messages.getString("MainWindow.synccancelled"));
			} else {
				inputPhone = new InputPhone(this, this, port, syncPswd);
				inputPhone.setMetadata("keylength=" + props.getProperty(Constants.KEYLENGTH));
				inputPhone.start(); // initiate sync operation
			}
      } else if (event.getActionCommand().equals(Constants.EXPORT)) {
         FileExporter handler = new FileExporter();
         handler.export(this);
      } else if (event.getActionCommand().equals(Constants.IMPORT)) {
         FileImporter handler = new FileImporter();
         handler.handleImport(this);
      } else if (event.getActionCommand().equals(Constants.PREFERENCES)) {
         SecretsProperties.getInstance().showDialog(this);
      } else if (event.getActionCommand().equals(Constants.DEVICES)) {
         DeviceView deviceView = new DeviceView(this, syncDevices);
         deviceView.setVisible(true);
      } else if (event.getActionCommand().equals(Constants.CHANGEPSWD)) {
         UIUtils.getNewPassword(this);
      } else if (event.getActionCommand().equals(Constants.REGENCIPHERS)) {
         regenCiphers();
      } else if (event.getActionCommand().equals(Constants.ABOUT)) {
         UIUtils.aboutDialog(this);
      } else if (event.getActionCommand().equals(Constants.SYSINFO)) {
         UIUtils.sysInfoDialog(this);
      } else if (event.getActionCommand().equals(Constants.HELP)) {
         Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
         if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
            try {
               URI uri = new URI(Messages.getString("PCSecrets.helpuri"));
               desktop.browse(uri);
            } catch (Exception e) {
               logger.log(Level.WARNING, "Help uri problem: " + e);
            }
         }
      } else if (event.getActionCommand().equals(Constants.TIMEOUT_TICK)) {
         // check if timeout date has been passed
         Calendar nowCal = Calendar.getInstance();
         Calendar timeoutCal = Calendar.getInstance();
         timeoutCal.setTime(timeoutDate);
         if (nowCal.after(timeoutCal)) {
            idleTimer.stop();
            idleTimeoutHasOccurred = true;
            logger.log(Level.INFO, "PCSecrets idle timeout");
            this.dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
         }
      } else if (event.getActionCommand().equals(Constants.EXIT)) {
         this.dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
      }
		
		if (!event.getActionCommand().equals(Constants.TIMEOUT_TICK)) {
	      setIdleTimer();		   
		}
   }

	/* Completion of communication to/from the phone is handled by property change */
	@Override
	public void propertyChange(PropertyChangeEvent event) {
		logger.log(Level.INFO, "property is " + event.getPropertyName() + "=" + event.getNewValue());
		if (event.getPropertyName().equals(Constants.PHONE_RECEIVE)) { // incoming from phone
			if (event.getNewValue().equals(Constants.COMPLETE)) {
				phoneSecrets = inputPhone.getPhoneSecrets();
				/* add new sync device to list of remembered devices if currently unknown */
				String deviceId = phoneSecrets.getDeviceId();
				SyncDevice syncDevice = null;
				if (syncDevices.containsKey(deviceId)) {
				  syncDevice = syncDevices.get(deviceId);
				  logger.log(Level.FINE, "Sync device recognised: " + deviceId);
				} else { // new device
				  syncDevice = new SyncDevice(deviceId);
				  syncDevices.add(syncDevice);
				  logger.log(Level.FINE, "New sync device '" + deviceId + "' added to device collection");
				}
            /* set the last sync date into the collection */
            phoneSecrets.setLastSyncTimestamp(syncDevice.getSyncTimestamp());
            listModel.setChanged(true);
				/* transfer device IP address to details recorded on PC */
				syncDevice.setLastIP(phoneSecrets.getSourceName());
				/* create SyncDialog and show if necessary */
				syncDialog = new SyncDialog(this, listModel, phoneSecrets, syncDevice);
				syncDialog.showDialogOrCompleteExecution();
			} else if (event.getNewValue().equals(Constants.CANCEL)) {
				getAreaMsg().setText(Messages.getString("MainWindow.synccancelled"));
			} else if (event.getNewValue().equals(Constants.FAILED)) {
				if (inputPhone.isDecryptionError()) {
					getAreaMsg().setText(Messages.getString("MainWindow.syncdecryptfailure"));
					sendErrorStatusToPhone(OutputErrorPhone.DECRYPT_ERROR);
				} else if (inputPhone.isCommsError()) {
					getAreaMsg().setText(Messages.getString("MainWindow.synccommsfailure"));
				}
			}
		} else if (event.getPropertyName().equals(Constants.PHONE_SEND)) { // outgoing to phone
			/* even if the write-back to the phone fails, the values received from the phone and
			 * updated to the allSecrets collection is still valid, so use them.
			 */
         updateListModel(syncDialog.getUpdatedPCSecrets());
         if (event.getNewValue().equals(Constants.COMPLETE)) {
            listModel.setLastSyncTimestamp(System.currentTimeMillis()); // update last sync time
            getAreaMsg().setText(Messages.getString("MainWindow.syncsuccessful") + " - " + syncCounts);
            SyncDevice syncDevice = syncDevices.get(phoneSecrets.getDeviceId());
            syncDevice.setSyncTimestamp(System.currentTimeMillis());
            syncDevice.clearDeletedSecrets();
            listModel.setChanged(true);
         } else if (event.getNewValue().equals(Constants.FAILED)) {
            getAreaMsg().setText(Messages.getString("MainWindow.syncwritebackfailure"));
         }
         syncDialog.dispose();
         syncDialog = null;
		} else if (event.getPropertyName().equals(Constants.SYNC)) {
			if (event.getNewValue().equals(Constants.COMPLETE)) { // sync dialog complete
				logger.log(Level.INFO, "Sync complete signalled");
            syncCounts = syncDialog.getStats(); // need to get stats before phone updates are applied
				/* generate updates for the phone, and apply phone updates to the PC - order is important */
				DeviceSecretsCollection updatesForPhone = syncDialog.getChangedPhoneSecretsCollection();
				syncDialog.applyPhoneUpdatesToPC();
				/* Send updates to phone */
				outputPhone = new OutputPhone(this, this);
				outputPhone.start(updatesForPhone);
			} else if (event.getNewValue().equals(Constants.CANCEL)) { // sync dialog cancelled
				logger.log(Level.INFO, "Sync cancel signalled");
				sendErrorStatusToPhone(OutputErrorPhone.SYNC_CANCELLED);
				getAreaMsg().setText(Messages.getString("MainWindow.synccancelled"));
			}
		} else if (event.getPropertyName().equals(Constants.TIMEOUT_ENABLED)) {
		   setIdleTimer();
		}
	}
   
   /**
    * (re)start the inactivity detection thread, if requested
    */
   public void setIdleTimer() {
      if (props.getProperty(Constants.TIMEOUT_ENABLED).equals("true")) {
         int timeoutTime = Integer.parseInt(props.getProperty(Constants.TIMEOUT_TIME));
         timeoutTime *= 60 * 1000;
         timeoutDate = new Date(System.currentTimeMillis() + timeoutTime);
         logger.log(Level.FINE, "Inactivity timeout set");
         if (idleTimer == null) {
            idleTimer = new Timer(TIMER_INTERVAL, this);
            idleTimer.setActionCommand(Constants.TIMEOUT_TICK);
         }
         if (!idleTimer.isRunning()) {
            idleTimer.start();
            logger.log(Level.FINE, "Inactivity timeout started");
         }
      } else {
         if (idleTimer != null) {
            idleTimer.stop();
            logger.log(Level.FINE, "Inactivity timeout cancelled");
         }
      }
   }
	
	private void sendErrorStatusToPhone(byte status) {
	   outputErrorPhone = new OutputErrorPhone(this);
	   outputErrorPhone.start(status);
	}
	
	/*
	 * Regenerate the ciphers and save the secrets.
	 * This will action any change to the effective key length e.g. after
	 * installing the JCE Unlimited Strength Ploicy files. To do this, the 
	 * keylength property is reset to the max - the regen processing will lower
	 * it if necessary.
	 */
	private void regenCiphers() {
	   logger.log(Level.INFO, "regenCiphers()");
	   // reset keylength property to default
	   SecretsProperties props = SecretsProperties.getInstance();
	   props.updateProperty(Constants.KEYLENGTH, props.getDefaultProperty(Constants.KEYLENGTH));

	   getListModel().createCipherInfo();
	   
	   saveSecrets();
	   getListModel().setChanged(false);
      getAreaMsg().setText(
                  MessageFormat.format(Messages.getString("MainWindow.ciphersregened"),
                              props.getProperty(Constants.KEYLENGTH)));
	}

  /**
	 * Save the secrets. This means saving to the secrets file and to a
	 * backup file if backup support is enabled.
	 */
   public void saveSecrets() {
      listModel.save();
      if (props.getProperty(Constants.BACKUP_ENABLED).equals("true")) {
         int fileMax = Integer.parseInt(props.getProperty(Constants.MAX_BACKUP_COUNT));
         File backupDir = new File(props.getProperty(Constants.BACKUP_DIR));
         File[] files = backupDir.listFiles();
         if (files == null) {
            JOptionPane.showMessageDialog(this, Messages.getString("MainWindow.backupdirinaccessible"),
                        Messages.getString("MainWindow.backupfailedtitle"), JOptionPane.WARNING_MESSAGE);
         } else {
            while (files.length > fileMax - 1) {
               /* find oldest file and delete it */
               File oldestFile = null;
               for (File file : files) {
                  if (oldestFile == null)
                     oldestFile = file;
                  else if (file.lastModified() < oldestFile.lastModified()) {
                     oldestFile = file;
                  }
               }
               // ensure file is deleted or a loop will result
               if (oldestFile.delete()) {
                  logger.log(Level.INFO, "Backup file deleted: " + oldestFile.getName());
                  files = backupDir.listFiles();
               } else {
                  String msg = MessageFormat.format(Messages.getString("MainWindow.backupfiledeletefailed"),
                              oldestFile.getPath());
                  JOptionPane.showMessageDialog(this, msg,
                              Messages.getString("MainWindow.backupdeletefailedtitle"), JOptionPane.ERROR_MESSAGE);
                  logger.log(Level.SEVERE, msg);
                  break;
               }
            }
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
            String timeSuffix = sdf.format(System.currentTimeMillis());
            StringBuffer sb = new StringBuffer(props.getProperty(Constants.BACKUP_DIR));
            sb.append("backup-").append(timeSuffix).append(".dat");
            String backupFileName = sb.toString();
            int retval = listModel.saveAs(backupFileName);
            if (retval == 0) {
               logger.log(Level.INFO, "Backup file created: " + backupFileName);
            } else {
               String msg;
               if (retval == 1) {
                  msg = MessageFormat.format(Messages.getString("MainWindow.savecannotwrite"),
                              backupFileName);
               } else {
                  msg = MessageFormat.format(Messages.getString("MainWindow.savefailed"),
                              backupFileName);
               }
               JOptionPane.showMessageDialog(this, msg, Messages.getString("MainWindow.savefailedtitle"),
                           JOptionPane.ERROR_MESSAGE);
               logger.log(Level.SEVERE, msg);
            }            
         }
      }
   }

	/**
	 * @return the props
	 */
	public SecretsProperties getProps() {
		return props;
	}
	
	/**
	 * @return the areaMsg
	 */
	public JTextArea getAreaMsg() {
		return areaMsg;
	}

	/**
	 * @param areaMsg the areaMsg to set
	 */
	public void setAreaMsg(JTextArea areaMsg) {
		this.areaMsg = areaMsg;
	}

	/**
	 * @return the listModel
	 */
	public StoredSecretsCollection getListModel() {
		return listModel;
	}

	/**
	 * Update the list of secrets in the collection. Other attribues of the
	 * collection are unchanged.
	 * @param listModel the StoredSecretsCollection to update
	 */
	public void updateListModel(SecretsCollection listModel) {
		this.listModel.replaceSecrets(listModel);
		this.listModel.notifyChange(-1, 0);
	}
   
   /*
    * Handler to check for unsaved changes
    *
    */
   private class UnsavedChangesHandler extends WindowAdapter {
      @Override
      public void windowClosing(WindowEvent e) {
         if (MainWindow.this.listModel.isChanged()) {
            boolean saveDefault = props.getProperty(Constants.SAVE_ON_TIMEOUT).equals("true");
            int rc = saveDefault ? JOptionPane.YES_OPTION : JOptionPane.NO_OPTION;
            if (!idleTimeoutHasOccurred) {
               rc = JOptionPane.showConfirmDialog(MainWindow.this, 
                           Messages.getString("MainWindow.savechanges"), 
                           Messages.getString("MainWindow.unsavedchanges"), 
                           JOptionPane.YES_NO_CANCEL_OPTION);
            }
            if (rc == JOptionPane.YES_OPTION) {
               saveSecrets();
            } else if (rc == JOptionPane.CANCEL_OPTION) {
              return;
            } else {
              logger.log(Level.INFO, "Secrets not saved");
            }
         }
         try {
            MainWindow.this.socket.close();
         } catch (Exception e1) {
            // ignore
         }
         if (idleTimer != null) {
            idleTimer.stop();
         }
         logger.log(Level.INFO, "PCSecrets is terminating");
         MainWindow.this.dispose();
      }
      
   }
   
   /*
    * Thread to support client discovery of the server
    */
   private class UDPDiscovery extends Thread {
      @Override
      public void run() {
         try {
            socket.setBroadcast(true);
            while (true) {
               // Receive a packet
               byte[] recvBuf = new byte[256];
               DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
               logger.log(Level.FINE, "UDPDiscovery waiting for packet");
               socket.receive(packet); // blocks here

               // Packet received
               String message = new String(packet.getData()).trim();
               logger.log(Level.FINE, "UDPDiscovery discovery packet received from: "
                           + packet.getAddress().getHostAddress());
               logger.log(Level.FINE, "UDPDiscovery packet received; data: " + message);

               // See if the packet holds the right command (message)
               if (message.equals("DISCOVER_PCSECRETS_REQUEST")) {
                  byte[] sendData = "DISCOVER_PCSECRETS_RESPONSE".getBytes();

                  // Send a response
                  DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, packet.getAddress(),
                              packet.getPort());
                  socket.send(sendPacket);

                  logger.log(Level.FINE, "UDPDiscovery sent packet to: "
                              + sendPacket.getAddress().getHostAddress());
               }
            }
         } catch (IOException e) {
            if (e.getMessage().contains("closed")) {
               logger.log(Level.FINE, "UDP socket closed");
            } else {
               logger.log(Level.FINE, "UDP socket problem: " + e.getMessage());
            }
         }
      }
   }
	
}