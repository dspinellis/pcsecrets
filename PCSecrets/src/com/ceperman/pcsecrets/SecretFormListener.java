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

import javax.swing.JList;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * Selection listener for a JList list of secrets. It transfers the contents of a
 * secret to the form provided.
 * The JList model (collection of secrets) used for the user selection may be different
 * from the collection to pull values from. Therefore the selection is used to obtain the
 * secret's key which is then used to access the correct secret in the source collection.
 * 
 * @author Chris Wood
 */
public class SecretFormListener implements ListSelectionListener {
//  private static Logger logger = Logger.getLogger(SecretFormListener.class.getName());
	private SecretsCollection secrets;
	protected InputForm form;

	/**
	 * Constructor
	 * @param secrets the collection of secrets to use
	 * @param form the form to be filled
	 */
	public SecretFormListener(SecretsCollection secrets, InputForm form) {
		this.secrets = secrets;
		this.form = form;
	}
	
	/**
	 * Override to provide form-specific behaviour
	 */
	protected void doFormUpdate(SyncDialog.ListSecret listsecret, HostSecret mySecret) {
	  if (mySecret != null) {
	    form.setInputFromSecret(mySecret);
	    form.getFieldDate().setText(mySecret.getFormattedTimestamp());
	  }
	}

	/* (non-Javadoc)
	 * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
	 */
	@SuppressWarnings("rawtypes")
   @Override
	public void valueChanged(ListSelectionEvent event) {
		if (event.getValueIsAdjusting() == false) {
			int index = ((JList) event.getSource()).getSelectedIndex();
			if (!(index < 0)) { /* -1 indicates no selection */
				SyncDialog.ListSecret selectedSecret = (SyncDialog.ListSecret)((JList)event.getSource()).getModel().getElementAt(index);
				HostSecret mySecret = secrets.get(selectedSecret.getDescription());
				if (!(selectedSecret.isDeletedOnDevice() || selectedSecret.isDeletedOnPC()) && mySecret == null) {
					form.clear();
				} else {
				  doFormUpdate(selectedSecret, mySecret);
				}
			}
		}
	}

}
