package com.ceperman.pcsecrets.test;

import junit.framework.AssertionFailedError;

import com.ceperman.pcsecrets.DeviceSecretsCollection;
import com.ceperman.pcsecrets.SecretsCollection;

/**
 * Sync Test 6
 * Conditions:
 *   aaaa conflict
 *   bbbb conflict
 *   cccc deleted on PC
 *   dddd deleted on PC
 *   eeee deleted on device
 *   ffff updated on PC
 *   gggg updated on device
 *   iiii PCUpdatedAndDeviceDeleted
 *   jjjj PCDeletedAndDeviceUpdated
 *   
 * User action:
 *   aaaa copy to pc secret and mark as merged
 *   bbbb mark as merged
 *   iiii confirm deletion
 *   jjjj confirm deletion
 *   
 * Check results:
 *   aaaa updated on pc
 *   bbbb updated on device  
 *   cccc deleted on pc -> deleted on device
 *   dddd deleted on pc -> deleted on device
 *   eeee deleted on device -> deleted on pc
 *   ffff updated on device
 *   gggg updated on pc
 *   iiii deleted on pc
 *   jjjj deleted on pc -> deleted on device
 *     
 * @author Chris Wood
 */
public class SyncTest6 extends SyncTestBase {

  @Override
  protected void setupTestConditions() {
    makeUpdateConflict("aaaa");
    makeUpdateConflict("bbbb");
    makePCDeleted("cccc");
    makePCDeleted("dddd");
    makeDeviceDeleted("eeee");
    makePCUpdated("ffff");
    makeDeviceUpdated("gggg");
    makePCUpdatedAndDeviceDeleted("iiii");
    makePCDeletedAndDeviceUpdated("jjjj");
  }

  @Override
  protected void checkResults(DeviceSecretsCollection updatedPhoneSecrets, SecretsCollection updatedPCSecrets) {
    try {
      assertTrue(updatedPhoneSecrets.getSize() == 5);
      assertTrue(updatedPCSecrets.getSize() == 5);
    } catch (AssertionFailedError e) {
      System.out.println("F A I L E D!\n");
      System.out.println("phone secrets: expected 5, got " + updatedPhoneSecrets.getSize());
      System.out.println("pc secrets: expected 5, got " + updatedPCSecrets.getSize());
    }
  }
}
