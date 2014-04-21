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
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;

import com.ceperman.utils.Chars;
import com.ceperman.utils.Strings;

/**
 * Creates elements for main UI
 * @author Chris Wood
 */
public class UIUtils {
	private static Logger logger = Logger.getLogger(UIUtils.class.getName());
	
	private static final String VERSION_BUNDLE_NAME = "com.ceperman.pcsecrets.version";
	
	static final int OK_OPTION = 0;
	static final int RESET_OPTION = 1;
	static final int CANCEL_OPTION = 2;
	
	private static JPasswordField pwField;
	private static JPasswordField newPswd1;
	private static JPasswordField newPswd2;
	private static JCheckBox showCheckbox = new JCheckBox();
	
	/**
	 * Create menu bar for the main window
	 * @param actionListener 
	 * @return JMenuBar
	 */
	public static JMenuBar createMenuBar(ActionListener actionListener) {
		JMenuBar jmenubar = new JMenuBar();

		JMenu jmenu = new JMenu();
		jmenu.setText(Messages.getString("PCSecrets.file"));
		jmenubar.add(jmenu);

		JMenuItem jmenuitem = new JMenuItem();
		jmenuitem.setText(Messages.getString("PCSecrets.export"));
		jmenuitem.setActionCommand(Constants.EXPORT);
		jmenuitem.setMnemonic(KeyEvent.VK_E);
		jmenuitem.addActionListener(actionListener);
		jmenu.add(jmenuitem);
		jmenuitem = new JMenuItem();
		jmenuitem.setText(Messages.getString("PCSecrets.import"));
		jmenuitem.setActionCommand(Constants.IMPORT);
		jmenuitem.setMnemonic(KeyEvent.VK_I);
		jmenuitem.addActionListener(actionListener);
		jmenu.add(jmenuitem);
		jmenuitem = new JMenuItem();
		jmenuitem.setText(Messages.getString("PCSecrets.preferences"));
		jmenuitem.setActionCommand(Constants.PREFERENCES);
		jmenuitem.setMnemonic(KeyEvent.VK_P);
		jmenuitem.addActionListener(actionListener);
		jmenu.add(jmenuitem);

      jmenuitem = new JMenuItem();
      jmenuitem.setText(Messages.getString("PCSecrets.changepswd"));
      jmenuitem.setActionCommand(Constants.CHANGEPSWD);
      jmenuitem.setMnemonic(KeyEvent.VK_C);
      jmenuitem.addActionListener(actionListener);
      jmenu.add(jmenuitem);

      jmenuitem = new JMenuItem();
      jmenuitem.setText(Messages.getString("PCSecrets.regenciphers"));
      jmenuitem.setActionCommand(Constants.REGENCIPHERS);
      jmenuitem.setMnemonic(KeyEvent.VK_R);
      jmenuitem.addActionListener(actionListener);
      jmenu.add(jmenuitem);
		
		jmenuitem = new JMenuItem();
		jmenuitem.setText(Messages.getString("PCSecrets.exit"));
		jmenuitem.setActionCommand(Constants.EXIT);
		jmenuitem.setMnemonic(KeyEvent.VK_X);
		jmenuitem.addActionListener(actionListener);
      jmenu.add(jmenuitem);

      jmenu = new JMenu();
      jmenu.setText(Messages.getString("PCSecrets.view"));
      jmenubar.add(jmenu);

      jmenuitem = new JMenuItem();
      jmenuitem.setText(Messages.getString("PCSecrets.devices"));
      jmenuitem.setActionCommand(Constants.DEVICES);
      jmenuitem.addActionListener(actionListener);
      jmenu.add(jmenuitem);

		jmenu = new JMenu();
		jmenu.setText(Messages.getString("PCSecrets.help"));
		jmenubar.add(jmenu);
		
		jmenuitem = new JMenuItem();
		jmenuitem.setText(Messages.getString("PCSecrets.help"));
		jmenuitem.setActionCommand(Constants.HELP);
		jmenuitem.addActionListener(actionListener);
		jmenu.add(jmenuitem);
		jmenuitem = new JMenuItem();
      jmenuitem.setText(Messages.getString("PCSecrets.sysinfo"));
      jmenuitem.setActionCommand(Constants.SYSINFO);
      jmenuitem.addActionListener(actionListener);
      jmenu.add(jmenuitem);
		jmenuitem = new JMenuItem();
		jmenuitem.setText(Messages.getString("PCSecrets.aboutdots"));
		jmenuitem.setActionCommand(Constants.ABOUT);
		jmenuitem.addActionListener(actionListener);
		jmenu.add(jmenuitem);
		
		return jmenubar;
	}
	
	/**
	 * Create input form buttons for the main window
	 * @param actionListener 
	 * @return JPanel
	 */
	public static JPanel createInputFormButtons(ActionListener actionListener) {
		/* edit buttons */
		JPanel editButtons = new JPanel(new FlowLayout());
		JButton jButtonCreate = new JButton(Messages.getString("PCSecrets.create"));
		jButtonCreate.setToolTipText(Messages.getString("PCSecrets.createtooltip"));
		jButtonCreate.setActionCommand(Constants.CREATE);
		jButtonCreate.setMnemonic(KeyEvent.VK_C);
		jButtonCreate.addActionListener(actionListener);
		editButtons.add(jButtonCreate);
		JButton jButtonUpdate = new JButton(Messages.getString("PCSecrets.update"));
		jButtonUpdate.setToolTipText(Messages.getString("PCSecrets.updatetooltip"));
		jButtonUpdate.setActionCommand(Constants.UPDATE);
		jButtonUpdate.setMnemonic(KeyEvent.VK_U);
		jButtonUpdate.addActionListener(actionListener);
		editButtons.add(jButtonUpdate);
		JButton jButtonDelete = new JButton(Messages.getString("PCSecrets.delete"));
		jButtonDelete.setToolTipText(Messages.getString("PCSecrets.deletetooltip"));
		jButtonDelete.setActionCommand(Constants.DELETE);
		jButtonDelete.setMnemonic(KeyEvent.VK_D);
		jButtonDelete.addActionListener(actionListener);
		editButtons.add(jButtonDelete);
		JButton jButtonClear = new JButton(Messages.getString("PCSecrets.clear"));
		jButtonClear.setToolTipText(Messages.getString("PCSecrets.cleartooltip"));
		jButtonClear.setActionCommand(Constants.CLEAR);
		jButtonClear.setMnemonic(KeyEvent.VK_R);
		jButtonClear.addActionListener(actionListener);
		editButtons.add(jButtonClear);
		
		return editButtons;
	}
	
	/**
	 * Create panel with titled and spaced borders
	 * @param title
	 * @return panel
	 */
	public static JPanel createTitledBorderedPanel(String title) {
		JPanel titledPanel = new JPanel();
		titledPanel.setLayout(new BorderLayout());
		titledPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(5,5,5,5),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.gray), title), 
                        BorderFactory.createEmptyBorder(5,5,5,5))));
		return titledPanel;
	}
	
	/**
	 * Show About dialog
	 * @param mainWindow
	 */
   public static void aboutDialog(MainWindow mainWindow) {
      String version = ResourceBundle.getBundle(VERSION_BUNDLE_NAME).getString("version");
      JPanel msgPane = new JPanel(new BorderLayout());
      JLabel versionText = new JLabel(Messages.getString("About.versiontext") + " " + version, JLabel.CENTER);
      versionText.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
      List<Image> images = mainWindow.getIconImages();
      versionText.setIcon(new ImageIcon(images.get(1))); // medium size image
      msgPane.add(versionText, BorderLayout.NORTH);
      JLabel txt = new JLabel(Messages.getString("About.text"), JLabel.CENTER);
      txt.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
      msgPane.add(txt, BorderLayout.CENTER);
      JLabel copyright = new JLabel(Messages.getString("About.copyright"), JLabel.CENTER);
      msgPane.add(copyright, BorderLayout.SOUTH);
      JOptionPane.showMessageDialog(mainWindow, msgPane, Messages.getString("About.title"), JOptionPane.PLAIN_MESSAGE);
   }
   
   /**
    * Show system info dialog
    * @param mainWindow
    */
   public static void sysInfoDialog(MainWindow mainWindow) {
      JPanel msgPane = new JPanel(new BorderLayout());
      msgPane.setLayout(new BoxLayout(msgPane, BoxLayout.PAGE_AXIS));
      String[] keys = SecretsProperties.loggedProps;
      Properties props = System.getProperties();
      for (int i = 0; i < keys.length; i++) {
         String txt = keys[i] + " = " + props.getProperty(keys[i]);
         final int maxline = 100;
         if (txt.length() > maxline) {
            int offset = 0;
            do {
               int txtlen = Math.min(maxline, txt.length() - offset);
               String subtxt = txt.substring(offset, offset + txtlen);
               JLabel info = new JLabel(subtxt, JLabel.LEFT);
               msgPane.add(info);
               offset += txtlen;
            } while (offset < txt.length());
         } else {
            JLabel info = new JLabel(txt, JLabel.LEFT);
            msgPane.add(info);
         }
      }
      JOptionPane.showMessageDialog(mainWindow, msgPane, Messages.getString("UIUtils.sysinfotitle"), JOptionPane.PLAIN_MESSAGE);
   }
	
	/**
	 * Request password
	 * 
	 * @param mainWindow
	 * @param msgTxt 
	 * @return byte[] password or null if cancelled
	 */
	public static byte[] getPassword(MainWindow mainWindow, String msgTxt) {
		byte[] retval = null;
		JPanel msgPane = new JPanel(new BorderLayout());
		pwField = new JPasswordField();
		JLabel txt = new JLabel(msgTxt);
		txt.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
		msgPane.add(txt, BorderLayout.CENTER);
		msgPane.add(pwField, BorderLayout.SOUTH);		
		char[] password = pwField.getPassword();
		while (password.length == 0) {
			JOptionPane optionPane = new JOptionPane(msgPane, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
			JDialog dialog = optionPane.createDialog(mainWindow, Messages.getString("UIUtils.passwordtitle"));
			dialog.addWindowFocusListener(new FocusSetter());
			dialog.pack();
			dialog.setVisible(true);
		    int action = ((Integer)optionPane.getValue()).intValue();
			if (action == JOptionPane.CANCEL_OPTION) {
				password = null;
				break;
			}
			password = pwField.getPassword();
		}
		
		if (password != null) {
			try {
				/* convert chars into bytes without using an intermediate String class */
				CharsetEncoder encoder = Charset.forName("UTF-8").newEncoder();
				ByteBuffer bbuf = encoder.encode(CharBuffer.wrap(password));
				retval = new byte[bbuf.limit()];
				System.arraycopy(bbuf.array(), 0, retval, 0, bbuf.limit());
			} catch (CharacterCodingException e) {
				password = null;
			}
		}
		return retval;
	}
	
	/**
    * Get a new password
    * 
    * @param mainWindow
	 * @return pswd or null
    */
	public static byte[] getNewPassword(MainWindow mainWindow) {
	   byte[] pswd = getResetPassword(mainWindow);
	   if (pswd != null) {
         Object[] options = { Messages.getString("UIUtils.savenow"), Messages.getString("UIUtils.savelater") };
	      int n = JOptionPane.showOptionDialog(mainWindow,
	                  Messages.getString("UIUtils.saveneeded"),
	                  Messages.getString("UIUtils.saveneededtitle"),
	                  JOptionPane.YES_NO_OPTION,
	                  JOptionPane.INFORMATION_MESSAGE,
	                  null,     //do not use a custom Icon
	                  options,  //the titles of buttons
	                  options[0]); //default button title
	      if (n == 0) {
	         mainWindow.saveSecrets();
	         mainWindow.getListModel().setChanged(false);
	      }
	   }
	   return pswd;
	}
   
   /**
    * Get a new password
    * 
    * @param mainWindow
    * @return pswd or null
    */
   public static byte[] getResetPassword(MainWindow mainWindow) {
      byte[] pswd = getConfirmedPassword(mainWindow, "UIUtils.passwordtitle", "UIUtils.providenewpassword");
      if (pswd != null) {
         // reset keylength property to default
         SecretsProperties props = SecretsProperties.getInstance();
         props.updateProperty(Constants.KEYLENGTH, props.getDefaultProperty(Constants.KEYLENGTH));
         
         mainWindow.getListModel().setPswdBytes(pswd);
         mainWindow.getListModel().createCipherInfo();
         mainWindow.getListModel().setChanged(true);
      }
      return pswd;
   }
   
   /**
    * Get a password with confirmation
    * 
    * @param mainWindow
    * @param title id of title message string
    * @param intro id of intro text message string
    * @return password or null if cancelled
    */
   public static byte[] getConfirmedPassword(MainWindow mainWindow, 
               String title, String intro) {
      byte[] pwsdBytes = null;
      
      JPanel inputPane = new JPanel(new BorderLayout());
      JLabel msgText = new JLabel(Messages.getString(intro));
      msgText.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
      inputPane.add(msgText, BorderLayout.NORTH);
      JPanel pswdPane = new JPanel();
      pswdPane.setLayout(new GridLayout(2, 2, 5, 5));
      pswdPane.add(new JLabel(Messages.getString("UIUtils.enternewpassword"), JLabel.RIGHT));
      newPswd1 = new JPasswordField(15);
      pswdPane.add(newPswd1);
      pswdPane.add(new JLabel(Messages.getString("UIUtils.reenternewpassword"), JLabel.RIGHT));
      newPswd2 = new JPasswordField(15);
      pswdPane.add(newPswd2);
      inputPane.add(pswdPane, BorderLayout.CENTER);
      
      JPanel showPane = new JPanel(new FlowLayout(FlowLayout.RIGHT));
      showCheckbox.setBorder(BorderFactory.createEmptyBorder(10, 30, 10, 0));
      showCheckbox.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent arg0) {
            if (showCheckbox.isSelected()) {
               newPswd1.setEchoChar((char) 0);
               newPswd2.setEchoChar((char) 0);
            } else {
               newPswd1.setEchoChar('*');
               newPswd2.setEchoChar('*');
            }
         }
      });
      showPane.add(showCheckbox);
      showPane.add(new JLabel(Messages.getString("UIUtils.showpassword")));
      inputPane.add(showPane, BorderLayout.SOUTH);
      
      char[] password = null;
      int msgType = JOptionPane.INFORMATION_MESSAGE;

      /* ensure no exit (except CANCEL) without valid password */
      while (password == null || password.length == 0) {
         String[] options  = { Messages.getString("UIUtils.optok"), Messages.getString("UIUtils.optcancel") };
         JOptionPane optionPane = new JOptionPane(inputPane, msgType, JOptionPane.OK_CANCEL_OPTION, null, options, options[0]);
         JDialog dialog = optionPane.createDialog(mainWindow, Messages.getString(title));
         dialog.addWindowFocusListener(new WindowFocusListener() {
            public void windowGainedFocus(WindowEvent arg0) {
               newPswd1.requestFocusInWindow();
            }
            public void windowLostFocus(WindowEvent arg0) { }
         } );
         dialog.pack();
         dialog.setVisible(true);
         /* this variation of JOptionPane returns the option and not the index */
         int optval = Strings.indexOf((String) optionPane.getValue(), options);
         if (optval == 1) { // cancel
            break;
         }
         
         if (Chars.trim(newPswd1.getPassword()).length == 0 && Chars.trim(newPswd2.getPassword()).length == 0) {
            msgText.setText(Messages.getString("UIUtils.nopasswordprovided"));
            msgType = JOptionPane.WARNING_MESSAGE;
            newPswd1.setText(""); // in case spaces were entered
            newPswd2.setText(""); // in case spaces were entered
         } else {
            password = getPasswordFromFields(newPswd1, newPswd2);
            if (password != null && password.length > 0) {
               password = Chars.trim(password); // trim leading/trailing spaces

               /* if the char encoding fails then we will ask for a new password */
               try {
                  /* convert chars into bytes without using an intermediate String class */
                  CharsetEncoder encoder = Charset.forName("UTF-8").newEncoder();
                  ByteBuffer bbuf = encoder.encode(CharBuffer.wrap(password));
                  // bug001 2014/04/15 only chars up to buf limit should be copied
                  pwsdBytes = new byte[bbuf.limit()];
                  System.arraycopy(bbuf.array(), 0, pwsdBytes, 0, bbuf.limit());
               } catch (CharacterCodingException e) {
                  password = null;
               }
            } else {
               msgText.setText(Messages.getString("UIUtils.passwordsnotsame"));
               msgType = JOptionPane.WARNING_MESSAGE;
            }
         }
        
      }
      logger.log(Level.FINE, (pwsdBytes == null ? "no " : "") + "password returned");
      
      return pwsdBytes;
   }
	
	/**
	 * Get password offering reset option.
	 * 
	 * @param mainWindow
	 * @return 0 = password supplied, 1 = reset, 2 = cancel
	 */
	public static int getOrResetPassword(MainWindow mainWindow) {
		int retval = 0;
		JPanel msgPane = new JPanel(new BorderLayout());
		JPanel resetPane = new JPanel(new BorderLayout());
		JLabel wrongPswdTxt = new JLabel(Messages.getString("UIUtils.wrongpassword"));
		wrongPswdTxt.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
		JLabel resetTxt = new JLabel(Messages.getString("UIUtils.resetoption"));
		resetTxt.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
		JLabel warnTxt = new JLabel(Messages.getString("UIUtils.resetwarning"));
		warnTxt.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
		resetPane.add(resetTxt, BorderLayout.NORTH);
		resetPane.add(warnTxt, BorderLayout.SOUTH);
		pwField = new JPasswordField();
		msgPane.add(wrongPswdTxt, BorderLayout.NORTH);
		msgPane.add(pwField, BorderLayout.CENTER);
		msgPane.add(resetPane, BorderLayout.SOUTH);
		char[] password = pwField.getPassword();
		
		/* ensure no exit (except CANCEL) without valid password */ 
		while (password == null || password.length == 0) {
			String[] options  = { Messages.getString("UIUtils.optok"), Messages.getString("UIUtils.optnew"), Messages.getString("UIUtils.optcancel") };
			JOptionPane optionPane = new JOptionPane(msgPane, JOptionPane.WARNING_MESSAGE, JOptionPane.YES_NO_CANCEL_OPTION, null, options, options[0]);
			JDialog dialog = optionPane.createDialog(mainWindow, Messages.getString("UIUtils.passwordtitle"));
			dialog.addWindowFocusListener(new WindowFocusListener() {
            public void windowGainedFocus(WindowEvent arg0) {
               pwField.requestFocusInWindow();
            }
            public void windowLostFocus(WindowEvent arg0) { }
         } );
			dialog.pack();
			dialog.setVisible(true);
			/* this variation of JOptionPane returns the option and not the index */
			retval = Strings.indexOf((String) optionPane.getValue(), options);
			if (retval > 0) {
				break;
			}
			
			password = Chars.trim(pwField.getPassword()); // trim leading/trailing spaces
			
			/* if the char encoding fails then we will ask for a new password */			
			try {
				/* convert chars into bytes without using an intermediate String class */
				CharsetEncoder encoder = Charset.forName("UTF-8").newEncoder();
				ByteBuffer bbuf = encoder.encode(CharBuffer.wrap(password));
			// bug001 2014/04/15 only chars up to buf limit should be copied
				byte[] pswd = new byte[bbuf.limit()];
            System.arraycopy(bbuf.array(), 0, pswd, 0, bbuf.limit());
				mainWindow.getListModel().setPswdBytes(pswd);
			} catch (CharacterCodingException e) {
				password = null;
			}
			
			if (retval == 1) {
			   break;
			}
			
			/* for a reshow of this dialog, change the main text to just enter a password */
			wrongPswdTxt.setText(Messages.getString("UIUtils.passwordplease")); 
		}
		
		return retval;
	}
	
	private static char[] getPasswordFromFields(JPasswordField field1, JPasswordField field2) {
      char[] p1 = field1.getPassword();
      char[] p2 = field2.getPassword();
      if (Arrays.equals(p1, p2)) {
         return p1;
      } else {
         return null;
      }
   }
	
	/**
	 * Show restart required dialog
	 * @param mainWindow
	 */
	public static void restartDialog(MainWindow mainWindow) {
		JOptionPane.showMessageDialog(mainWindow, Messages.getString("UIUtils.restartreqd"), Messages.getString("UIUtils.restartTitle"), JOptionPane.PLAIN_MESSAGE);
   }

   /**
    * Gives focus to the password field in the dialogs.
    */
   private static class FocusSetter implements WindowFocusListener {
      @Override
      public void windowGainedFocus(WindowEvent arg0) {
         pwField.requestFocusInWindow();
      }

      @Override
      public void windowLostFocus(WindowEvent arg0) {
      }

   }
}
