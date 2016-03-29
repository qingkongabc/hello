//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//


import com.atlassian.extras.common.log.Logger;
import com.atlassian.extras.common.log.Logger.Log;
import com.atlassian.license.License;
import com.atlassian.license.LicenseConfiguration;
import com.atlassian.license.LicenseException;
import com.atlassian.license.LicensePair;
import com.atlassian.license.LicenseRegistry;
import com.atlassian.license.LicenseType;
import com.atlassian.license.LicenseTypeStore;
import com.atlassian.license.LicenseUtils;
import com.atlassian.license.decoder.LicenseDecoder;
import java.util.HashMap;
import java.util.Map;

public class LicenseManager {
    private static final Log log = Logger.getInstance(LicenseManager.class);
    Map licenseList = new HashMap();
    Map licenseConfigurations = new HashMap();
    private static LicenseManager licenseManager;

    public LicenseManager() {
    }

    public static LicenseManager getInstance() {
        if(licenseManager == null) {
            licenseManager = new LicenseManager();
        }

        return licenseManager;
    }

    public void addLicenseConfiguration(String applicationName, LicenseTypeStore licenseTypeStore, LicenseRegistry licenseRegistry) {
        LicenseConfiguration licenseConfiguration = new LicenseConfiguration(licenseRegistry, licenseTypeStore);
        this.licenseConfigurations.put(applicationName, licenseConfiguration);
    }

    public LicenseRegistry getLicenseRegistry(String applicationName) {
        return this.getLicenseConfiguration(applicationName).getLicenseRegistry();
    }

    public LicenseTypeStore getLicenseTypeStore(String applicationName) {
        return this.getLicenseConfiguration(applicationName).getLicenseTypeStore();
    }

    private LicenseConfiguration getLicenseConfiguration(String applicationName) {
        LicenseConfiguration licenseConfiguration = (LicenseConfiguration)this.licenseConfigurations.get(applicationName);
        if(licenseConfiguration == null) {
            throw new RuntimeException("No LicenseConfiguration found for key " + applicationName);
        } else {
            return licenseConfiguration;
        }
    }

    public LicenseTypeStore lookupLicenseTypeStore(String applicationName) {
        LicenseConfiguration licenseConfiguration = (LicenseConfiguration)this.licenseConfigurations.get(applicationName);
        return licenseConfiguration == null?null:licenseConfiguration.getLicenseTypeStore();
    }

    public boolean hasValidLicense(String licenseKey) {
        return true;
    }

    public License getLicense(String applicationName) {
        if(this.licenseList.isEmpty() || !this.licenseList.containsKey(applicationName)) {
            try {
                License e = null;
                LicenseConfiguration licenseConfiguration = (LicenseConfiguration)this.licenseConfigurations.get(applicationName);
                if(licenseConfiguration == null) {
                    log.error("There is no License Configuration defined for the application " + applicationName + ".");
                    return null;
                }

                LicenseRegistry licenseRegistry = licenseConfiguration.getLicenseRegistry();
                String licenseStr = licenseRegistry.getLicenseMessage();
                String hash = licenseRegistry.getLicenseHash();
                if(licenseStr == null || hash == null) {
                    log.info("There is no license string or hash defined for the application " + applicationName + ".");
                    return null;
                }

                LicensePair pair = null;

                try {
                    pair = new LicensePair(licenseStr, hash);
                } catch (LicenseException var9) {
                    log.error("Could not build a license pair", var9);
                    return null;
                }

                e = LicenseDecoder.getLicense(pair, applicationName);
                this.licenseList.put(applicationName, e);
            } catch (Exception var10) {
                log.error("Exception getting license: " + var10, var10);
            }
        }

        return (License)this.licenseList.get(applicationName);
    }

    public License setLicense(String license, String applicationName) {
        LicensePair pair = null;

        try {
            pair = new LicensePair(license);
            License e = LicenseDecoder.getLicense(pair, applicationName);
            if(LicenseDecoder.isValid(pair, applicationName)) {
                this.setLicense(pair, applicationName);
            }

            return e;
        } catch (Exception var5) {
            log.warn("Attempt to set invalid license. Ensure that you are calling setLicense(license, appName) - not (appName, license)", var5);
            return null;
        }
    }

    public void setLicense(LicensePair pair, String applicationName) throws LicenseException {
        if(pair != null) {
            this.licenseList.remove(applicationName);
            LicenseConfiguration licenseConfiguration = (LicenseConfiguration)this.licenseConfigurations.get(applicationName);
            LicenseRegistry licenseRegistry = licenseConfiguration.getLicenseRegistry();
            licenseRegistry.setLicenseMessage(LicenseUtils.getString(pair.getLicense()));
            licenseRegistry.setLicenseHash(LicenseUtils.getString(pair.getHash()));
        }

    }

    public LicensePair getLicensePair(String applicationName) {
        try {
            LicenseConfiguration e = (LicenseConfiguration)this.licenseConfigurations.get(applicationName);
            LicenseRegistry licenseRegistry = e.getLicenseRegistry();
            return new LicensePair(licenseRegistry.getLicenseMessage(), licenseRegistry.getLicenseHash());
        } catch (LicenseException var4) {
            log.error("Couldn\'t get the LicensePair ...", var4);
            return null;
        }
    }

    public LicenseType getLicenseType(String applicationName, String licenseTypeString) throws LicenseException {
        LicenseConfiguration licenseConfiguration = (LicenseConfiguration)this.licenseConfigurations.get(applicationName);
        return licenseConfiguration.getLicenseTypeStore().getLicenseType(licenseTypeString);
    }

    public LicenseType getLicenseType(String applicationName, int licenseTypeCode) throws LicenseException {
        LicenseConfiguration licenseConfiguration = (LicenseConfiguration)this.licenseConfigurations.get(applicationName);
        return licenseConfiguration.getLicenseTypeStore().getLicenseType(licenseTypeCode);
    }

    public void reset() {
        this.licenseConfigurations.clear();
        this.licenseList.clear();
        licenseManager = null;
    }

    public void clearLicenseConfigurations() {
        this.licenseConfigurations.clear();
    }

    public void removeLicense(String applicationName) {
        this.licenseList.remove(applicationName);
    }
}
