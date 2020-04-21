package com.winllc.pki.ra.beans.form;

import com.winllc.acme.common.AdditionalSetting;
import com.winllc.acme.common.CertificateAuthoritySettings;

import javax.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;

public class AcmeCertificateAuthoritySettingsForm extends AcmeSettingsForm<CertificateAuthoritySettings> {

    private String type;
    @NotEmpty
    private String name;
    private String issuerDn;
    private String mapsToExternalAccountProviderName;
    private String mapsToCaConnectionName;
    private String baseUrl;
    private List<AdditionalSetting> additionalSettings;

    public AcmeCertificateAuthoritySettingsForm(CertificateAuthoritySettings entity) {
        super(entity);
        this.type = entity.getType();
        this.name = entity.getName();
        this.issuerDn = entity.getIssuerDn();
        this.mapsToExternalAccountProviderName = entity.getMapsToExternalAccountProviderName();
        this.mapsToCaConnectionName = entity.getMapsToCaConnectionName();
        this.baseUrl = entity.getBaseUrl();
        this.additionalSettings = entity.getAdditionalSettings();
    }

    private AcmeCertificateAuthoritySettingsForm() {
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIssuerDn() {
        return issuerDn;
    }

    public void setIssuerDn(String issuerDn) {
        this.issuerDn = issuerDn;
    }

    public String getMapsToExternalAccountProviderName() {
        return mapsToExternalAccountProviderName;
    }

    public void setMapsToExternalAccountProviderName(String mapsToExternalAccountProviderName) {
        this.mapsToExternalAccountProviderName = mapsToExternalAccountProviderName;
    }

    public String getMapsToCaConnectionName() {
        return mapsToCaConnectionName;
    }

    public void setMapsToCaConnectionName(String mapsToCaConnectionName) {
        this.mapsToCaConnectionName = mapsToCaConnectionName;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public List<AdditionalSetting> getAdditionalSettings() {
        return additionalSettings;
    }

    public void setAdditionalSettings(List<AdditionalSetting> additionalSettings) {
        this.additionalSettings = additionalSettings;
    }
}
