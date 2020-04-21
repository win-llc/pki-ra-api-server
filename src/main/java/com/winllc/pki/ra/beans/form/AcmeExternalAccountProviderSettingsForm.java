package com.winllc.pki.ra.beans.form;

import com.winllc.acme.common.ExternalAccountProviderSettings;

import javax.validation.constraints.NotEmpty;
import java.util.Map;

public class AcmeExternalAccountProviderSettingsForm extends AcmeSettingsForm<ExternalAccountProviderSettings> {

    @NotEmpty
    private String name;
    private String baseUrl;
    private String accountVerificationUrl;
    private String accountValidationRulesUrl;
    private Map<String, String> additionalSettings;

    public AcmeExternalAccountProviderSettingsForm(ExternalAccountProviderSettings entity) {
        super(entity);
        this.name = entity.getName();
        this.baseUrl = entity.getBaseUrl();
        this.accountVerificationUrl = entity.getAccountVerificationUrl();
        this.accountValidationRulesUrl = entity.getAccountValidationRulesUrl();
        this.additionalSettings = entity.getAdditionalSettings();
    }

    private AcmeExternalAccountProviderSettingsForm() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getAccountVerificationUrl() {
        return accountVerificationUrl;
    }

    public void setAccountVerificationUrl(String accountVerificationUrl) {
        this.accountVerificationUrl = accountVerificationUrl;
    }

    public String getAccountValidationRulesUrl() {
        return accountValidationRulesUrl;
    }

    public void setAccountValidationRulesUrl(String accountValidationRulesUrl) {
        this.accountValidationRulesUrl = accountValidationRulesUrl;
    }

    public Map<String, String> getAdditionalSettings() {
        return additionalSettings;
    }

    public void setAdditionalSettings(Map<String, String> additionalSettings) {
        this.additionalSettings = additionalSettings;
    }
}
