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

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;

/**
 * This class extends the function of SecretFormListener. It modifies the
 * function of the PC form button to match the state of the secret: if the 
 * secret has been deleted on the device, the "Mark as merged" button becomes
 * "Confirm deletion"; if deleted on the PC, it becomes "Reload from the device".
 * 
 * @author Chris Wood
 */
public class PCFormListener extends SecretFormListener {
  private static Logger logger = Logger.getLogger(PCFormListener.class.getName());
  private JButton button;

  /**
   * Constructor
   * 
   * @param secrets
   * @param form
   * @param button the button on the form (normally "Mark as Merged")
   */
  public PCFormListener(SecretsCollection secrets, InputForm form, JButton button) {
    super(secrets, form);
    this.button = button;
  }

  /*
   * Modify the form text, button text and function to support deletions.
   * 
   * @see
   * com.ceperman.pcsecrets.SecretFormListener#doFormUpdate(com.ceperman.pcsecrets
   * .HostSecret)
   */
  @Override
  protected void doFormUpdate(SyncDialog.ListSecret listSecret, HostSecret mySecret) {
    logger.log(Level.FINE, "doFormUpdate: " + listSecret.toString());
    if (listSecret.isDeletedOnDevice()) {    
      logger.log(Level.FINE, "isDeletedOnDevice()");
      /* modify form for a deletion */
      button.setText("Confirm Deletion");
      button.setToolTipText("Delete the secret from PCSecrets");
      button.setActionCommand(SyncDialog.CMD_CONFIRM_DELETE);
      form.setTimestampLabel(Messages.getString("InputForm.timestamp"));
      super.doFormUpdate(listSecret, mySecret);
    } else if (listSecret.isDeletedOnPC()) {
      logger.log(Level.FINE, "isDeletedOnPC()");
      /* modify form for an undeletion */
      button.setText("Undelete");
      button.setToolTipText("Reload the secret from the device");
      button.setActionCommand(SyncDialog.CMD_RELOAD_FROM_DEVICE);
      form.setTimestampLabel("Deleted");
      form.setDeleted();
      form.getFieldDate().setText(listSecret.getFormattedTimestamp());
    } else {
      logger.log(Level.FINE, "revert to normal");
      /* revert form to normal */
      button.setText(Messages.getString("SyncDialog.markasmerged"));
      button.setToolTipText(Messages.getString("SyncDialog.markasmergedtooltip"));
      button.setActionCommand(Constants.MERGED);
      form.setTimestampLabel(Messages.getString("InputForm.timestamp"));
      super.doFormUpdate(listSecret, mySecret);
    }
  }
  
}
