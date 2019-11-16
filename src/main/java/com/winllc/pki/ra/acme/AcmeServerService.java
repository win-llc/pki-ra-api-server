package com.winllc.pki.ra.acme;

import com.winllc.acme.common.CertificateAuthoritySettings;
import com.winllc.acme.common.DirectoryDataSettings;
import com.winllc.acme.common.ExternalAccountProviderSettings;
import com.winllc.pki.ra.exception.AcmeConnectionException;

import java.util.List;

public interface AcmeServerService {

    void saveDirectorySettings(DirectoryDataSettings directoryDataSettings) throws AcmeConnectionException;
    DirectoryDataSettings getDirectorySettingsByName(String name) throws AcmeConnectionException;
    List<DirectoryDataSettings> getAllDirectorySettings() throws AcmeConnectionException;
    void deleteDirectorySettings(String name) throws AcmeConnectionException;

    void saveCertificateAuthoritySettings(CertificateAuthoritySettings directoryDataSettings) throws AcmeConnectionException;
    CertificateAuthoritySettings getCertificateAuthoritySettingsByName(String name) throws AcmeConnectionException;
    List<CertificateAuthoritySettings> getAllCertificateAuthoritySettings() throws AcmeConnectionException;
    void deleteCertificateAuthoritySettings(String name) throws AcmeConnectionException;

    void saveExternalAccountProviderSettings(ExternalAccountProviderSettings directoryDataSettings) throws AcmeConnectionException;
    ExternalAccountProviderSettings getExternalAccountProviderSettingsByName(String name) throws AcmeConnectionException;
    List<ExternalAccountProviderSettings> getAllExternalAccountProviderSettings() throws AcmeConnectionException;
    void deleteExternalAccountProviderSettings(String name) throws AcmeConnectionException;

}
