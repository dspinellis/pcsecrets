package com.ceperman.pcsecrets.test;

import junit.framework.AssertionFailedError;

import com.ceperman.pcsecrets.DeviceSecretsCollection;
import com.ceperman.pcsecrets.SecretsCollection;

/**
 * Sync Test 5a
 * Conditions:
 *   iiii PCUpdatedAndDeviceDeleted
 *   jjjj PCDeletedAndDeviceUpdated
 *   
 * User action:
 *   iiii confirm deletion
 *   jjjj confirm deletion
 *   
 * Check results:
 *   iiii deleted on pc
 *   jjjj deleted on pc -> deleted on device
 *   1 device update
 *     
 * @author Chris Wood
 */
public class SyncTest5a extends SyncTestBase {

  @Override
  protected void setupTestConditions() {
    makePCUpdatedAndDeviceDeleted("iiii");
    makePCDeletedAndDeviceUpdated("jjjj");
  }

  @Override
  protected void checkResults(DeviceSecretsCollection updatedPhoneSecrets, SecretsCollection updatedPCSecrets) {
    try {
      assertTrue(updatedPhoneSecrets.getSize() == 1);
      assertTrue(updatedPCSecrets.getSize() == 8);
    } catch (AssertionFailedError e) {
      System.out.println("F A I L E D!\n");
      System.out.println("phone secrets: expected 1, got " + updatedPhoneSecrets.getSize());
      System.out.println("pc secrets: expected 8, got " + updatedPCSecrets.getSize());
    }
  }
}
