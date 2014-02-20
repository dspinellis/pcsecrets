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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractListModel;

/**
 * Represents a collection of secrets
 * 
 * @author Chris Wood
 */
public class SecretsCollection extends AbstractListModel<HostSecret> implements Iterable<HostSecret> {
//  private static Logger logger = Logger.getLogger(SecretsCollection.class.getName());

	/** serialVersionUID */
	private static final long serialVersionUID = -7697994255671452799L;
	protected List<HostSecret> secrets = Collections.synchronizedList(new ArrayList<HostSecret>());
	private boolean changed;
	
	/**
	 * Default constructor
	 */
	public SecretsCollection() {}
	
	/**
	 * Copy constructor (deep copy)
	 * @param other
	 */
	public SecretsCollection(SecretsCollection other) {
	  for (HostSecret secret : other.secrets) {
      this.secrets.add(new HostSecret(secret));
    }
    this.changed = other.changed;
	}

	/**
	 * Insert new element at correct point or update existing element
	 * 
	 * @param newSecret
	 * @return index of element added or updated
	 */
	public int addOrUpdate(HostSecret newSecret) {
		int index = 0;
		changed = true;
		if (secrets.isEmpty() || (newSecret.getDescription().compareToIgnoreCase(secrets.get(secrets.size()-1).getDescription()) > 0)) {
			secrets.add(newSecret); /* add to end if empty or if greater than last */
			fireIntervalAdded(this, secrets.size()-1, secrets.size()-1);
			return secrets.size()-1;
		} else {
			for (index = 0; index < secrets.size(); index++) {
				HostSecret secret = secrets.get(index);
				int rc = newSecret.getDescription().compareToIgnoreCase(secret.getDescription());
				if (rc == 0) { /* update if equals */
					secret.setUsername(newSecret.getUsername());
					secret.setPassword(newSecret.getPassword());
					secret.setEmail(newSecret.getEmail());
					secret.setNote(newSecret.getNote());
					secret.setTimestamp(newSecret.getTimestamp());
					fireContentsChanged(this, index, index);
					break;
				} else if (rc < 0) { /* insert if newSecret lower */
					secrets.add(index,newSecret);
					fireIntervalAdded(this, index, index);
					break;
				}
			}
		}
		return index;
	}
	
	/**
	 * Empty the collection
	 */
	public void clear() {
	  secrets.clear();
	}

  @Override
  public Iterator<HostSecret> iterator() {
    return secrets.iterator();
  }

	/**
	 * Check if collection contains key (description) provided
	 * 
	 * @param descr
	 * @return true if description matches existing secret, false otherwise
	 */
	public boolean contains(String descr) {
		return get(descr) != null;
	}

	/**
	 * Get the element corresponding to the key (description) provided
	 * 
	 * @param descr
	 * @return the requested element or null
	 */
	public HostSecret get(String descr) {
		for (Iterator<HostSecret> iterator = secrets.iterator(); iterator.hasNext();) {
			HostSecret secret = iterator.next();
			if (secret.getDescription().equalsIgnoreCase(descr)) {
				return secret;
			}
		}
		return null;
	}

	/**
	 * Remove by key
	 * 
	 * @param descr
	 */
	public void delete(String descr) {
		for (int i = 0; i < secrets.size(); i++) {
			HostSecret secret = secrets.get(i);
			if (secret.getDescription().equalsIgnoreCase(descr)) {
				secrets.remove(i);
				changed = true;
				fireIntervalRemoved(this, i, i);
				break;
			}
		}
	}
	
	/**
	 * Allow other classes involved in the model implementation to signal that
	 * a refresh is needed. If the start index is negative, the whole range is
	 * implied.
	 * 
	 * @param start 
	 * @param end 
	 */
	public void notifyChange(int start, int end) {
		if (start < 0) {
			fireContentsChanged(this, 0, secrets.size()-1);
		} else {
			fireContentsChanged(this, start, end);			
		}
	}
	
	/**
	 * Remove all elements from the collection and replace with the
	 * contents of the supplied collection.
	 * 
	 * @param secrets
	 */
	public void replaceSecrets(SecretsCollection secrets) {
		this.secrets.clear();
		this.secrets.addAll(secrets.secrets);
		changed = true;
	}

	@Override
	public HostSecret getElementAt(int index) {
		return secrets.get(index);
	}

	@Override
	public int getSize() {
		return secrets.size();
	}

	/**
	 * @return the changed
	 */
	public boolean isChanged() {
		return changed;
	}

	/**
	 * @param changed the changed to set
	 */
	public void setChanged(boolean changed) {
		this.changed = changed;
	}

}