/**
 * 
 */
package com.ceperman.pcsecrets.test;

import junit.framework.TestCase;

import com.ceperman.pcsecrets.DatedSecretsCollection;
import com.ceperman.pcsecrets.HostSecret;
import com.ceperman.pcsecrets.SyncDialog;

/**
 * Test secret categorisation
 * @author Chris Wood
 */
public class SecretCategorisationTest extends TestCase {
	
	private final DatedSecretsCollection pcSecrets = new DatedSecretsCollection();
	private final DatedSecretsCollection phoneSecrets = new DatedSecretsCollection();
	private final long baseTime = 10000L;
	
	private static final int PC_VALUE = 0;
	private static final int PHONE_VALUE = 1;
	private static final int EQUALS_VALUE = 2;
	private static final int CONFLICT_VALUE = 3;
  private static final int DELETED_ON_DEVICE = 4;
  private static final int DELETED_ON_PC = 5;
	
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		HostSecret secret;
		
		/* test data */
		
		/* aaaa - pc only */
		pcSecrets.addOrUpdate(createSecret("aaaa"));
		/* bbbb - phone only */
		phoneSecrets.addOrUpdate(createSecret("bbbb"));
		/* cccc - identical, same timestamp */
		pcSecrets.addOrUpdate(createSecret("cccc"));
		phoneSecrets.addOrUpdate(createSecret("cccc"));
		/* dddd - identical, pc earlier */
		pcSecrets.addOrUpdate(createSecret("dddd"));
		secret = createSecret("dddd");
		secret.setTimestamp(baseTime + 500);
		phoneSecrets.addOrUpdate(secret);
		/* eeee - identical, phone earlier */
		secret = createSecret("eeee");
		secret.setTimestamp(baseTime + 500);
		pcSecrets.addOrUpdate(secret);
		phoneSecrets.addOrUpdate(createSecret("eeee"));
		/* ffff - different, pc earlier */
		pcSecrets.addOrUpdate(createSecret("ffff"));
		secret = createSecret("ffff");
		secret.setPassword("password");
		secret.setTimestamp(baseTime + 500);
		phoneSecrets.addOrUpdate(secret);
		/* gggg - different, phone earlier */
		secret = createSecret("gggg");
		secret.setPassword("password");
		secret.setTimestamp(baseTime + 500);
		pcSecrets.addOrUpdate(secret);
		phoneSecrets.addOrUpdate(createSecret("gggg"));
		
		/* hhhh - deleted on pc */
		secret = createSecret("hhhh");
    secret.setPassword("password");
    secret.setTimestamp(baseTime + 1000);
    pcSecrets.addOrUpdate(secret);
    phoneSecrets.addOrUpdate(createSecret("hhhh"));
    /* iiii - deleted on device */
    secret = createSecret("iiii");
    secret.setPassword("password");
    secret.setTimestamp(baseTime + 1000);
    phoneSecrets.addOrUpdate(secret);
    pcSecrets.addOrUpdate(createSecret("iiii"));
	}

	/**
	 * Test the categoriseSecret() method
	 */
	public final void testSecretCategorisation() {
	  SyncDialog.ListSecret listSecret = null;
		/* Case 1: pc only
		 * sync timestamps same (should not be important)
		 * Expected result: PC_VALUE
		 */
		pcSecrets.setLastSyncTimestamp(baseTime);
		phoneSecrets.setLastSyncTimestamp(baseTime);
		listSecret = new SyncDialog.ListSecret("aaaa");
		assertTrue(SyncDialog.categoriseSecret(listSecret, pcSecrets, phoneSecrets) == PC_VALUE);
		
		/* Case 2: phone only
		 * sync timestamps same (should not be important)
		 * Expected result: PHONE_VALUE
		 */
		pcSecrets.setLastSyncTimestamp(baseTime);
		phoneSecrets.setLastSyncTimestamp(baseTime);
    listSecret = new SyncDialog.ListSecret("bbbb");
		assertTrue(SyncDialog.categoriseSecret(listSecret, pcSecrets, phoneSecrets) == PHONE_VALUE);
		
		/* Case 3: identical, same timestamp
		 * sync timestamps same (should not be important)
		 * Expected result: EQUALS_VALUE
		 */
		pcSecrets.setLastSyncTimestamp(baseTime);
		phoneSecrets.setLastSyncTimestamp(baseTime);
		listSecret = new SyncDialog.ListSecret("cccc");
		assertTrue(SyncDialog.categoriseSecret(listSecret, pcSecrets, phoneSecrets) == EQUALS_VALUE);
		
		/* Case 4: identical, same timestamp
		 * sync timestamps different (should not be important)
		 * Expected result: EQUALS_VALUE
		 */
		pcSecrets.setLastSyncTimestamp(baseTime);
		phoneSecrets.setLastSyncTimestamp(baseTime + 1000);
    listSecret = new SyncDialog.ListSecret("cccc");
		assertTrue(SyncDialog.categoriseSecret(listSecret, pcSecrets, phoneSecrets) == EQUALS_VALUE);
		
		/* Case 5: identical, pc earlier (should not be important)
		 * sync timestamps same (should not be important)
		 * Expected result: EQUALS_VALUE
		 */
		pcSecrets.setLastSyncTimestamp(baseTime);
		phoneSecrets.setLastSyncTimestamp(baseTime);
    listSecret = new SyncDialog.ListSecret("dddd");
		assertTrue(SyncDialog.categoriseSecret(listSecret, pcSecrets, phoneSecrets) == EQUALS_VALUE);
		
		/* Case 6: identical, pc earlier (should not be important)
		 * sync timestamps different (should not be important)
		 * Expected result: EQUALS_VALUE
		 */
		pcSecrets.setLastSyncTimestamp(baseTime);
		phoneSecrets.setLastSyncTimestamp(baseTime + 1000);
    listSecret = new SyncDialog.ListSecret("dddd");
		assertTrue(SyncDialog.categoriseSecret(listSecret, pcSecrets, phoneSecrets) == EQUALS_VALUE);
		
		/* Case 7: identical, phone earlier (should not be important)
		 * sync timestamps same (should not be important)
		 * Expected result: EQUALS_VALUE
		 */
		pcSecrets.setLastSyncTimestamp(baseTime);
		phoneSecrets.setLastSyncTimestamp(baseTime);
    listSecret = new SyncDialog.ListSecret("eeee");
		assertTrue(SyncDialog.categoriseSecret(listSecret, pcSecrets, phoneSecrets) == EQUALS_VALUE);
		
		/* Case 8: identical, phone earlier (should not be important)
		 * sync timestamps different (should not be important)
		 * Expected result: EQUALS_VALUE
		 */
		pcSecrets.setLastSyncTimestamp(baseTime);
		phoneSecrets.setLastSyncTimestamp(baseTime + 1000);
    listSecret = new SyncDialog.ListSecret("eeee");
		assertTrue(SyncDialog.categoriseSecret(listSecret, pcSecrets, phoneSecrets) == EQUALS_VALUE);
		
		/* Case 9: values different, pc timestamp < phone timestamp
		 * sync timestamp < pc timestamp < phone timestamp
		 * Expected result: CONFLICT_VALUE
		 */
		pcSecrets.setLastSyncTimestamp(baseTime - 100);
		phoneSecrets.setLastSyncTimestamp(baseTime - 100);
    listSecret = new SyncDialog.ListSecret("ffff");
		assertTrue(SyncDialog.categoriseSecret(listSecret, pcSecrets, phoneSecrets) == CONFLICT_VALUE);
		
		/* Case 10: values different, pc timestamp < phone timestamp
		 * pc timestamp < sync timestamp < phone timestamp
		 * Expected result: PHONE_VALUE
		 */
		pcSecrets.setLastSyncTimestamp(baseTime + 100);
		phoneSecrets.setLastSyncTimestamp(baseTime + 100);
    listSecret = new SyncDialog.ListSecret("ffff");
		assertTrue(SyncDialog.categoriseSecret(listSecret, pcSecrets, phoneSecrets) == PHONE_VALUE);
		
		/* Case 11: values different, pc timestamp < phone timestamp
		 * pc timestamp < phone timestamp < sync timestamp (maybe not possible)
		 * Expected result: CONFLICT_VALUE
		 */
		pcSecrets.setLastSyncTimestamp(baseTime + 1000);
		phoneSecrets.setLastSyncTimestamp(baseTime + 1000);
    listSecret = new SyncDialog.ListSecret("ffff");
		assertTrue(SyncDialog.categoriseSecret(listSecret, pcSecrets, phoneSecrets) == CONFLICT_VALUE);
		
		/* Case 12: values different, phone timestamp < pc timestamp
		 * sync timestamp < phone timestamp < pc timestamp
		 * Expected result: CONFLICT_VALUE
		 */
		pcSecrets.setLastSyncTimestamp(baseTime - 100);
		phoneSecrets.setLastSyncTimestamp(baseTime - 100);
		assertTrue(SyncDialog.categoriseSecret(listSecret, pcSecrets, phoneSecrets) == CONFLICT_VALUE);
		
		/* Case 13: values different, phone timestamp < pc timestamp
		 * phone timestamp < sync timestamp < pc timestamp
		 * Expected result: PC_VALUE
		 */
		pcSecrets.setLastSyncTimestamp(baseTime + 100);
		phoneSecrets.setLastSyncTimestamp(baseTime + 100);
    listSecret = new SyncDialog.ListSecret("gggg");
		assertTrue(SyncDialog.categoriseSecret(listSecret, pcSecrets, phoneSecrets) == PC_VALUE);
		
		/* Case 14: values different, phone timestamp < pc timestamp
		 * phone timestamp < pc timestamp < sync timestamp (maybe not possible)
		 * Expected result: CONFLICT_VALUE
		 */
		pcSecrets.setLastSyncTimestamp(baseTime + 1000);
		phoneSecrets.setLastSyncTimestamp(baseTime + 1000);
    listSecret = new SyncDialog.ListSecret("gggg");
		assertTrue(SyncDialog.categoriseSecret(listSecret, pcSecrets, phoneSecrets) == CONFLICT_VALUE);
    
    /* Case 15: deleted on pc, pc deletion timestamp < phone timestamp
     * pc deletion timestamp < sync timestamp< phone timestamp
     * Expected result: DELETED_ON_PC
     */
    pcSecrets.setLastSyncTimestamp(baseTime + 500);
    phoneSecrets.setLastSyncTimestamp(baseTime + 500);
    listSecret = new SyncDialog.ListSecret("hhhh");
    listSecret.setDeletedOnPC(true);
    listSecret.setDeletedTimestamp(pcSecrets.get("hhhh").getTimestamp());
    assertTrue(SyncDialog.categoriseSecret(listSecret, pcSecrets, phoneSecrets) == DELETED_ON_PC);
    
    /* Case 16: deleted on device, phone deletion timestamp < pc timestamp
     * pc deletion timestamp < sync timestamp< phone timestamp
     * Expected result: DELETED_ON_DEVICE
     */
    pcSecrets.setLastSyncTimestamp(baseTime + 500);
    phoneSecrets.setLastSyncTimestamp(baseTime + 500);
    listSecret = new SyncDialog.ListSecret("iiii");
    listSecret.setDeletedOnDevice(true);
    listSecret.setDeletedTimestamp(pcSecrets.get("iiii").getTimestamp());
    assertTrue(SyncDialog.categoriseSecret(listSecret, pcSecrets, phoneSecrets) == DELETED_ON_DEVICE);
	}
	
	private HostSecret createSecret(String id) {
		HostSecret secret = new HostSecret(id, "test", "test", "test", "test");
		secret.setTimestamp(baseTime);
		return secret;
	}
	
}
