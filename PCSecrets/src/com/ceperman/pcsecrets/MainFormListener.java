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

import java.text.MessageFormat;

import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * Selection listener for the MainWindow JList list of secrets.
 * 
 * It transfers the contents of a secret to the form provided. Altogether simpler than the SyncDialog listener!
 * 
 * @author Chris Wood
 */
public class MainFormListener implements ListSelectionListener {
   protected InputForm form;
   protected MainWindow mainWindow;
   private static HostSecret previousSecret;

   /**
    * Constructor
    * 
    * @param mainWindow 
    * @param form
    *           the form to be filled
    */
   public MainFormListener(MainWindow mainWindow, InputForm form) {
      this.form = form;
      this.mainWindow = mainWindow;
   }

   /* (non-Javadoc)
    * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
    */
   @SuppressWarnings("unchecked")
   @Override
   public void valueChanged(ListSelectionEvent event) {
      if (event.getValueIsAdjusting() == false) {
         int index = ((JList<HostSecret>) event.getSource()).getSelectedIndex();
         SecretsCollection listModel = (SecretsCollection) ((JList<HostSecret>) event.getSource()).getModel();
         if (!(index < 0)) { /* -1 indicates no selection */
            HostSecret selectedSecret = (HostSecret) listModel.getElementAt(index);
            /* Only offer save if there was a previous secret selected and form data (not descr) was changed.
             * However, we have to handle the cases where the descr was changed AND some other field, so we
             * must check if the descr field is an existing secret, otherwise ignore.
             */
            if (selectedSecret != previousSecret && previousSecret != null && form.isChanged()) {
               String descr = form.getFieldDescr().getText().trim();
               if (descr.length() > 0 && listModel.contains(descr)) {
                  saveIfRequired(listModel);
               }
            }
            previousSecret = selectedSecret;
            form.setInputFromSecret(selectedSecret);
            form.getFieldDate().setText(selectedSecret.getFormattedTimestamp());
            form.setChanged(false);
            
            // set inactivity timer if necessary
            mainWindow.setIdleTimer();
         }
      }
   }
   
   private void saveIfRequired(SecretsCollection listModel) {
      String descr = form.getFieldDescr().getText().trim();
      Object[] options = { Messages.getString("InputForm.update"), Messages.getString("InputForm.discard") };
      int n = JOptionPane.showOptionDialog(mainWindow,
                  MessageFormat.format(Messages.getString("InputForm.changed"), descr),
                  Messages.getString("InputForm.changedtitle"),
                  JOptionPane.YES_NO_OPTION,
                  JOptionPane.INFORMATION_MESSAGE,
                  null,     //do not use a custom Icon
                  options,  //the titles of buttons
                  options[0]); //default button title
      if (n == 0) {
         HostSecret newSecret = form.getSecretFromInput();
         listModel.addOrUpdate(newSecret);
         mainWindow.getAreaMsg().setText(MessageFormat.format(Messages.getString("PCSecrets.updated"), form.getFieldDescr().getText()));
      }
   }

}