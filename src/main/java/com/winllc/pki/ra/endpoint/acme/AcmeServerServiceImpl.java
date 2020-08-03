package com.winllc.pki.ra.endpoint.acme;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.winllc.acme.common.AcmeCertAuthorityType;
import com.winllc.acme.common.CertificateAuthoritySettings;
import com.winllc.acme.common.DirectoryDataSettings;
import com.winllc.acme.common.ExternalAccountProviderSettings;
import com.winllc.pki.ra.exception.AcmeConnectionException;

import java.io.IOException;
import java.util.List;

public class AcmeServerServiceImpl implements AcmeServerService {

    private static final String DIRECTORY_DATA_BASE = "directoryData";
    private static final String CERT_AUTHORITY_BASE = "certAuthority";
    private static final String EXTERNAL_ACCOUNT_PROVIDER_BASE = "externalAccountProvider";

    private final AcmeServerConnection connection;

    public AcmeServerServiceImpl(AcmeServerConnection connection) {
        this.connection = connection;
    }

    public String getName() {
        return this.connection.getConnectionInfo().getName();
    }

    /*
    Directory settings CRUD
     */

    public DirectoryDataSettings saveDirectorySettings(DirectoryDataSettings directoryDataSettings) throws AcmeConnectionException, IOException {
        String response = this.connection.saveEntity(directoryDataSettings, DIRECTORY_DATA_BASE);
        ObjectMapper objectMapper = new ObjectMapper();
        DirectoryDataSettings settings = objectMapper.readValue(response, DirectoryDataSettings.class);
        return settings;
    }

    public DirectoryDataSettings getDirectorySettingsByName(String name) throws AcmeConnectionException {
        DirectoryDataSettings settings = this.connection.getEntityByName(DIRECTORY_DATA_BASE, name, DirectoryDataSettings.class);
        return settings;
    }

    @Override
    public DirectoryDataSettings getDirectorySettingsById(String id) throws AcmeConnectionException {
        DirectoryDataSettings settings = this.connection.getEntityById(DIRECTORY_DATA_BASE, id, DirectoryDataSettings.class);
        return settings;
    }

    public List<DirectoryDataSettings> getAllDirectorySettings() throws AcmeConnectionException {
        List<DirectoryDataSettings> list = this.connection.getAllEntities(DIRECTORY_DATA_BASE, DirectoryDataSettings.class);
        return list;
    }

    public void deleteDirectorySettings(String name) throws AcmeConnectionException {
        this.connection.deleteEntity(DIRECTORY_DATA_BASE, name);
    }

    @Override
    public List<AcmeCertAuthorityType> getAcmeCertAuthorityTypes() throws AcmeConnectionException {
        List<AcmeCertAuthorityType> list = this.connection.getAllEntitiesCustom(CERT_AUTHORITY_BASE+"/acmeCertAuthorityTypes", AcmeCertAuthorityType.class);
        return list;
    }

    @Override
    public CertificateAuthoritySettings saveCertificateAuthoritySettings(CertificateAuthoritySettings directoryDataSettings) throws AcmeConnectionException, IOException {
        String response = this.connection.saveEntity(directoryDataSettings, CERT_AUTHORITY_BASE);
        ObjectMapper objectMapper = new ObjectMapper();
        CertificateAuthoritySettings settings = objectMapper.readValue(response, CertificateAuthoritySettings.class);
        return settings;
    }

    @Override
    public CertificateAuthoritySettings getCertificateAuthoritySettingsByName(String name) throws AcmeConnectionException {
        CertificateAuthoritySettings settings = this.connection.getEntityByName(CERT_AUTHORITY_BASE, name, CertificateAuthoritySettings.class);
        return settings;
    }

    @Override
    public CertificateAuthoritySettings getCertificateAuthoritySettingsById(String id) throws AcmeConnectionException {
        CertificateAuthoritySettings settings = this.connection.getEntityById(CERT_AUTHORITY_BASE, id, CertificateAuthoritySettings.class);
        return settings;
    }

    @Override
    public List<CertificateAuthoritySettings> getAllCertificateAuthoritySettings() throws AcmeConnectionException {
        List<CertificateAuthoritySettings> list = this.connection.getAllEntities(CERT_AUTHORITY_BASE, CertificateAuthoritySettings.class);
        return list;
    }

    @Override
    public void deleteCertificateAuthoritySettings(String name) throws AcmeConnectionException {
        this.connection.deleteEntity(CERT_AUTHORITY_BASE, name);
    }

    @Override
    public ExternalAccountProviderSettings saveExternalAccountProviderSettings(ExternalAccountProviderSettings directoryDataSettings) throws AcmeConnectionException, IOException {
        String response = this.connection.saveEntity(directoryDataSettings, EXTERNAL_ACCOUNT_PROVIDER_BASE);
        ObjectMapper objectMapper = new ObjectMapper();
        ExternalAccountProviderSettings settings = objectMapper.readValue(response, ExternalAccountProviderSettings.class);
        return settings;
    }

    @Override
    public ExternalAccountProviderSettings getExternalAccountProviderSettingsByName(String name) throws AcmeConnectionException {
        ExternalAccountProviderSettings settings = this.connection.getEntityByName(EXTERNAL_ACCOUNT_PROVIDER_BASE, name, ExternalAccountProviderSettings.class);
        return settings;
    }

    @Override
    public ExternalAccountProviderSettings getExternalAccountProviderSettingsById(String id) throws AcmeConnectionException {
        ExternalAccountProviderSettings settings = this.connection.getEntityById(EXTERNAL_ACCOUNT_PROVIDER_BASE, id, ExternalAccountProviderSettings.class);
        return settings;
    }

    @Override
    public List<ExternalAccountProviderSettings> getAllExternalAccountProviderSettings() throws AcmeConnectionException {
        List<ExternalAccountProviderSettings> list = this.connection.getAllEntities(EXTERNAL_ACCOUNT_PROVIDER_BASE, ExternalAccountProviderSettings.class);
        return list;
    }

    @Override
    public void deleteExternalAccountProviderSettings(String name) throws AcmeConnectionException {
        this.connection.deleteEntity(EXTERNAL_ACCOUNT_PROVIDER_BASE, name);
    }

    public AcmeServerConnection getConnection() {
        return connection;
    }
}
