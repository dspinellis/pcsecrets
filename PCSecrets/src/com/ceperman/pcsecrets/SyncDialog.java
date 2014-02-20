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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * Synchronises changes between the PC secrets and device secrets.
 * 
 * * Overview of processing
 * 
 * The secrets collection from the device contains its all secrets, including
 * deleted ones (the deleted flag is set).
 * 
 * The pc secrets collection does not contain deletions. These are held in a
 * SyncDevice object for the specific device. These are added back in to (a copy
 * of) the pc collection before conflict resolution, so info about deletions can
 * be displayed.
 * 
 * A conflict exists if a secret has been changed (updated or deleted) on both
 * pc and device since the last sync. Conflict resolution will result in a set
 * of updated secrets to be sent to the device; specifically (1) a secret that
 * has been updated on both pc and device will be resolved by the user into an
 * updated pc secret. The updated secret is sent to the device. (2) a secret
 * that has been updated on the pc and deleted from the device will be either
 * deleted from the pc (nothing is sent to the device) or restored to the device
 * using the pc values. (3) a secret that has been deleted from the pc and
 * updated on the device will be either restored to the pc or deleted from the
 * device. A deleted secret (the deleted flag is set) is sent to the device in
 * the latter case.
 * 
 * As a result of the resolution, the secrets collection should contain (to be
 * completed...)
 * 
 * Processing steps: 
 * - produce combined collection of pc and device secrets, including deletions 
 * - remove any secrets that have been deleted on both sides; these cause no
 *   changes and can be silently discarded (and are thus not visible) 
 * - resolve conflicts using dialog - extract secrets (PC_VALUE and
 *   DELETED_ON_PC) to be sent to device 
 * - remove secrets marked as deletedOnPC -
 * apply updates from device to pc secrets (PHONE_VALUE and DELETED_ON_DEVICE)
 * 
 * Note for deletions: pc deletions only exist in allSecrets, not in pcSecrets.
 * For device deletions, these exist in allSecrets and deviceSecrets.
 * 
 * @author Chris Wood
 */
@SuppressWarnings({"serial", "rawtypes", "unchecked"})
public class SyncDialog extends JDialog implements ActionListener, ItemListener, ListSelectionListener {
   private static Logger logger = Logger.getLogger(SyncDialog.class.getName());

   final static String CMD_CONFIRM_DELETE = "confirmDelete";
   final static String CMD_RELOAD_FROM_DEVICE = "reloadFromDevice";
   final static String CMD_RELOAD_FROM_PC = "reloadFromPC";

   private DatedSecretsCollection pcSecretsOriginal; // original collection

   private DatedSecretsCollection pcSecrets;
   private DeviceSecretsCollection deviceSecrets;

   private ListSecrets allSecrets;
   private ListSecrets filteredSecrets;
   private ListSecrets conflicts;

   private JList secretsList;
   private InputForm pcForm;
   private InputForm phoneForm;
   private JTextArea areaMsg;

   private JLabel deviceId = new JLabel();
   private JLabel ipAddress = new JLabel();
   private JLabel lastSyncDate = new JLabel();
   private JCheckBox showLatestOnPC = new JCheckBox();
   private JCheckBox showLatestOnPhone = new JCheckBox();
   private JCheckBox showDeletedOnPC = new JCheckBox();
   private JCheckBox showDeletedOnDevice = new JCheckBox();
   private JCheckBox showUnchanged = new JCheckBox();
   private JCheckBox showConflicts = new JCheckBox();

   private JButton buttonMarkAsMerged;
   private JButton buttonCopyToPC;
   private JButton buttonFinish;

   private boolean isCancelled;

   /* secret comparison values */
   private static final int PC_VALUE = 0;
   private static final int PHONE_VALUE = 1;
   private static final int EQUALS_VALUE = 2;
   private static final int CONFLICT_VALUE = 3;
   private static final int DELETED_ON_DEVICE = 4;
   private static final int DELETED_ON_PC = 5;

   /* Colours for items in secrets list */
   /* http://www.december.com/html/spec/colorshades.html */
   private static final Color pcColour = Color.decode("0xC5E9FE"); // blue mist  202
   private static final Color pcColourHi = Color.decode("0x82CFFD");
   private static final Color phoneColour = Color.decode("0xD9FFD9"); // darkseagreen1 120
   private static final Color phoneColourHi = Color.decode("0x9AFF9A");
   private static final Color conflictColour = Color.decode("0xFF9999"); // red 0
   private static final Color conflictColourHi = Color.decode("0xFF4D4D");
   private static final Color pcDeleteColour = Color.decode("0xF7BE8D"); // sandybrown 28
   private static final Color pcDeleteColourHi = Color.decode("0xF4A460");
   private static final Color phoneDeleteColour = Color.decode("0xDFEB6E"); // pear 66
   private static final Color phoneDeleteColourHi = Color.decode("0xD1E231");
   private static final Color equalsColour = Color.decode("0xFFFFFF"); // white - dialog default (by inspection)
   private static final Color equalsColourHi = Color.decode("0xC0C0C0"); // grey

   private static final Color[] NORMAL_COLOURS = { pcColour, phoneColour, equalsColour, conflictColour,
         phoneDeleteColour, pcDeleteColour };
   private static final Color[] HIGHLIGHT_COLOURS = { pcColourHi, phoneColourHi, equalsColourHi, conflictColourHi,
         phoneDeleteColourHi, pcDeleteColourHi };

   /**
    * Constructor
    * 
    * @param parent
    * @param hostSecrets
    *           pc secrets
    * @param inputSecrets
    *           device secrets collection
    * @param syncDevice
    *           deletion data for PCSecrets
    */
   public SyncDialog(PropertyChangeWindow parent, DatedSecretsCollection hostSecrets,
               DeviceSecretsCollection inputSecrets, SyncDevice syncDevice) {
      logger.log(Level.FINE, "SyncDialog()");
      deviceSecrets = inputSecrets;
      pcSecretsOriginal = hostSecrets;

      /*
       * get deep copy of pc secrets - so that if dialog is cancelled all
       * changes are discarded. The device deletions are not copied as these
       * will not be modified
       */
      pcSecrets = new DatedSecretsCollection(pcSecretsOriginal); // this is a
                                                                 // deep copy

      /* create the dialog window contents */
      setTitle(Messages.getString("SyncDialog.title"));
      setModalityType(ModalityType.APPLICATION_MODAL);
      createUI(parent);
      deviceId.setText(syncDevice.getDisplayName());
      ipAddress.setText(syncDevice.getLastIP());

      /*
       * Get the last recorded sync timestamp
       */
      lastSyncDate.setText(SecretsProperties.sdf.format(inputSecrets.getLastSyncTimestamp()));

      /*
       * create a combined list of pc and device secrets - this is a master list
       * of all secrets, information specific to each side (device or PC) is not
       * held here.
       */
      allSecrets = generateListModel(hostSecrets, inputSecrets, syncDevice);

      /* determine the number of changes, conflicts */
      conflicts = determineConflicts();
      int[] counts = getCounts();
      int total = 0;
      for (int i = 0; i < counts.length; i++) {
         if (i != EQUALS_VALUE) total += counts[i];
      }
      if (conflicts.getSize() > 0) {
         areaMsg.setText(MessageFormat.format(Messages.getString("SyncDialog.conflictcount"), conflicts.getSize()));
      } else {
         buttonFinish.setEnabled(true);
         if (total == 0) {
            areaMsg.setText("There are no differences");
         } else if (conflicts.getSize() == 0) {
            areaMsg.setText("There are no conflicts");
         } else {
            areaMsg.setText(getStats());
         }
      }

      /**
       * Set the list model, cell renderer and add listeners for both forms. The
       * cell renderer colours the list item according to the source of the
       * latest version, equality or conflict. The listeners will complete the
       * forms for the selected item from the corresponding secrets collection,
       * and handle enable/disable of buttons.
       */
      secretsList.setModel(allSecrets);
      itemStateChanged(null); // filter list by initial checkbox settings
      secretsList.setCellRenderer(new ColourCellRenderer());
      secretsList.addListSelectionListener(new PCFormListener(pcSecrets, pcForm, buttonMarkAsMerged));
      secretsList.addListSelectionListener(new DeviceFormListener(deviceSecrets, phoneForm, buttonCopyToPC));
      secretsList.addListSelectionListener(new ButtonFormListener());
      secretsList.addListSelectionListener(this); // conflict info listener
      addPropertyChangeListener(parent);
      logger.log(Level.FINE, "SyncDialog() complete");
   }

   /**
    * Generate the list model collection by combining the secrets from the pc
    * collection, the device collection and the pc deletions. Secrets that have
    * been deleted on both, or deleted on one and don't exist on the other, are
    * not added.
    */
   private ListSecrets generateListModel(SecretsCollection pcSecrets, SecretsCollection deviceSecrets,
               SyncDevice pcDeletions) {
      ListSecrets listSecrets = new ListSecrets();
      ListSecret listSecret;
      for (HostSecret hostSecret : pcSecrets) {
         listSecrets.insert(new ListSecret(hostSecret.getDescription(), hostSecret.getTimestamp()));
      }
      for (HostSecret hostSecret : deviceSecrets) {
         listSecret = listSecrets.find(hostSecret.getDescription());
         if (listSecret == null && !hostSecret.isDeleted()) {
            listSecret = new ListSecret(hostSecret.getDescription(), hostSecret.getTimestamp());
            listSecrets.insert(listSecret);
         } 
         if (listSecret != null && hostSecret.isDeleted()) {
            listSecret.setDeletedOnDevice(true);
         }
      }
      /* At this point, the list contains secrets that exist in an undeleted state
       * in at least one place.
       * Now look at pc deletions...
       */
      for (SyncDevice.DeletedSecret deletedSecret : pcDeletions.getDeletedSecrets()) {
         listSecret = listSecrets.find(deletedSecret.getDescription());
         /* if the secrets has also been deleted on the device there is no
          * conflict - remove the secret
          */
         if (listSecret != null) {
            if (listSecret.isDeletedOnDevice()) {
               listSecrets.delete(deletedSecret.getDescription());
            } else {
               listSecret.setDeletedOnPC(true);
            }
         }
      }
      return listSecrets;
   }

   /**
    * Conditionally show the sync dialog
    * 
    * Don't show the dialog if the preference to suppress it is set and there
    * are no conflicts.
    */
   public void showDialogOrCompleteExecution() {
      SecretsProperties props = SecretsProperties.getInstance();
      if (props.getProperty(Constants.SUPPRESS_SYNCDIALOG).equals("true") && conflicts.getSize() == 0) {
         logger.log(Level.INFO, "Supressing sync dialog");
         firePropertyChange(Constants.SYNC, null, Constants.COMPLETE);
      } else {
         logger.log(Level.INFO, "Showing sync dialog");
         setVisible(true);
      }
   }

   /**
    * Handle actions
    */
   @Override
   public void actionPerformed(ActionEvent event) {
      logger.log(Level.INFO, "command is " + event.getActionCommand());
      if (event.getActionCommand().equals(Constants.CANCEL)) {
         isCancelled = true;
         dispatchEvent(new WindowEvent(SyncDialog.this, WindowEvent.WINDOW_CLOSING));
      } else if (event.getActionCommand().equals(Constants.MERGED)) {
         /* A conflict has been resolved, and the data in the PC form should be
          * applied to the PC secret, which is now the "correct" version.
          * The secret is removed from the conflicts collection. The secret
          * will be copied to the device.
          */
         int selected = secretsList.getSelectedIndex();
         if (!(selected < 0)) { /* -1 indicates no selection */
            ListSecret selectedListSecret = (ListSecret) secretsList.getSelectedValue();
            HostSecret selectedPCSecret = pcSecrets.get(selectedListSecret.getDescription());
            pcForm.updateSecretFromInput(selectedPCSecret);
            selectedPCSecret.setTimestamp(System.currentTimeMillis());
            selectedListSecret.setMerged(true);
            removeConflict(selected, selectedListSecret.getDescription());
            buttonMarkAsMerged.setEnabled(false);
            buttonCopyToPC.setEnabled(false);
         }
      } else if (event.getActionCommand().equals(Constants.COPYTOPC)) {
         /* copy the data from the device form to the pc form */
         pcForm.setInputFromSecret(phoneForm.getSecretFromInput());
      } else if (event.getActionCommand().equals(CMD_CONFIRM_DELETE)) {
         /* accept the deletion - remove from the jlist and conflict list.
          * The secret will be deleted when the deletion list is eventually
          * processed.
          */
         int selected = secretsList.getSelectedIndex();
         if (!(selected < 0)) { /* -1 indicates no selection */
            ListSecret selectedListSecret = (ListSecret) secretsList.getSelectedValue();
            selectedListSecret.setMerged(true);
            removeConflict(selected, selectedListSecret.getDescription());
         }
      } else if (event.getActionCommand().equals(CMD_RELOAD_FROM_PC)) {
         /* device delete rejected - remove from the conflict list and from
          * the device list so it gets refreshed from the PC version.
          */
         int selected = secretsList.getSelectedIndex();
         if (!(selected < 0)) { /* -1 indicates no selection */
            ListSecret selectedListSecret = (ListSecret) secretsList.getSelectedValue();
            selectedListSecret.setDeletedOnDevice(false);
            deviceSecrets.delete(selectedListSecret.getDescription()); // remove the device secret so
                                                                       // it will be updated
            selectedListSecret.setMerged(true);
            removeConflict(selected, selectedListSecret.getDescription());
         }
      } else if (event.getActionCommand().equals(CMD_RELOAD_FROM_DEVICE)) {
         /* PC delete rejected - refresh from the device. This differs from the
          * device case as the secret is not in the PC collection as it was deleted. 
          * Just setting it as not deleted will cause it to be refreshed from the
          * device. 
          */
         int selected = secretsList.getSelectedIndex();
         if (!(selected < 0)) { /* -1 indicates no selection */
            ListSecret selectedListSecret = (ListSecret) secretsList.getSelectedValue();
            selectedListSecret.setDeletedOnPC(false);
            selectedListSecret.setMerged(true);
            removeConflict(selected, selectedListSecret.getDescription());
         }
      } else if (event.getActionCommand().equals(Constants.FINISH)) {
         firePropertyChange(Constants.SYNC, null, Constants.COMPLETE);
      }
   }

   /**
    * Remove a conflict, if count is zero then we're OK to go
    * 
    * @param selected
    * @param key
    */
   private void removeConflict(int selected, String key) {
      pcSecrets.setChanged(true);
      conflicts.delete(key);
      if (conflicts.getSize() == 0) {
         buttonFinish.setEnabled(true);
         areaMsg.setText(getStats());
      }
      areaMsg.setForeground(Color.black);
      areaMsg.setText(MessageFormat.format(Messages.getString("SyncDialog.conflictcount"), conflicts.getSize()));
      ((ListSecrets) secretsList.getModel()).notifyRemoval(selected);
   }

   /**
    * Get updated collection of pc secrets
    * 
    * @return pc secrets
    */
   public SecretsCollection getUpdatedPCSecrets() {
      return pcSecrets;
   }

   /**
    * Create collection of secrets where the PC value is the current one (and
    * not equal or conflict), where the PC secret has been deleted.
    * 
    * @return stored collection of updated secrets
    */
   public DeviceSecretsCollection getChangedPhoneSecretsCollection() {
      DeviceSecretsCollection secretsCollection = new DeviceSecretsCollection(deviceSecrets);
      secretsCollection.clear();
      /* copy in all secrets where latest version is on PC */
      for (int i = 0; i < allSecrets.getSize(); i++) {
         ListSecret secret = (ListSecret) allSecrets.getElementAt(i);
         int category = categoriseSecret(secret, pcSecrets, deviceSecrets);
         if (category == PC_VALUE) {
            secretsCollection.addOrUpdate(pcSecrets.get(secret.getDescription()));
         } else if (category == DELETED_ON_PC) {
            HostSecret deletedSecret = new HostSecret(secret.getDescription());
            deletedSecret.setDeleted(true);
            secretsCollection.addOrUpdate(deletedSecret);
         }
      }
      return secretsCollection;
   }

   /**
    * Apply any updates from the phone to the pc secrets - update pc secret
    * where last updated on the device, remove pc secret where deleted on the
    * device
    */
   public void applyPhoneUpdatesToPC() {
      for (Iterator<ListSecret> iterator = allSecrets.iterator(); iterator.hasNext();) {
         ListSecret secret = iterator.next();
         int category = categoriseSecret(secret, pcSecrets, deviceSecrets);
         if (category == PHONE_VALUE) {
            pcSecrets.addOrUpdate(deviceSecrets.get(secret.getDescription()));
            pcSecrets.setLastSyncTimestamp(System.currentTimeMillis());
            pcSecrets.setChanged(true);
         } else if (category == DELETED_ON_DEVICE) {
            pcSecrets.delete(secret.getDescription());
         }
      }
   }

   /**
    * Count the categorisations
    * 
    * @return array of counts, index by comparison value
    */
   public int[] getCounts() {
      int[] counts = new int[6];
      for (int i = 0; i < allSecrets.getSize(); i++) {
         ListSecret secret = (ListSecret) allSecrets.getElementAt(i);
         int cat = categoriseSecret(secret, pcSecrets, deviceSecrets);
         counts[cat]++;
      }
      return counts;
   }

   /**
    * Create stats string to put in msg area
    * 
    * @return stats info
    */
   public String getStats() {
      int[] counts = getCounts();
      return MessageFormat.format(Messages.getString("SyncDialog.counts"), counts[0], counts[1], counts[2], counts[4],
                  counts[5]);
   }

   /**
    * Create a collection of conflicts.
    * 
    * @return collection of conflicts
    */
   private ListSecrets determineConflicts() {
      ListSecrets conflicts = new ListSecrets();
      for (int i = 0; i < allSecrets.getSize(); i++) {
         ListSecret secret = (ListSecret) allSecrets.getElementAt(i);
         if (categoriseSecret(secret, pcSecrets, deviceSecrets) == CONFLICT_VALUE) {
            conflicts.insert(secret);
         }
      }
      return conflicts;
   }

   /**
    * Construct the dialog window.
    * 
    * @param parent
    */
   private void createUI(JFrame parent) {
      /* left and right content for the top-level split pane */
      JPanel listPane = new JPanel(new BorderLayout());
      JPanel formPane = new JPanel(new BorderLayout());

      /* top-level split pane */
      JSplitPane contentSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listPane, formPane);
      contentSplitPane.setResizeWeight(0.2); // most extra space to the form
      getContentPane().add(contentSplitPane, BorderLayout.CENTER);

      /* list view goes in top-level split pane: left */
      secretsList = new JList(pcSecrets);
      JScrollPane jScrollPane = new JScrollPane(secretsList);
      jScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      listPane.add(jScrollPane, BorderLayout.CENTER);
      secretsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

      /* list view lower area for info, checkbox and button(s) */
      JPanel listInfoPane = new JPanel(new BorderLayout());
      listPane.add(listInfoPane, BorderLayout.SOUTH);

      /* list view info */
      JPanel listItemsPane = new JPanel();
      listItemsPane.setLayout(new BoxLayout(listItemsPane, BoxLayout.Y_AXIS));
      listInfoPane.add(listItemsPane, BorderLayout.NORTH);

      JPanel deviceInfoPane = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2)); // hgap=5,vgap=2
      listItemsPane.add(deviceInfoPane);
      JLabel labelDeviceIdHdr = new JLabel(Messages.getString("SyncDialog.deviceid"), JLabel.RIGHT);
      labelDeviceIdHdr.setFont(new Font("SanSerif", Font.ITALIC, 12));
      deviceInfoPane.add(labelDeviceIdHdr);
      deviceId = new JLabel(Messages.getString("SyncDialog.unknownid"), JLabel.LEFT);
      deviceId.setFont(new Font("SanSerif", Font.PLAIN, 12));
      deviceInfoPane.add(deviceId);

      JPanel ipAddressInfoPane = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2)); // hgap=5,vgap=2
      listItemsPane.add(ipAddressInfoPane);
      JLabel labelIpAddressHdr = new JLabel(Messages.getString("SyncDialog.ipaddress"), JLabel.RIGHT);
      labelIpAddressHdr.setFont(new Font("SanSerif", Font.ITALIC, 12));
      ipAddressInfoPane.add(labelIpAddressHdr);
      ipAddress = new JLabel(Messages.getString("SyncDialog.unknownid"), JLabel.LEFT);
      ipAddress.setFont(new Font("SanSerif", Font.PLAIN, 12));
      ipAddressInfoPane.add(ipAddress);

      JPanel dateInfoPane = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
      listItemsPane.add(dateInfoPane);
      JLabel labelSyncDateHdr = new JLabel(Messages.getString("SyncDialog.lastsyncdate"), JLabel.RIGHT);
      labelSyncDateHdr.setFont(new Font("SanSerif", Font.ITALIC, 12));
      dateInfoPane.add(labelSyncDateHdr);
      lastSyncDate = new JLabel(Messages.getString("SyncDialog.neversynced"), JLabel.LEFT);
      lastSyncDate.setFont(new Font("SanSerif", Font.PLAIN, 12));
      dateInfoPane.add(lastSyncDate);

      /* list view checkboxes */
      /* conflicts checkbox is disabled, so these are always shown */
      JPanel listInfoCheckboxPane = new JPanel();
      listInfoCheckboxPane.setLayout(new BoxLayout(listInfoCheckboxPane, BoxLayout.Y_AXIS));
      listInfoPane.add(listInfoCheckboxPane, BorderLayout.CENTER);

      JPanel checkboxPane = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)); // hgap,vgap=0
                                                                               // for
                                                                               // compact
                                                                               // layout
      checkboxPane.add(showLatestOnPC);
      showLatestOnPC.addItemListener(this);
      showLatestOnPC.setSelected(true);
      JLabel checkboxLabel = new JLabel(Messages.getString("SyncDialog.showlatestonpc"));
      checkboxLabel.setBackground(pcColour);
      checkboxLabel.setOpaque(true);
      checkboxPane.add(checkboxLabel);
      listInfoCheckboxPane.add(checkboxPane);

      checkboxPane = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
      checkboxPane.add(showLatestOnPhone);
      showLatestOnPhone.addItemListener(this);
      showLatestOnPhone.setSelected(true);
      checkboxLabel = new JLabel(Messages.getString("SyncDialog.showlatestonphone"));
      checkboxLabel.setBackground(phoneColour);
      checkboxLabel.setOpaque(true);
      checkboxPane.add(checkboxLabel);
      listInfoCheckboxPane.add(checkboxPane);

      checkboxPane = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
      checkboxPane.add(showDeletedOnPC);
      showDeletedOnPC.addItemListener(this);
      showDeletedOnPC.setSelected(true);
      checkboxLabel = new JLabel(Messages.getString("SyncDialog.showdeletedonpc"));
      checkboxLabel.setBackground(pcDeleteColour);
      checkboxLabel.setOpaque(true);
      checkboxPane.add(checkboxLabel);
      listInfoCheckboxPane.add(checkboxPane);

      checkboxPane = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
      checkboxPane.add(showDeletedOnDevice);
      showDeletedOnDevice.addItemListener(this);
      showDeletedOnDevice.setSelected(true);
      checkboxLabel = new JLabel(Messages.getString("SyncDialog.showdeletedondevice"));
      checkboxLabel.setBackground(phoneDeleteColour);
      checkboxLabel.setOpaque(true);
      checkboxPane.add(checkboxLabel);
      listInfoCheckboxPane.add(checkboxPane);

      checkboxPane = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
      checkboxPane.add(showUnchanged);
      showUnchanged.addItemListener(this);
      showUnchanged.setSelected(false);
      checkboxLabel = new JLabel(Messages.getString("SyncDialog.showunchanged"));
      checkboxLabel.setBackground(equalsColour);
      checkboxLabel.setOpaque(true);
      checkboxPane.add(checkboxLabel);
      listInfoCheckboxPane.add(checkboxPane);

      checkboxPane = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
      checkboxPane.add(showConflicts);
      showConflicts.addItemListener(this);
      showConflicts.setSelected(true);
      showConflicts.setEnabled(false);
      checkboxLabel = new JLabel(Messages.getString("SyncDialog.showconflicts"));
      checkboxLabel.setBackground(conflictColour);
      checkboxLabel.setOpaque(true);
      checkboxPane.add(checkboxLabel);
      listInfoCheckboxPane.add(checkboxPane);

      /* list view buttons */
      JPanel listButtons = new JPanel(new FlowLayout());
      listInfoPane.add(listButtons, BorderLayout.SOUTH);
      buttonFinish = new JButton(Messages.getString("SyncDialog.finish"));
      buttonFinish.setToolTipText(Messages.getString("SyncDialog.finishtooltip"));
      buttonFinish.setActionCommand(Constants.FINISH);
      buttonFinish.setMnemonic(KeyEvent.VK_F);
      buttonFinish.setEnabled(false); // initially disabled
      buttonFinish.addActionListener(this);
      listButtons.add(buttonFinish);
      JButton jButtonCancel = new JButton(Messages.getString("SyncDialog.cancel"));
      jButtonCancel.setToolTipText(Messages.getString("SyncDialog.canceltooltip"));
      jButtonCancel.setActionCommand(Constants.CANCEL);
      jButtonCancel.setMnemonic(KeyEvent.VK_X);
      jButtonCancel.addActionListener(this);
      listButtons.add(jButtonCancel);

      /* form view goes in top-level split pane: right */
      /*
       * form view itself is a split pane for pc form (left) and phone form
       * (right)
       */
      JPanel pcFormPane = new JPanel(new BorderLayout());
      JPanel phoneFormPane = new JPanel(new BorderLayout());
      pcFormPane.setBackground(pcColour);
      phoneFormPane.setBackground(phoneColour);
      JSplitPane formSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, pcFormPane, phoneFormPane);
      formSplitPane.setResizeWeight(0.5);
      formPane.add(formSplitPane, BorderLayout.CENTER);

      /* pc form goes in left split */
      pcForm = new InputForm();
      pcFormPane.add(pcForm, BorderLayout.CENTER);
      pcFormPane.add(new JLabel(Messages.getString("SyncDialog.pcformhdr"), JLabel.CENTER), BorderLayout.NORTH);

      /* phone form goes in right split */
      phoneForm = new InputForm();
      phoneFormPane.add(phoneForm, BorderLayout.CENTER);
      phoneFormPane.add(new JLabel(Messages.getString("SyncDialog.phoneformhdr"), JLabel.CENTER), BorderLayout.NORTH);
      phoneForm.setReadOnly();

      /* pcForm info and buttons */
      JPanel pcFormButtonArea = new JPanel(new BorderLayout());
      pcFormPane.add(pcFormButtonArea, BorderLayout.SOUTH);
      JPanel pcFormButtons = new JPanel(new FlowLayout());
      pcFormButtonArea.add(pcFormButtons, BorderLayout.SOUTH);
      buttonMarkAsMerged = new JButton(Messages.getString("SyncDialog.markasmerged"));
      buttonMarkAsMerged.setToolTipText(Messages.getString("SyncDialog.markasmergedtooltip"));
      buttonMarkAsMerged.setActionCommand(Constants.MERGED);
      buttonMarkAsMerged.setMnemonic(KeyEvent.VK_M);
      buttonMarkAsMerged.setEnabled(false); // initially disabled
      buttonMarkAsMerged.addActionListener(this);
      pcFormButtons.add(buttonMarkAsMerged);

      /* phoneForm info and buttons */
      JPanel phoneFormButtonArea = new JPanel(new BorderLayout());
      phoneFormPane.add(phoneFormButtonArea, BorderLayout.SOUTH);
      JPanel phoneFormButtons = new JPanel(new FlowLayout());
      phoneFormButtonArea.add(phoneFormButtons, BorderLayout.SOUTH);
      buttonCopyToPC = new JButton(Messages.getString("SyncDialog.copytopc"));
      buttonCopyToPC.setToolTipText(Messages.getString("SyncDialog.copytopctooltip"));
      buttonCopyToPC.setActionCommand(Constants.COPYTOPC);
      buttonCopyToPC.setMnemonic(KeyEvent.VK_P);
      buttonCopyToPC.setEnabled(false); // initially disabled
      buttonCopyToPC.addActionListener(this);
      phoneFormButtons.add(buttonCopyToPC);

      /* msg area at bottom */
      areaMsg = new JTextArea();
      areaMsg.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
      areaMsg.setRows(1);
      areaMsg.setEditable(false);
      getContentPane().add(areaMsg, BorderLayout.SOUTH);

      addWindowListener(new UnsavedChangesHandler());
      setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
      setPreferredSize(new Dimension(900, 600));
      listPane.setPreferredSize(listPane.getMinimumSize()); // force list pane
                                                            // to minimum size
      pack();
      setLocationRelativeTo(parent);
   }

   /**
    * Determine the status of the secret - where the latest version is, or if
    * they are equal or conflict.
    * 
    * If the secret exists in only one place, that's the one to use. If the
    * secret is the same in both places then leave alone regardless of their
    * timestamps (because of this, the timestamps could be different) If both
    * secrets have been changed since the last sync, then there is a conflict*
    * If only one secret has been changed since the last sync, then that secret
    * is the one to use - there is no conflict.
    * 
    * (*=unless the pc secret has been updated during the sync/merge process, in
    * which case the conflict has been resolved)
    * 
    * It is not clear if the case where the last sync timestamp is *later* than
    * changes to a secret on both systems when they are not the same is actually
    * possible or valid, but if it occurs it will be classed as a conflict as a
    * safety measure.
    * 
    * @param secret
    * @param pcSecrets
    * @param phoneSecrets
    * @return int: 0=pc, 1=phone, 2=equal 3=conflict 4=deleted on device
    *         5=deleted on PC
    */
   public static int categoriseSecret(ListSecret secret, DatedSecretsCollection pcSecrets,
               DatedSecretsCollection phoneSecrets) {
      int valueToUse;
      final long lastSyncDate = phoneSecrets.getLastSyncTimestamp(); // provided from pc's SyncDevice
      String s = secret.getDescription();
      HostSecret pcSecret = pcSecrets.get(s);
      HostSecret phoneSecret = phoneSecrets.get(s);
      if (secret.isDeletedOnPC()) {
         if (secret.getDeletedTimestamp() > lastSyncDate 
                     && phoneSecret != null 
                     && phoneSecret.getTimestamp() > lastSyncDate
                     && !(secret.isMerged())) {
            valueToUse = CONFLICT_VALUE;
         } else {
            valueToUse = DELETED_ON_PC;
         }
      } else if (secret.isDeletedOnDevice()) {
         if (secret.getDeletedTimestamp() > lastSyncDate 
                     && pcSecret != null
                     && pcSecret.getTimestamp() > lastSyncDate
                     && !(secret.isMerged())) {
            valueToUse = CONFLICT_VALUE;
         } else {
            valueToUse = DELETED_ON_DEVICE;
         }
      } else if (pcSecret == null) {
         valueToUse = PHONE_VALUE;
      } else if (phoneSecret == null) {
         valueToUse = PC_VALUE;
      } else if (pcSecret.equals(phoneSecret)) {
         valueToUse = EQUALS_VALUE;
      } else if ((pcSecret.getTimestamp() > lastSyncDate && phoneSecret.getTimestamp() > lastSyncDate && !(secret
                  .isMerged()))
                  || (pcSecret.getTimestamp() < lastSyncDate && phoneSecret.getTimestamp() < lastSyncDate)) {
         valueToUse = CONFLICT_VALUE;
      } else if ((phoneSecret.getTimestamp() > pcSecret.getTimestamp() && phoneSecret.getTimestamp() > lastSyncDate)) {
         valueToUse = PHONE_VALUE;
      } else {
         valueToUse = PC_VALUE;
      }
      return valueToUse;
   }

   /**
    * List selection listener to provide information in the message area about
    * the selected secret
    * 
    * @param event
    */
   @Override
   public void valueChanged(ListSelectionEvent event) {
      if (event.getValueIsAdjusting() == false) {
         int index = ((JList) event.getSource()).getSelectedIndex();
         ListSecrets listModel = (ListSecrets) ((JList) event.getSource()).getModel();
         if (!(index < 0)) { // -1 indicates no selection
            ListSecret selectedSecret = (ListSecret) listModel.getElementAt(index);
            String s = selectedSecret.getDescription();
            if (conflicts.contains(s)) {
               areaMsg.setForeground(Color.red);
               if (selectedSecret.isDeletedOnPC()) {
                  areaMsg.setText(Messages.getString("SyncDialog.conflict1"));
               } else if (selectedSecret.isDeletedOnDevice()) {
                  areaMsg.setText(Messages.getString("SyncDialog.conflict2"));
               } else {
                  areaMsg.setText(Messages.getString("SyncDialog.conflict3"));
               }
            } else {
               areaMsg.setForeground(Color.black);
               int cat = categoriseSecret(selectedSecret, pcSecrets, deviceSecrets);
               if (cat == PC_VALUE) {
                  areaMsg.setText(Messages.getString("SyncDialog.infopcvalue"));
               } else if (cat == PHONE_VALUE) {
                  areaMsg.setText(Messages.getString("SyncDialog.infodevvalue"));
               } else if (cat == DELETED_ON_PC) {
                  areaMsg.setText(Messages.getString("SyncDialog.infopcdel"));
               } else if (cat == DELETED_ON_DEVICE) {
                  areaMsg.setText(Messages.getString("SyncDialog.infodevdel"));
               } else {
                  areaMsg.setText(Messages.getString("SyncDialog.nochange"));
               }
            }
         }
      }
   }

   /**
    * Checkbox listener
    * 
    * Modify JList model dependent on checkbox state: if all options are
    * checked, set the full list as the model, otherwise create an appropriate
    * filtered colection and use this. Conflicts are always shown.
    * 
    * @see java.awt.event.ItemListener#itemStateChanged(java.awt.event.ItemEvent)
    */
   @Override
   public void itemStateChanged(ItemEvent event) {
      if (allSecrets == null) return; // not created yet
      if (showLatestOnPC.isSelected() && showLatestOnPhone.isSelected() && showUnchanged.isSelected()
                  && showDeletedOnDevice.isSelected() && showDeletedOnPC.isSelected()) {
         secretsList.setModel(allSecrets);
      } else {
         filteredSecrets = new ListSecrets();
         for (int i = 0; i < allSecrets.getSize(); i++) {
            ListSecret secret = (ListSecret) allSecrets.getElementAt(i);
            int cat = categoriseSecret(secret, pcSecrets, deviceSecrets);
            if ((showLatestOnPC.isSelected() && cat == PC_VALUE)
                        || (showLatestOnPhone.isSelected() && cat == PHONE_VALUE)
                        || (showDeletedOnPC.isSelected() && cat == DELETED_ON_PC)
                        || (showDeletedOnDevice.isSelected() && cat == DELETED_ON_DEVICE)
                        || (showUnchanged.isSelected() && cat == EQUALS_VALUE)
                        || (showConflicts.isSelected() && cat == CONFLICT_VALUE)) {
               filteredSecrets.insert(secret);
            }
         }
         secretsList.setModel(filteredSecrets);
      }
   }

   /**
    * Confirm exit from sync operation
    */
   private class UnsavedChangesHandler extends WindowAdapter {
      @Override
      public void windowClosing(WindowEvent e) {
         if (pcSecrets.isChanged()) {
            int rc = JOptionPane.showConfirmDialog(SyncDialog.this, Messages.getString("SyncDialog.confirmexit"),
                        Messages.getString("MainWindow.unsavedchanges"), JOptionPane.YES_NO_OPTION);
            if (rc == JOptionPane.NO_OPTION) {
               return;
            }
         }
         if (isCancelled) {
            firePropertyChange(Constants.SYNC, null, Constants.CANCEL);
         }
         SyncDialog.this.dispose();
      }

   }

   /**
    * For each secret determine where the current "master" copy is, PC or phone.
    * Apply colour to the secrets list to indicate the source of the master copy
    * of the data.
    */
   private class ColourCellRenderer extends JLabel implements ListCellRenderer {

      @Override
      public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                  boolean cellHasFocus) {
         String s = value.toString();
         setText(s);
         int valueToUse = 0;
         valueToUse = SyncDialog.categoriseSecret(allSecrets.find(s), pcSecrets, deviceSecrets);

         if (isSelected) {
            setBackground(HIGHLIGHT_COLOURS[valueToUse]);
            setForeground(list.getSelectionForeground());
         } else {
            setBackground(NORMAL_COLOURS[valueToUse]);
            setForeground(list.getForeground());
         }
         setEnabled(list.isEnabled());
         setFont(list.getFont());
         setOpaque(true);
         return this;
      }
   }

   /**
    * Controls the enabling/disabling of the form buttons
    * 
    * The action buttons were originally only enabled if a conflict existed.
    * However, it appears useful to enable them for deletions in all cases to
    * provide for second thoughts...
    */
   private class ButtonFormListener implements ListSelectionListener {

      @Override
      public void valueChanged(ListSelectionEvent event) {
         buttonMarkAsMerged.setEnabled(false);
         buttonCopyToPC.setEnabled(false);
         if (event.getValueIsAdjusting() == false) {
            int index = ((JList) event.getSource()).getSelectedIndex();
            ListSecrets listModel = (ListSecrets) ((JList) event.getSource()).getModel();
            if (!(index < 0)) { // -1 indicates no selection
               ListSecret selectedSecret = (ListSecret) listModel.getElementAt(index);
               if (conflicts.contains(selectedSecret.getDescription()) || selectedSecret.isDeletedOnDevice() || selectedSecret.isDeletedOnPC()) {
                  buttonMarkAsMerged.setEnabled(true);
                  buttonCopyToPC.setEnabled(true);
               }
            }
         }
      }
   }

   /**
    * Cut-down secret class for use in JList
    * 
    * @author Chris Wood
    */
   public static class ListSecret {
      private String description;
      private boolean deletedOnPC;
      private boolean deletedOnDevice;
      private long deletedTimestamp;
      private boolean merged;

      @SuppressWarnings("javadoc")
      public ListSecret(String description) {
         this.description = description;
      }

      @SuppressWarnings("javadoc")
      public ListSecret(String description, long timestamp) {
         this.description = description;
         this.deletedTimestamp = timestamp;
      }

      /**
       * @return the description
       */
      public String getDescription() {
         return description;
      }

      /**
       * @return the deletedOnPC
       */
      public boolean isDeletedOnPC() {
         return deletedOnPC;
      }

      /**
       * @param deletedOnPC
       *           the deletedOnPC to set
       */
      public void setDeletedOnPC(boolean deletedOnPC) {
         this.deletedOnPC = deletedOnPC;
      }

      /**
       * @return the deletedOnDevice
       */
      public boolean isDeletedOnDevice() {
         return deletedOnDevice;
      }

      /**
       * @param deletedOnDevice
       *           the deletedOnDevice to set
       */
      public void setDeletedOnDevice(boolean deletedOnDevice) {
         this.deletedOnDevice = deletedOnDevice;
      }

      /**
       * @return the deletedTimestamp
       */
      public long getDeletedTimestamp() {
         return deletedTimestamp;
      }

      /**
       * @param deletedTimestamp
       *           the deletedTimestamp to set
       */
      public void setDeletedTimestamp(long deletedTimestamp) {
         this.deletedTimestamp = deletedTimestamp;
      }

      /**
       * @return the timestamp
       */
      public String getFormattedTimestamp() {
         return SecretsProperties.sdf.format(deletedTimestamp);
      }

      /**
       * @return the merged
       */
      public boolean isMerged() {
         return merged;
      }

      /**
       * @param merged
       *           the merged to set
       */
      public void setMerged(boolean merged) {
         this.merged = merged;
      }

      /*
       * (non-Javadoc)
       * 
       * @see java.lang.Object#toString()
       */
      @Override
      public String toString() {
         return description;
      }
   }

   /**
    * List class for JList secrets model
    * 
    * @author Chris Wood
    */
   public class ListSecrets extends AbstractListModel {
      private List<ListSecret> secrets = new ArrayList<ListSecret>();

      @Override
      public ListSecret getElementAt(int index) {
         return secrets.get(index);
      }

      @Override
      public int getSize() {
         return secrets.size();
      }

      @SuppressWarnings("javadoc")
      public void insert(ListSecret newSecret) {
         boolean added = false;
         for (int i = 0; i < secrets.size(); i++) {
            ListSecret secret = secrets.get(i);
            if (newSecret.getDescription().compareToIgnoreCase(secret.getDescription()) < 0) {
               secrets.add(i, newSecret);
               added = true;
               break;
            } else if (newSecret.getDescription().compareToIgnoreCase(secret.getDescription()) == 0) {
               throw new IllegalArgumentException("Duplicate key");
            }
         }
         if (!added) {
            secrets.add(newSecret);
         }
      }

      @SuppressWarnings("javadoc")
      public boolean contains(String key) {
         return !(find(key) == null);
      }

      @SuppressWarnings("javadoc")
      public ListSecret find(String key) {
         for (ListSecret secret : secrets) {
            if (secret.getDescription().equalsIgnoreCase(key)) {
               return secret;
            }
         }
         return null;
      }

      @SuppressWarnings("javadoc")
      public void delete(String key) {
         ListSecret secret = find(key);
         if (secret != null) {
            secrets.remove(secret);
         }
      }

      @SuppressWarnings("javadoc")
      public Iterator<ListSecret> iterator() {
         return secrets.iterator();
      }

      /**
       * Allow other classes involved in the model implementation to signal that
       * a refresh is needed. If the start index is negative, the whole range is
       * implied.
       * 
       * @param index
       */
      public void notifyRemoval(int index) {
         fireIntervalRemoved(this, index, index);
      }

   }
   
}
