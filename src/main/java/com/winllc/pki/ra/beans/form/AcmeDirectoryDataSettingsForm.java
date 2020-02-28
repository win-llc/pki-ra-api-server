package com.winllc.pki.ra.beans.form;

import com.winllc.acme.common.DirectoryDataSettings;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class AcmeDirectoryDataSettingsForm extends AcmeSettingsForm<DirectoryDataSettings> {

    private String name;
    private boolean allowPreAuthorization;
    private String mapsToCertificateAuthorityName;
    private String externalAccountProviderName;
    private String metaWebsite;
    private List<String> metaCaaIdentities;
    private boolean metaExternalAccountRequired;

    public AcmeDirectoryDataSettingsForm(DirectoryDataSettings entity) {
        super(entity);
        this.name = entity.getName();
        this.allowPreAuthorization = entity.isAllowPreAuthorization();
        this.mapsToCertificateAuthorityName = entity.getMapsToCertificateAuthorityName();
        this.externalAccountProviderName = entity.getExternalAccountProviderName();
        this.metaWebsite = entity.getMetaWebsite();
        this.metaCaaIdentities = Arrays.asList(entity.getMetaCaaIdentities());
        this.metaExternalAccountRequired = entity.isMetaExternalAccountRequired();
    }

    private AcmeDirectoryDataSettingsForm() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isAllowPreAuthorization() {
        return allowPreAuthorization;
    }

    public void setAllowPreAuthorization(boolean allowPreAuthorization) {
        this.allowPreAuthorization = allowPreAuthorization;
    }

    public String getMapsToCertificateAuthorityName() {
        return mapsToCertificateAuthorityName;
    }

    public void setMapsToCertificateAuthorityName(String mapsToCertificateAuthorityName) {
        this.mapsToCertificateAuthorityName = mapsToCertificateAuthorityName;
    }

    public String getExternalAccountProviderName() {
        return externalAccountProviderName;
    }

    public void setExternalAccountProviderName(String externalAccountProviderName) {
        this.externalAccountProviderName = externalAccountProviderName;
    }

    public String getMetaWebsite() {
        return metaWebsite;
    }

    public void setMetaWebsite(String metaWebsite) {
        this.metaWebsite = metaWebsite;
    }

    public List<String> getMetaCaaIdentities() {
        return metaCaaIdentities;
    }

    public void setMetaCaaIdentities(List<String> metaCaaIdentities) {
        this.metaCaaIdentities = metaCaaIdentities;
    }

    public boolean isMetaExternalAccountRequired() {
        return metaExternalAccountRequired;
    }

    public void setMetaExternalAccountRequired(boolean metaExternalAccountRequired) {
        this.metaExternalAccountRequired = metaExternalAccountRequired;
    }
}
