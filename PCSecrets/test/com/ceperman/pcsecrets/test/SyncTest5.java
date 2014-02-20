package com.ceperman.pcsecrets.test;

import junit.framework.AssertionFailedError;

import com.ceperman.pcsecrets.DeviceSecretsCollection;
import com.ceperman.pcsecrets.SecretsCollection;

/**
 * Sync Test 5a
 * Conditions:
 *   iiii PCUpdatedAndDeviceDeleted
 *   
 * User action:
 *   iiii undelete
 *   
 * Check results:
 *   iiii updated on device
 *   1 device update
 *     
 * @author Chris Wood
 */
public class SyncTest5 extends SyncTestBase {

  @Override
  protected void setupTestConditions() {
    makePCUpdatedAndDeviceDeleted("iiii");
  }

  @Override
  protected void checkResults(DeviceSecretsCollection updatedPhoneSecrets, SecretsCollection updatedPCSecrets) {
    try {
      assertTrue(updatedPhoneSecrets.getSize() == 1);
      assertTrue(updatedPCSecrets.getSize() == 10);
    } catch (AssertionFailedError e) {
      System.out.println("F A I L E D!\n");
      System.out.println("phone secrets: expected 1, got " + updatedPhoneSecrets.getSize());
      System.out.println("pc secrets: expected 9, got " + updatedPCSecrets.getSize());
    }
  }
}
