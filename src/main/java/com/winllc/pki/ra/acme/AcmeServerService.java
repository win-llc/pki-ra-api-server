package com.winllc.pki.ra.acme;

import com.winllc.acme.common.CertificateAuthoritySettings;
import com.winllc.acme.common.DirectoryDataSettings;
import com.winllc.acme.common.ExternalAccountProviderSettings;
import com.winllc.pki.ra.exception.AcmeConnectionException;

import java.io.IOException;
import java.util.List;

public interface AcmeServerService {

    DirectoryDataSettings saveDirectorySettings(DirectoryDataSettings directoryDataSettings) throws AcmeConnectionException, IOException;
    DirectoryDataSettings getDirectorySettingsByName(String name) throws AcmeConnectionException;
    DirectoryDataSettings getDirectorySettingsById(String id) throws AcmeConnectionException;
    List<DirectoryDataSettings> getAllDirectorySettings() throws AcmeConnectionException;
    void deleteDirectorySettings(String name) throws AcmeConnectionException;

    CertificateAuthoritySettings saveCertificateAuthoritySettings(CertificateAuthoritySettings directoryDataSettings) throws AcmeConnectionException, IOException;
    CertificateAuthoritySettings getCertificateAuthoritySettingsByName(String name) throws AcmeConnectionException;
    CertificateAuthoritySettings getCertificateAuthoritySettingsById(String id) throws AcmeConnectionException;
    List<CertificateAuthoritySettings> getAllCertificateAuthoritySettings() throws AcmeConnectionException;
    void deleteCertificateAuthoritySettings(String name) throws AcmeConnectionException;

    ExternalAccountProviderSettings saveExternalAccountProviderSettings(ExternalAccountProviderSettings directoryDataSettings) throws AcmeConnectionException, IOException;
    ExternalAccountProviderSettings getExternalAccountProviderSettingsByName(String name) throws AcmeConnectionException;
    ExternalAccountProviderSettings getExternalAccountProviderSettingsById(String id) throws AcmeConnectionException;
    List<ExternalAccountProviderSettings> getAllExternalAccountProviderSettings() throws AcmeConnectionException;
    void deleteExternalAccountProviderSettings(String name) throws AcmeConnectionException;

}
