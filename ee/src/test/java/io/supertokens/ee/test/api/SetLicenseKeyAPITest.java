package io.supertokens.ee.test.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.google.gson.JsonObject;

import io.supertokens.ProcessState.PROCESS_STATE;
import io.supertokens.ee.EEFeatureFlag;
import io.supertokens.ee.test.EETest;
import io.supertokens.ee.test.TestingProcessManager;
import io.supertokens.ee.test.Utils;
import io.supertokens.ee.test.TestingProcessManager.TestingProcess;
import io.supertokens.ee.test.httpRequest.HttpRequestForTesting;
import io.supertokens.ee.test.httpRequest.HttpResponseException;
import io.supertokens.featureflag.FeatureFlag;
import io.supertokens.featureflag.FeatureFlagTestContent;
import io.supertokens.pluginInterface.KeyValueInfo;
import io.supertokens.storageLayer.StorageLayer;
import io.supertokens.webserver.WebserverAPI;

public class SetLicenseKeyAPITest {
    @Rule
    public TestRule watchman = Utils.getOnFailure();

    @AfterClass
    public static void afterTesting() {
        Utils.afterTesting();
    }

    @Before
    public void beforeEach() {
        Utils.reset();
    }

    @Test
    public void testSettingBadInput() throws Exception {
        String[] args = { "../../" };

        TestingProcessManager.TestingProcess process = TestingProcessManager.start(args);
        Assert.assertNotNull(process.checkOrWaitForEvent(PROCESS_STATE.STARTED));

        // setting licenseKey as invalid type

        JsonObject requestBody = new JsonObject();

        requestBody.addProperty("licenseKey", 123);

        try {
            HttpRequestForTesting.sendJsonPUTRequest(process.getProcess(), "",
                    "http://localhost:3567/ee/license",
                    requestBody, 1000, 1000, null, WebserverAPI.getLatestCDIVersion(), "");
            throw new Exception("should never come here");
        } catch (HttpResponseException e) {
            assertTrue(e.statusCode == 400 && e.getMessage().equals(
                    "Http error. Status Code: 400. Message:" + " Field name 'licenseKey' is invalid in JSON input"));
        }

        process.kill();
        Assert.assertNotNull(process.checkOrWaitForEvent(PROCESS_STATE.STOPPED));
    }

    @Test
    public void testSettingLicenseKeyWhenEEFolderDoesNotExist() throws Exception {
        String[] args = { "../../" };

        TestingProcessManager.TestingProcess process = TestingProcessManager.start(args, false);

        FeatureFlagTestContent.getInstance(process.getProcess())
                .setKeyValue(FeatureFlagTestContent.EE_FOLDER_LOCATION, "random");
        process.startProcess();

        assertNotNull(process.checkOrWaitForEvent(PROCESS_STATE.STARTED));

        Assert.assertNull(FeatureFlag.getInstance(process.main).getEeFeatureFlagInstance());

        Assert.assertEquals(FeatureFlag.getInstance(process.getProcess()).getEnabledFeatures().length, 0);

        // set license key when ee folder does not exist
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("licenseKey", EETest.STATELESS_LICENSE_KEY_WITH_TEST_FEATURE_NO_EXP);

        JsonObject response = HttpRequestForTesting.sendJsonPUTRequest(process.getProcess(), "",
                "http://localhost:3567/ee/license",
                requestBody, 1000, 1000, null, WebserverAPI.getLatestCDIVersion(), "");
        assertEquals(1, response.entrySet().size());
        assertEquals("MISSING_EE_FOLDER_ERROR", response.get("status").getAsString());

        process.kill();
        Assert.assertNotNull(process.checkOrWaitForEvent(PROCESS_STATE.STOPPED));
    }

    @Test
    public void testSettingLicenseKeySuccessfully() throws Exception {
        String[] args = { "../../" };

        TestingProcessManager.TestingProcess process = TestingProcessManager.start(args);
        Assert.assertNotNull(process.checkOrWaitForEvent(PROCESS_STATE.STARTED));
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("licenseKey", EETest.STATELESS_LICENSE_KEY_WITH_TEST_FEATURE_NO_EXP);

        JsonObject response = HttpRequestForTesting.sendJsonPUTRequest(process.getProcess(), "",
                "http://localhost:3567/ee/license",
                requestBody, 1000, 1000, null, WebserverAPI.getLatestCDIVersion(), "");
        assertEquals(1, response.entrySet().size());
        assertEquals("OK", response.get("status").getAsString());

        // retrieve license key to check that it was correctly set
        String licenseKey = FeatureFlag.getInstance(process.getProcess()).getLicenseKey();
        assertEquals(EETest.STATELESS_LICENSE_KEY_WITH_TEST_FEATURE_NO_EXP, licenseKey);

        process.kill();
        Assert.assertNotNull(process.checkOrWaitForEvent(PROCESS_STATE.STOPPED));
    }

    @Test
    public void testCallingAPIToSyncLicenseKey() throws Exception {
        String[] args = { "../../" };

        TestingProcessManager.TestingProcess process = TestingProcessManager.start(args);
        Assert.assertNotNull(process.checkOrWaitForEvent(PROCESS_STATE.STARTED));

        // set licenseKey in db
        StorageLayer.getStorage(process.getProcess()).setKeyValue(EEFeatureFlag.LICENSE_KEY_IN_DB,
                new KeyValueInfo(EETest.STATELESS_LICENSE_KEY_WITH_TEST_FEATURE_NO_EXP));

        // check that licenseKey is not present
        assertFalse(FeatureFlag.getInstance(process.getProcess()).getEeFeatureFlagInstance().getIsLicenseKeyPresent());

        JsonObject response = HttpRequestForTesting.sendJsonPUTRequest(process.getProcess(), "",
                "http://localhost:3567/ee/license",
                new JsonObject(), 1000, 1000, null, WebserverAPI.getLatestCDIVersion(), "");
        assertEquals(1, response.entrySet().size());
        assertEquals("OK", response.get("status").getAsString());

        // check that licenseKey is present
        assertTrue(FeatureFlag.getInstance(process.getProcess()).getEeFeatureFlagInstance().getIsLicenseKeyPresent());

        process.kill();
        Assert.assertNotNull(process.checkOrWaitForEvent(PROCESS_STATE.STOPPED));
    }

    @Test
    public void testSettingInvalidLicenseKey() throws Exception {
        String[] args = { "../../" };

        TestingProcessManager.TestingProcess process = TestingProcessManager.start(args);
        Assert.assertNotNull(process.checkOrWaitForEvent(PROCESS_STATE.STARTED));

        // setting licenseKey as invalid string
        JsonObject requestBody = new JsonObject();

        requestBody.addProperty("licenseKey", "invalidKey123");

        JsonObject response = HttpRequestForTesting.sendJsonPUTRequest(process.getProcess(), "",
                "http://localhost:3567/ee/license",
                requestBody, 1000, 1000, null, WebserverAPI.getLatestCDIVersion(), "");

        assertEquals(1, response.entrySet().size());
        assertEquals("INVALID_LICENSE_KEY_ERROR", response.get("status").getAsString());

        process.kill();
        Assert.assertNotNull(process.checkOrWaitForEvent(PROCESS_STATE.STOPPED));
    }
}