package com.winllc.pki.ra.acme;

import com.winllc.acme.common.CertificateAuthoritySettings;
import com.winllc.acme.common.DirectoryDataSettings;
import com.winllc.acme.common.ExternalAccountProviderSettings;

import java.util.List;

public interface AcmeServerService {

    void saveDirectorySettings(DirectoryDataSettings directoryDataSettings);
    DirectoryDataSettings getDirectorySettingsByName(String name);
    List<DirectoryDataSettings> getAllDirectorySettings();
    void deleteDirectorySettings(String name);

    void saveCertificateAuthoritySettings(CertificateAuthoritySettings directoryDataSettings);
    CertificateAuthoritySettings getCertificateAuthoritySettingsByName(String name);
    List<CertificateAuthoritySettings> getAllCertificateAuthoritySettings();
    void deleteCertificateAuthoritySettings(String name);

    void saveExternalAccountProviderSettings(ExternalAccountProviderSettings directoryDataSettings);
    ExternalAccountProviderSettings getExternalAccountProviderSettingsByName(String name);
    List<ExternalAccountProviderSettings> getAllExternalAccountProviderSettings();
    void deleteExternalAccountProviderSettings(String name);

}
