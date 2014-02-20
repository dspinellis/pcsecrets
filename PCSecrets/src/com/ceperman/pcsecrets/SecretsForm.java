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

import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 * INterface for secrets form.
 * 
 * @author Chris Wood
 */
public interface SecretsForm {
	/**
	 * @return the fieldDescr
	 */
	public JTextField getFieldDescr();

	/**
	 * @return the fieldId
	 */
	public JTextField getFieldId();

	/**
	 * @return the fieldPswd
	 */
	public JTextField getFieldPswd();

	/**
	 * @return the fieldEmail
	 */
	public JTextField getFieldEmail();

	/**
	 * @return the areaNote
	 */
	public JTextArea getAreaNote();

	/**
	 * @return the fieldDate
	 */
	public JLabel getFieldDate();
}
