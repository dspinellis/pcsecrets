package com.ceperman.pcsecrets.test;

import junit.framework.AssertionFailedError;

import com.ceperman.pcsecrets.DeviceSecretsCollection;
import com.ceperman.pcsecrets.SecretsCollection;

/**
 * Sync Test 3
 * Conditions:
 *   dddd deleted on PC
 *   eeee deleted on device
 *   
 * User action:
 *   none available, finish
 *   
 * Check results:
 *   dddd deleted on pc -> deleted on device
 *   eeee deleted on device -> deleted on pc
 *   1 device update
 *     
 * @author Chris Wood
 */
public class SyncTest3 extends SyncTestBase {

  @Override
  protected void setupTestConditions() {
    makePCDeleted("dddd");
    makeDeviceDeleted("eeee");
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