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

import java.awt.Component;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.Timer;

/**
 * This class provides the dialog support for a swingworker thread. The swingworker itself is provided
 * by the subclass.
 * 
 * @author Chris Wood
 */
public abstract class AbstractPhoneCommunicator {
	@SuppressWarnings("unused")
	private static Logger logger = Logger.getLogger(AbstractPhoneCommunicator.class.getName());
	protected static String CHARSET = "UTF-8";
	
	protected Window parent;
	protected JDialog phoneConnectionDialog = new JDialog(parent);
	private final JLabel dialogInfo = new JLabel();
	private boolean suppressWaitMessage;

	/**
	 * @param parent
	 * @param connectionTask
	 */
	@SuppressWarnings("serial")
	protected AbstractPhoneCommunicator(Window parent) {
		this.parent = parent;
		
		phoneConnectionDialog = new JDialog(parent);
		phoneConnectionDialog.setTitle(Messages.getString("PhoneCommunicator.waittitle"));
		phoneConnectionDialog.setModalityType(ModalityType.DOCUMENT_MODAL);
		phoneConnectionDialog.getContentPane();
		JPanel pane = new JPanel();
		pane.setLayout(new BoxLayout(pane, BoxLayout.PAGE_AXIS));
		dialogInfo.setPreferredSize(new Dimension(300, 40));
		pane.add(dialogInfo);
		JButton buttonCancel = new JButton(Messages.getString("PCSecrets.cancel"));
		buttonCancel.setActionCommand(Constants.CANCEL);
		buttonCancel.addActionListener(new AbstractAction() {
			public void actionPerformed(ActionEvent event) {
				if (event.getActionCommand().equals(Constants.CANCEL)) {
					getConnectionTask().cancel(true);
				}
			}
		});
		pane.add(buttonCancel);
		buttonCancel.setAlignmentX(Component.CENTER_ALIGNMENT);
		dialogInfo.setAlignmentX(Component.CENTER_ALIGNMENT);
		dialogInfo.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(10,10,10,10), dialogInfo.getBorder()));
		
		Dimension spacerSize = new Dimension(5, 5);
		pane.add(new Box.Filler(spacerSize, spacerSize, spacerSize)); // space at bottom
		
		phoneConnectionDialog.add(pane);
		phoneConnectionDialog.pack();
		phoneConnectionDialog.setLocationRelativeTo(parent);
	}
	
	/**
	 * Allows the subclass to provide access to the SwingWorker task. 
	 * @return SwingWorker
	 */
	protected abstract SwingWorker<DeviceSecretsCollection, Void> getConnectionTask();
	
	/**
	 * Kick off the swingworker task
	 * Display the wait dialog if the task is not finished after a short time.
	 * Typically the task will finish very quickly, or else be waiting a relatively
	 * long time. We want to prevent the dialog appearing and immediately vanishing,
	 * leaving the user wondering what they'd just missed.
	 */
	public void start() {
		getConnectionTask().execute();
		  ActionListener taskPerformer = new ActionListener() {
		      public void actionPerformed(ActionEvent evt) {
		    	  if (!suppressWaitMessage && !getConnectionTask().isDone()) {
		    		  showDialog();
		    	  }
		      }
		  };
		  Timer timer = new Timer(200, taskPerformer); // wait for 200 msecs before showing dialog
		  timer.setRepeats(false);
		  timer.start();
	}

	/**
	 * Show the dialog.
	 * 
	 * @param infoText
	 */
	public void showDialog() {
		phoneConnectionDialog.setVisible(true);
	}

	/**
	 * Set the supplied text into the dialog info area, and show
	 * the dialog if not already visible.
	 * 
	 * @param infoText
	 */
	public void setDialogText(String infoText) {
		dialogInfo.setText(infoText);
	}
	
	/**
	 * Set this to prevent the wait dialog being shown
	 * @param suppressWaitMessage true if msg should be suppressed
	 */
	public void setSuppressWaitMessage(boolean suppressWaitMessage) {
		this.suppressWaitMessage = suppressWaitMessage;
	}
}
