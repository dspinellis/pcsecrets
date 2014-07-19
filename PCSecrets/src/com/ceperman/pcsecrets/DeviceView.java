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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * Dialog to show details of the known sync devices. Includes a button to 
 * allow the user to forget a device i.e. discard all its details.
 * 
 * @author Chris Wood
 */
@SuppressWarnings("serial")
public class DeviceView extends JDialog implements ActionListener {
   private static Logger logger = Logger.getLogger(DeviceView.class.getName());

   /* Action commands */
   private static final String CLOSE = "close";
   private static final String FORGET = "forget";
   private static final String UPDATE = "update";

   private JList<?> jDeviceList;
   private JButton jButtonClose;
   private JButton jButtonForget;
   private JButton jButtonUpdate;

   private SyncDeviceCollection syncDevices;

   private DeviceForm deviceForm;

   /**
    * Constructor
    * @param owner
    * @param syncDevices
    */
   public DeviceView(Frame owner, SyncDeviceCollection syncDevices) {
      this.syncDevices = syncDevices;

      setTitle(Messages.getString("DeviceView.title"));
      setModalityType(ModalityType.APPLICATION_MODAL);
      createUI(owner);
   }

   private void createUI(Frame owner) {
      /* content */
      JPanel jDevicesPane = new JPanel(new BorderLayout());
      jDevicesPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 0));
      JPanel jDeviceForm = new JPanel(new BorderLayout());

      /* split pane to hold list and form views */
      JSplitPane jSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, jDevicesPane, jDeviceForm);
      jSplitPane.setResizeWeight(0.5);
      getContentPane().add(jSplitPane, BorderLayout.CENTER);

      /* list view */
      jDeviceList = new JList<SyncDevice>(syncDevices);
      JScrollPane jScrollPane = new JScrollPane(jDeviceList);
      jScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
      jDevicesPane.add(jScrollPane, BorderLayout.CENTER);
      jDeviceList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

      /* list buttons */
      JPanel listButtons = new JPanel(new FlowLayout());
      jButtonClose = new JButton(Messages.getString("DeviceView.close"));
      jButtonClose.setToolTipText(Messages.getString("DeviceView.closetooltip"));
      jButtonClose.setActionCommand(CLOSE);
      jButtonClose.setMnemonic(KeyEvent.VK_C);
      jButtonClose.addActionListener(this);
      jButtonClose.setEnabled(true);
      listButtons.add(jButtonClose);
      jDevicesPane.add(listButtons, BorderLayout.SOUTH);

      /* details view */
      deviceForm = new DeviceForm();
      jDeviceList.addListSelectionListener(deviceForm.new DeviceListListener(deviceForm));
      jDeviceList.addListSelectionListener(new ButtonFormListener());
      jDeviceForm.add(deviceForm, BorderLayout.CENTER);

      /* form buttons */
      jDeviceForm.add(createButtons(this), BorderLayout.SOUTH);

      setPreferredSize(new Dimension(600, 200));
      setSize(getPreferredSize()); // force list pane to preferred size
      setLocationRelativeTo(owner);
      setDefaultCloseOperation(DISPOSE_ON_CLOSE);
   }

   private JPanel createButtons(ActionListener actionListener) {
      /* edit buttons */
      JPanel buttons = new JPanel(new FlowLayout());
      jButtonForget = new JButton(Messages.getString("DeviceView.forget"));
      jButtonForget.setToolTipText(Messages.getString("DeviceView.forgettooltip"));
      jButtonForget.setActionCommand(FORGET);
      jButtonForget.setMnemonic(KeyEvent.VK_F);
      jButtonForget.addActionListener(actionListener);
      jButtonForget.setEnabled(false);
      buttons.add(jButtonForget);
      jButtonUpdate = new JButton(Messages.getString("DeviceView.update"));
      jButtonUpdate.setToolTipText(Messages.getString("DeviceView.updatetooltip"));
      jButtonUpdate.setActionCommand(UPDATE);
      jButtonUpdate.setMnemonic(KeyEvent.VK_U);
      jButtonUpdate.addActionListener(actionListener);
      jButtonUpdate.setEnabled(false);
      buttons.add(jButtonUpdate);

      return buttons;
   }

   @Override
   public void actionPerformed(ActionEvent event) {
      logger.log(Level.INFO, "command is " + event.getActionCommand());
      SyncDevice selectedDevice = (SyncDevice) jDeviceList.getSelectedValue();
      int index = jDeviceList.getSelectedIndex();
      if (event.getActionCommand().equals(FORGET)) {
         syncDevices.remove(selectedDevice.getId());
         syncDevices.intervalRemoved(index);
         syncDevices.setChanged(true);
         logger.log(Level.INFO, "device removed: " + selectedDevice.getId());
         /* ButtonFormListener is not called here even tho the selection has just 
          * been deleted (bug? design?), so disable the buttons here.
          */
         jButtonForget.setEnabled(false);
         jButtonUpdate.setEnabled(false);
         deviceForm.clear();
      } else if (event.getActionCommand().equals(UPDATE)) {
         selectedDevice.setDisplayName(deviceForm.getDisplayName());
         syncDevices.contentsChanged();
         syncDevices.setChanged(true);
      } else if (event.getActionCommand().equals(CLOSE)) {
         this.dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
      }
   }

   /**
    * Controls the enabling/disabling of the form buttons
    */
   private class ButtonFormListener implements ListSelectionListener {
      @Override
      public void valueChanged(ListSelectionEvent event) {
         jButtonForget.setEnabled(false);
         jButtonUpdate.setEnabled(false);
         if (event.getValueIsAdjusting() == false) {
            int index = ((JList<?>) event.getSource()).getSelectedIndex();
            if (!(index < 0)) { // -1 indicates no selection
               jButtonForget.setEnabled(true);
               jButtonUpdate.setEnabled(true);
            }
         }
      }

   }

   private class DeviceForm extends JPanel {
      private JLabel fieldId = new JLabel("", JLabel.LEFT);
      private JTextField fieldName = new JTextField();
      private JLabel fieldIP = new JLabel("", JLabel.LEFT);
      private JLabel fieldTimestamp = new JLabel("", JLabel.LEFT);

      public DeviceForm() {
         setLayout(new GridBagLayout());

         GridBagConstraints c = new GridBagConstraints();
         c.anchor = GridBagConstraints.NORTH;
         // natural height, maximum width
         c.fill = GridBagConstraints.HORIZONTAL;
         Insets insetsTop = new Insets(10, 5, 5, 5);
         Insets insetsMiddle = new Insets(5, 5, 5, 5);
         Insets insetsBottom = new Insets(0, 5, 0, 5);

         JLabel labelId = new JLabel(Messages.getString("DeviceView.id"), JLabel.RIGHT);
         c.gridx = 0;
         c.gridy = 0;
         c.weightx = 0.0;
         c.weighty = 0.0;
         c.insets = insetsTop;
         add(labelId, c);
         
         fieldId.setFont(new Font("SanSerif", Font.PLAIN, 12));
         c.gridx = 1;
         c.gridy = 0;
         c.weightx = 1.0;
         c.weighty = 0.0;
         c.insets = insetsTop;
         add(fieldId, c);

         JLabel labelName = new JLabel(Messages.getString("DeviceView.name"), JLabel.RIGHT);
         c.gridx = 0;
         c.gridy = 1;
         c.weightx = 0.0;
         c.weighty = 0.0;
         c.insets = insetsMiddle;
         add(labelName, c);
         
         fieldName.setColumns(10);
         c.gridx = 1;
         c.gridy = 1;
         c.weightx = 1.0;
         c.weighty = 0.0;
         c.insets = insetsMiddle;
         add(fieldName, c);

         JLabel labelIP = new JLabel(Messages.getString("DeviceView.lastip"), JLabel.RIGHT);
         c.gridx = 0;
         c.gridy = 2;
         c.weightx = 0.0;
         c.weighty = 0.0;
         c.insets = insetsMiddle;
         add(labelIP, c);
         
         fieldIP.setFont(new Font("SanSerif", Font.PLAIN, 12));
         c.gridx = 1;
         c.gridy = 2;
         c.weightx = 1.0;
         c.weighty = 0.0;
         c.insets = insetsMiddle;
         add(fieldIP, c);

         JLabel labelTimestamp = new JLabel(Messages.getString("DeviceView.timestamp"), JLabel.RIGHT);
         c.gridx = 0;
         c.gridy = 3;
         c.weightx = 0.0;
         c.weighty = 1.0;
         c.insets = insetsBottom;
         add(labelTimestamp, c);

         
         fieldTimestamp.setFont(new Font("SanSerif", Font.PLAIN, 12));
         c.gridx = 1;
         c.gridy = 3;
         c.weightx = 1.0;
         c.weighty = 1.0;
         c.insets = insetsBottom;
         add(fieldTimestamp, c);
      }

      /**
       * Clear the form fields
       */
      public void clear() {
         fieldName.setText("");
         fieldId.setText("");
         fieldIP.setText("");
         fieldTimestamp.setText("");
      }

      /**
       * Set the form fields from the supplied SyncDevice
       * @param device
       */
      public void setInputFromDeviceInfo(SyncDevice device) {
         fieldName.setText(device.getDisplayName());
         fieldId.setText(device.getId());
         String lastIP = device.getLastIP();
         if (lastIP == null) {
            fieldIP.setText(Messages.getString("DeviceView.notknown"));
         } else {
            fieldIP.setText(device.getLastIP());
         }
         long timestamp = device.getSyncTimestamp();
         if (timestamp > 0) {
            fieldTimestamp.setText(SecretsProperties.sdf.format(device.getSyncTimestamp()));
         } else {
            fieldTimestamp.setText(Messages.getString("DeviceView.notknown"));
         }
      }

      /**
       * @return the fieldName value
       */
      public String getDisplayName() {
         return fieldName.getText();
      }

      /**
       * Listens to the device list selection and updates the form 
       */
      private class DeviceListListener implements ListSelectionListener {
         private DeviceForm form;

         public DeviceListListener(DeviceForm form) {
            this.form = form;
         }

         @Override
         public void valueChanged(ListSelectionEvent event) {
            if (event.getValueIsAdjusting() == false) {
               int index = ((JList<?>) event.getSource()).getSelectedIndex();
               SyncDeviceCollection listModel = (SyncDeviceCollection)((JList<?>)event.getSource()).getModel();
               if (!(index < 0)) { /* -1 indicates no selection */
                  SyncDevice selectedDevice = (SyncDevice) listModel.getElementAt(index);
                  form.setInputFromDeviceInfo(selectedDevice);
               } else {
                  form.clear();
               }
            }
         }    
      }
   }

   /**
    * For testing only
    * 
    * @param args
    */
   public static void main(String[] args) {
      SecretsProperties.getInitialInstance("temp", false);
      SyncDeviceCollection sdc = new SyncDeviceCollection();
      sdc.add(new SyncDevice("1234567890"));
      sdc.add(new SyncDevice("abcdefghik"));
      // Create a frame and place in centre of screen
      JFrame frame = new JFrame("Test DeviceView");
      Dimension maxWindow = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().getSize();
      Dimension windowSize = new Dimension(400, 300);
      frame.setSize(windowSize);
      frame.setLocation(maxWindow.width/2 - windowSize.width/2, maxWindow.height/2 - windowSize.height/2);
      frame.setVisible(true);
      frame.setDefaultCloseOperation(EXIT_ON_CLOSE);
      DeviceView view = new DeviceView(frame, sdc);
      view.setVisible(true);
   }

}
