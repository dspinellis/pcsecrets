package com.ceperman.pcsecrets.test;

import junit.framework.AssertionFailedError;

import com.ceperman.pcsecrets.DeviceSecretsCollection;
import com.ceperman.pcsecrets.SecretsCollection;

/**
 * Sync Test 2
 * Conditions:
 *   cccc deleted on PC
 *   
 * User action:
 *   none available, finish
 *   
 * Check results:
 *   cccc deleted on pc -> deleted on device
 *   1 device update
 *     
 * @author Chris Wood
 */
public class SyncTest2 extends SyncTestBase {

  @Override
  protected void setupTestConditions() {
    makePCDeleted("cccc");
  }

  @Override
  protected void checkResults(DeviceSecretsCollection updatedPhoneSecrets, SecretsCollection updatedPCSecrets) {
    try {
      assertTrue(updatedPhoneSecrets.getSize() == 1);
      assertTrue(updatedPCSecrets.getSize() == 9);
    } catch (AssertionFailedError e) {
      System.out.println("F A I L E D!\n");
      System.out.println("phone secrets: expected 1, got " + updatedPhoneSecrets.getSize());
      System.out.println("pc secrets: expected 9, got " + updatedPCSecrets.getSize());
    }
  }
  
}
