/**
 * 
 */
package com.ceperman.pcsecrets.test;

import java.beans.PropertyChangeEvent;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import junit.framework.TestCase;

import com.ceperman.pcsecrets.DatedSecretsCollection;
import com.ceperman.pcsecrets.DeviceSecretsCollection;
import com.ceperman.pcsecrets.HostSecret;
import com.ceperman.pcsecrets.PropertyChangeWindow;
import com.ceperman.pcsecrets.SecretsCollection;
import com.ceperman.pcsecrets.SyncDevice;
import com.ceperman.pcsecrets.SyncDialog;

/**
 * Test secret categorisation in SyncDialog
 * @author Chris Wood
 */
public abstract class SyncTestBase extends TestCase {
  private static Logger logger = Logger.getLogger(SyncTestBase.class.getName());
  
  protected final static String SYNC = "sync";
  protected final static String COMPLETE = "complete";
  
  protected SyncDialog syncDialog;
	
  protected DatedSecretsCollection pcSecrets = new DatedSecretsCollection();
  protected DeviceSecretsCollection deviceSecrets = new DeviceSecretsCollection();
  protected SyncDevice pcDeletions = new SyncDevice("PCSecrets");
  
  protected final long syncTime = 10000L;
  protected final long baseTime = 1000L;
	
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		setupLogging();		
		createSecrets();
		pcDeletions.setDisplayName("JUnitTest device");
		pcDeletions.setLastIP("192.168.0.99");
    pcDeletions.setSyncTimestamp(syncTime);
		
		setupTestConditions();
	}

	/**
	 * Test the dialog
	 */
	public final void testDialog() {
	  @SuppressWarnings("serial")
    PropertyChangeWindow parent = new PropertyChangeWindow() {

      @Override
      public void propertyChange(PropertyChangeEvent event) {
        if (event.getPropertyName().equals(SYNC)) {
          if (event.getNewValue().equals(COMPLETE)) { // sync dialog complete
            logger.log(Level.INFO, "Sync complete signalled");
            /* generate updates for the phone, and apply phone updates to the PC - order is important */
            DeviceSecretsCollection updatesForPhone = syncDialog.getChangedPhoneSecretsCollection();
            syncDialog.applyPhoneUpdatesToPC();
            checkResults(updatesForPhone, syncDialog.getUpdatedPCSecrets());
            syncDialog.dispose();
          }
        }
        
      }};
    syncDialog = new SyncDialog(parent, pcSecrets, deviceSecrets, pcDeletions);
    logger.log(Level.INFO, "Launching dialog");
    syncDialog.setVisible(true);
	}
	
	/**
	 * Provided by real test class
	 */
	protected abstract void setupTestConditions();
  
  /**
   * Check the test class results
   */
  protected abstract void checkResults(DeviceSecretsCollection updatedPhoneSecrets, SecretsCollection updatedPCSecrets);
	
	/**
   * PC timestamp set to later than synctime
   */
	protected void makePCUpdated(String id) {
	  pcSecrets.get(id).setTimestamp(syncTime + baseTime);
	  pcSecrets.get(id).setNote("pc");
	}
  
	/**
   * Device timestamp set to later than synctime
   */
  protected void makeDeviceUpdated(String id) {
    deviceSecrets.get(id).setTimestamp(syncTime + baseTime);
    deviceSecrets.get(id).setNote("device");
  }
  
  /**
   * Remove PC secret from pc collection
   * Add to SyncDevice
   */
  protected void makePCDeleted(String id) {
    pcSecrets.delete(id);
    pcDeletions.addSecret(id, syncTime + baseTime);
  }
  
  /**
   * Set "deleted" in device secret
   */
  protected void makeDeviceDeleted(String id) {
    deviceSecrets.get(id).setDeleted(true);
  }
  
  /**
   * Both timestamps set to later than synctime
   */
  protected void makeUpdateConflict(String id) {
    makePCUpdated(id);
    makeDeviceUpdated(id);
  }
  
  protected void makePCUpdatedAndDeviceDeleted(String id) {
    makePCUpdated(id);
    makeDeviceDeleted(id);
  }
  
  protected void makePCDeletedAndDeviceUpdated(String id) {
    makePCDeleted(id);
    makeDeviceUpdated(id);
  }
  
  protected void makePCDeletedAndDeviceDeleted(String id) {
    makePCDeleted(id);
    makeDeviceDeleted(id);
  }
	
	/*
   * Setup logging level to FINE
   */
  protected void setupLogging() {
    Logger myRootLogger = Logger.getLogger("com.ceperman");
    myRootLogger.setLevel(Level.FINE);
    myRootLogger.setUseParentHandlers(false);
    LogManager.getLogManager().addLogger(myRootLogger);
    Handler handler = null;
    try {
      handler = new java.util.logging.FileHandler();
      handler.setLevel(Level.FINE);
      myRootLogger.addHandler(handler);
      handler = new java.util.logging.ConsoleHandler();
      handler.setLevel(Level.FINE);
      myRootLogger.addHandler(handler);
      /* Is this the correct level for this log message ?? */
      logger.log(Level.INFO, "Log level set to FINE");
    } catch (Exception e) {
      logger.log(Level.WARNING, "Problem setting log level: " + e.getMessage());
    }
  }
  
  protected HostSecret createSecret(String id) {
    HostSecret secret = new HostSecret(id, "test", "test", "test", "test");
    secret.setTimestamp(baseTime);
    return secret;
  }
  
  protected void createSecrets() {
    logger.log(Level.INFO, "Creating secrets");
    /* pc secrets */
    pcSecrets.addOrUpdate(createSecret("aaaa"));
    pcSecrets.addOrUpdate(createSecret("bbbb"));
    pcSecrets.addOrUpdate(createSecret("cccc"));
    pcSecrets.addOrUpdate(createSecret("dddd"));
    pcSecrets.addOrUpdate(createSecret("eeee"));
    pcSecrets.addOrUpdate(createSecret("ffff"));
    pcSecrets.addOrUpdate(createSecret("gggg"));
    pcSecrets.addOrUpdate(createSecret("hhhh"));
    pcSecrets.addOrUpdate(createSecret("iiii"));
    pcSecrets.addOrUpdate(createSecret("jjjj"));
    /* device secrets */
    deviceSecrets.addOrUpdate(createSecret("aaaa"));
    deviceSecrets.addOrUpdate(createSecret("bbbb"));
    deviceSecrets.addOrUpdate(createSecret("cccc"));
    deviceSecrets.addOrUpdate(createSecret("dddd"));
    deviceSecrets.addOrUpdate(createSecret("eeee"));
    deviceSecrets.addOrUpdate(createSecret("ffff"));
    deviceSecrets.addOrUpdate(createSecret("gggg"));
    deviceSecrets.addOrUpdate(createSecret("hhhh"));
    deviceSecrets.addOrUpdate(createSecret("iiii"));
    deviceSecrets.addOrUpdate(createSecret("jjjj"));
    
    logger.log(Level.INFO, "pcSecrets " + pcSecrets.getSize() + 
            ", deviceSecrets " + deviceSecrets.getSize());
  }
	
}
