package com.winllc.pki.ra.acme;

import com.winllc.acme.common.CertificateAuthoritySettings;
import com.winllc.acme.common.DirectoryDataSettings;
import com.winllc.acme.common.ExternalAccountProviderSettings;

import java.util.List;

public class AcmeServerServiceImpl implements AcmeServerService {

    private static final String DIRECTORY_DATA_BASE = "directoryData";
    private static final String CERT_AUTHORITY_BASE = "certAuthority";
    private static final String EXTERNAL_ACCOUNT_PROVIDER_BASE = "externalAccountProvider";

    private AcmeServerConnection connection;

    public AcmeServerServiceImpl(AcmeServerConnection connection) {
        this.connection = connection;
    }

    public String getName() {
        return this.connection.getConnectionInfo().getName();
    }

    /*
    Directory settings CRUD
     */

    public void saveDirectorySettings(DirectoryDataSettings directoryDataSettings) {
        //todo

        this.connection.saveEntity(directoryDataSettings, DIRECTORY_DATA_BASE);
    }

    public DirectoryDataSettings getDirectorySettingsByName(String name) {
        //todo

        DirectoryDataSettings settings = this.connection.getEntityByName(DIRECTORY_DATA_BASE, name, DirectoryDataSettings.class);

        return settings;
    }

    public List<DirectoryDataSettings> getAllDirectorySettings() {
        List<DirectoryDataSettings> list = this.connection.getAllEntities(DIRECTORY_DATA_BASE, DirectoryDataSettings.class);
        //todo
        return list;
    }

    public void deleteDirectorySettings(String name) {
        //todo
        this.connection.deleteEntity(DIRECTORY_DATA_BASE, name);
    }

    @Override
    public void saveCertificateAuthoritySettings(CertificateAuthoritySettings directoryDataSettings) {
        this.connection.saveEntity(directoryDataSettings, CERT_AUTHORITY_BASE);
    }

    @Override
    public CertificateAuthoritySettings getCertificateAuthoritySettingsByName(String name) {
        CertificateAuthoritySettings settings = this.connection.getEntityByName(CERT_AUTHORITY_BASE, name, CertificateAuthoritySettings.class);
        return settings;
    }

    @Override
    public List<CertificateAuthoritySettings> getAllCertificateAuthoritySettings() {
        List<CertificateAuthoritySettings> list = this.connection.getAllEntities(CERT_AUTHORITY_BASE, CertificateAuthoritySettings.class);
        return list;
    }

    @Override
    public void deleteCertificateAuthoritySettings(String name) {
        //todo
        this.connection.deleteEntity(CERT_AUTHORITY_BASE, name);
    }

    @Override
    public void saveExternalAccountProviderSettings(ExternalAccountProviderSettings directoryDataSettings) {
        this.connection.saveEntity(directoryDataSettings, EXTERNAL_ACCOUNT_PROVIDER_BASE);
    }

    @Override
    public ExternalAccountProviderSettings getExternalAccountProviderSettingsByName(String name) {
        ExternalAccountProviderSettings settings = this.connection.getEntityByName(EXTERNAL_ACCOUNT_PROVIDER_BASE, name, ExternalAccountProviderSettings.class);
        return settings;
    }

    @Override
    public List<ExternalAccountProviderSettings> getAllExternalAccountProviderSettings() {
        List<ExternalAccountProviderSettings> list = this.connection.getAllEntities(EXTERNAL_ACCOUNT_PROVIDER_BASE, ExternalAccountProviderSettings.class);
        return list;
    }

    @Override
    public void deleteExternalAccountProviderSettings(String name) {
        //todo
        this.connection.deleteEntity(EXTERNAL_ACCOUNT_PROVIDER_BASE, name);
    }


}
