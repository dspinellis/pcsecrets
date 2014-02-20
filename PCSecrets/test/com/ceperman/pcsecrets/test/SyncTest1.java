/**
 * 
 */
package com.ceperman.pcsecrets.test;

import junit.framework.AssertionFailedError;

import com.ceperman.pcsecrets.DeviceSecretsCollection;
import com.ceperman.pcsecrets.SecretsCollection;

/**
 * Sync Test 1
 * Conditions:
 *   aaaa conflict
 *   bbbb conflict
 *   
 * User action:
 *   aaaa copy to pc secret and mark as merged
 *   bbbb mark as merged
 *   
 * Check results:
 *   aaaa updated on pc
 *   bbbb updated on device
 *   1 device update
 *     
 * @author Chris Wood
 */
public class SyncTest1 extends SyncTestBase {

  @Override
  protected void setupTestConditions() {
    makeUpdateConflict("aaaa");
    makeUpdateConflict("bbbb");
  }

  @Override
  protected void checkResults(DeviceSecretsCollection updatedPhoneSecrets, SecretsCollection updatedPCSecrets) {
    try {
      assertTrue(updatedPhoneSecrets.getSize() == 1);
      assertTrue(updatedPCSecrets.getSize() == 10);
    } catch (AssertionFailedError e) {
      System.out.println("F A I L E D!\n");
      System.out.println("phone secrets: expected 1, got " + updatedPhoneSecrets.getSize());
      System.out.println("pc secrets: expected 10, got " + updatedPCSecrets.getSize());
    }
  }
  
}
