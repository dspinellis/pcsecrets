package com.ceperman.pcsecrets.test;

import junit.framework.AssertionFailedError;

import com.ceperman.pcsecrets.DeviceSecretsCollection;
import com.ceperman.pcsecrets.SecretsCollection;

/**
 * Sync Test 4
 * Conditions:
 *   ffff updated on PC
 *   gggg updated on device
 *   
 * User action:
 *   none available, finish
 *   
 * Check results:
 *   ffff updated on device
 *   gggg updated on pc
 *   1 device update
 *     
 * @author Chris Wood
 */
public class SyncTest4 extends SyncTestBase {

  @Override
  protected void setupTestConditions() {
    makePCUpdated("ffff");
    makeDeviceUpdated("gggg");
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
