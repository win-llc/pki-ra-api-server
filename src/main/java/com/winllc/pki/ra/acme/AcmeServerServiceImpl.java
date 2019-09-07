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
        //todo
        return null;
    }

    public void deleteDirectorySettings(String name) {
        //todo
    }

    @Override
    public void saveCertificateAuthoritySettings(CertificateAuthoritySettings directoryDataSettings) {

    }

    @Override
    public CertificateAuthoritySettings getCertificateAuthoritySettingsByName(String name) {
        return null;
    }

    @Override
    public List<CertificateAuthoritySettings> getAllCertificateAuthoritySettings() {
        return null;
    }

    @Override
    public void deleteCertificateAuthoritySettings(String name) {

    }

    @Override
    public void saveExternalAccountProviderSettings(ExternalAccountProviderSettings directoryDataSettings) {

    }

    @Override
    public ExternalAccountProviderSettings getExternalAccountProviderSettingsByName(String name) {
        return null;
    }

    @Override
    public List<ExternalAccountProviderSettings> getAllExternalAccountProviderSettings() {
        return null;
    }

    @Override
    public void deleteExternalAccountProviderSettings(String name) {

    }


}
