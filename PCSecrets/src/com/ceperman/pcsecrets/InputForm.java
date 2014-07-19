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
import java.awt.Container;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.JTextComponent;

/**
 * Form used to show/get a secret and its fields
 * 
 * @author Chris Wood
 */
@SuppressWarnings("serial")
public class InputForm extends JPanel implements SecretsForm, KeyListener {

	/* form fields */
	private JTextField fieldDescr;
	private JTextField fieldId;
	private JTextField fieldPswd;
	private JTextField fieldEmail;
	private JTextArea areaNote;
	private JLabel fieldDate;
	
	/* mutatable form label text */
	private JLabel timestampDescription;
	
	private boolean changed;

	/**
	 * Constructor
	 */
	public InputForm() {
		setLayout(new GridBagLayout());

		GridBagConstraints c = new GridBagConstraints();
		// natural height, maximum width
		c.fill = GridBagConstraints.HORIZONTAL;
		Insets insetsTop = new Insets(10, 5, 5, 5);
		Insets insetsMiddle = new Insets(5, 5, 5, 5);
		Insets insetsBottom = new Insets(0, 5, 0, 5);

		JLabel labelDescr = new JLabel(Messages.getString("PCSecrets.descr"), JLabel.RIGHT);
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.insets = insetsTop;
		add(labelDescr, c);

		fieldDescr = new JTextField();
		fieldDescr.setColumns(10);
		c.gridx = 1;
		c.gridy = 0;
		c.weightx = 1.0;
		c.weighty = 0.0;
		c.insets = insetsTop;
		add(fieldDescr, c);

		JLabel labelId = new JLabel(Messages.getString("PCSecrets.id"), JLabel.RIGHT);
		c.gridx = 0;
		c.gridy = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.insets = insetsMiddle;
		add(labelId, c);

		fieldId = new JTextField();
		fieldId.setColumns(10);
		c.gridx = 1;
		c.gridy = 1;
		c.weightx = 1.0;
		c.weighty = 0.0;
		c.insets = insetsMiddle;
		add(fieldId, c);

		JLabel labelPswd = new JLabel(Messages.getString("PCSecrets.password"), JLabel.RIGHT);
		c.gridx = 0;
		c.gridy = 2;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.insets = insetsMiddle;
		add(labelPswd, c);

		fieldPswd = new JTextField();
		fieldId.setColumns(10);
		c.gridx = 1;
		c.gridy = 2;
		c.weightx = 1.0;
		c.weighty = 0.0;
		c.insets = insetsMiddle;
		add(fieldPswd, c);

		JLabel labelEmail = new JLabel(Messages.getString("PCSecrets.email"), JLabel.RIGHT);
		c.gridx = 0;
		c.gridy = 3;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.insets = insetsMiddle;
		add(labelEmail, c);

		fieldEmail = new JTextField();
		fieldEmail.setColumns(10);
		c.gridx = 1;
		c.gridy = 3;
		c.weightx = 1.0;
		c.weighty = 0.0;
		c.insets = insetsMiddle;
		add(fieldEmail, c);

		JLabel labelNote = new JLabel(Messages.getString("PCSecrets.note"), JLabel.RIGHT);
		c.gridx = 0;
		c.gridy = 4;
		c.weightx = 0.0;
		c.weighty = 1.0;
		c.insets = insetsBottom;
		c.anchor = GridBagConstraints.NORTHWEST;
		add(labelNote, c);

		areaNote = new JTextArea();
		areaNote.setLineWrap(true);
		areaNote.setWrapStyleWord(true);
		JScrollPane areaScrollPane = new JScrollPane(areaNote);
		areaScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		c.gridx = 1;
		c.gridy = 4;
		c.weightx = 1.0;
		c.weighty = 1.0;
		c.insets = insetsMiddle;
		c.fill = GridBagConstraints.BOTH;
		add(areaScrollPane, c);

		JLabel labelDate = new JLabel(Messages.getString("InputForm.timestamp"), JLabel.RIGHT);
		labelDate.setFont(new Font("SanSerif", Font.ITALIC, 12));
		c.gridx = 0;
		c.gridy = 5;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.insets = insetsMiddle;
		add(labelDate, c);
		timestampDescription = labelDate;

		fieldDate = new JLabel("", JLabel.LEFT);
		fieldDate.setFont(new Font("SanSerif", Font.PLAIN, 12));
		c.gridx = 1;
		c.gridy = 5;
		c.weightx = 1.0;
		c.weighty = 0.0;
		c.insets = insetsBottom;
		add(fieldDate, c);
		
		// note: no listener for the description as this defines a different secret
		fieldId.addKeyListener(this);
		fieldPswd.addKeyListener(this);
		fieldEmail.addKeyListener(this);
		areaNote.addKeyListener(this);
		
		installContextMenu(this);
	}
  
  /**
   * Set timestamp label
   * @param text label text
   */
  public void setTimestampLabel(String text) {
    timestampDescription.setText(text);
  }
	
	/**
	 * Clear the form fields
	 */
	public void clear() {
		fieldDescr.setText("");
		fieldId.setText("");
		fieldPswd.setText("");
		fieldEmail.setText("");
		areaNote.setText("");
		fieldDate.setText("");
	}
  
  /**
   * Set the form fields to "DELETED"
   */
  public void setDeleted() {
    String deletedTxt = Messages.getString("InputForm.deleted");
    fieldDescr.setText(deletedTxt);
    fieldId.setText(deletedTxt);
    fieldPswd.setText(deletedTxt);
    fieldEmail.setText(deletedTxt);
    areaNote.setText(deletedTxt);
  }
	
	/**
	 * Make form read-only
	 */
	public void setReadOnly() {
		fieldDescr.setEditable(false);
		fieldId.setEditable(false);
		fieldPswd.setEditable(false);
		fieldEmail.setEditable(false);
		areaNote.setEditable(false);
		areaNote.setBackground(getBackground());
	}
	
	/**
	 * Return the form data as a new HostSecret
	 * @return HostSecret
	 */
	public HostSecret getSecretFromInput() {
		return new HostSecret(fieldDescr.getText().trim(), 
							  fieldId.getText(),
							  fieldPswd.getText(),
							  fieldEmail.getText(),
							  areaNote.getText());
	}
	
	/**
	 * Update the supplied secret with data from the form fields
	 * @param secret 
	 * @return HostSecret
	 */
	public HostSecret updateSecretFromInput(HostSecret secret) {
		secret.setUsername(fieldId.getText());
		secret.setPassword(fieldPswd.getText());
		secret.setEmail(fieldEmail.getText());
		secret.setNote(areaNote.getText());
		return secret;
	}
	
	/**
	 * Set the form fields from the supplied secret
	 * @param secret
	 */
	public void setInputFromSecret(HostSecret secret) {
		fieldDescr.setText(secret.getDescription());
		fieldId.setText(secret.getUsername());
		fieldPswd.setText(secret.getPassword());
		fieldEmail.setText(secret.getEmail());
		areaNote.setText(secret.getNote());
		areaNote.setCaretPosition(0);
	}
	
	/**
	 * @return the fieldDescr
	 */
	public JTextField getFieldDescr() {
		return fieldDescr;
	}

	/**
	 * @return the fieldId
	 */
	public JTextField getFieldId() {
		return fieldId;
	}

	/**
	 * @return the fieldPswd
	 */
	public JTextField getFieldPswd() {
		return fieldPswd;
	}

	/**
	 * @return the fieldEmail
	 */
	public JTextField getFieldEmail() {
		return fieldEmail;
	}

	/**
	 * @return the areaNote
	 */
	public JTextArea getAreaNote() {
		return areaNote;
	}

	/**
	 * @return the fieldDate
	 */
	public JLabel getFieldDate() {
		return fieldDate;
	}

   /**
    * Check that the form has been changed
    * @return the changed
    */
   public boolean isChanged() {
      return changed;
   }

   /**
    * Remember that the form has been changed
    * @param changed the changed to set
    */
   public void setChanged(boolean changed) {
      this.changed = changed;
   }

   /*
    * Install/handle context menu for cut/copy/paste on text fields
    */
   private void installContextMenu(Container comp) {
      for (Component c : comp.getComponents()) {
         if (c instanceof JTextComponent) {
            c.addMouseListener(new MouseAdapter() {
               public void mousePressed(final MouseEvent e) {
                  if (e.isPopupTrigger()) handlePopupTrigger(e); // on Linux/Mac
               }
               public void mouseReleased(final MouseEvent e) {
                  if (e.isPopupTrigger()) handlePopupTrigger(e); // on Windows
               }
               private void handlePopupTrigger(final MouseEvent e) {
                  final JTextComponent component = (JTextComponent) e.getComponent();
                  final JPopupMenu menu = new JPopupMenu();
                  JMenuItem item;
                  item = new JMenuItem(new DefaultEditorKit.CopyAction());
                  item.setText(Messages.getString("PCSecrets.copy"));
                  item.setEnabled(component.getSelectionStart() != component.getSelectionEnd());
                  menu.add(item);
                  item = new JMenuItem(new DefaultEditorKit.CutAction());
                  item.setText(Messages.getString("PCSecrets.cut"));
                  item.setEnabled(component.isEditable()
                              && component.getSelectionStart() != component.getSelectionEnd());
                  menu.add(item);
                  item = new JMenuItem(new DefaultEditorKit.PasteAction());
                  item.setText(Messages.getString("PCSecrets.paste"));
                  item.setEnabled(component.isEditable());
                  menu.add(item);
                  menu.show(e.getComponent(), e.getX(), e.getY());
               }
            });
         } else if (c instanceof Container) installContextMenu((Container) c);
      }
   }
   
   private boolean ignoreNext = false;

   @Override
   /*
    * Detect CTRL-C to prevent it being seen as a change
    */
   public void keyPressed(KeyEvent e) {
      if (e.getKeyCode() == KeyEvent.VK_C && e.isControlDown()) {
         ignoreNext = true;
     }
   }

   @Override
   public void keyReleased(KeyEvent e) {
   }

   @Override
   public void keyTyped(KeyEvent e) {
      if (!ignoreNext) {
         setChanged(true); // set changed unless CTRL+c (copy)
      } else {
         ignoreNext = false;
      }
   }
}
