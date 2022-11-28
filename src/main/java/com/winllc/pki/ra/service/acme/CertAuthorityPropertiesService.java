package com.winllc.pki.ra.service.acme;

import com.winllc.acme.common.CertificateAuthoritySettings;
import com.winllc.acme.common.ExternalAccountProviderSettings;
import com.winllc.acme.common.domain.AcmeServerConnectionInfo;
import com.winllc.acme.common.repository.AcmeServerConnectionInfoRepository;
import com.winllc.pki.ra.beans.search.GridModel;
import com.winllc.pki.ra.endpoint.acme.AcmeServerService;
import com.winllc.pki.ra.exception.AcmeConnectionException;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.pki.ra.service.DataService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/acme/ca")
public class CertAuthorityPropertiesService extends AcmeServerManagementService<CertificateAuthoritySettings> {

    public CertAuthorityPropertiesService(@Value("${win-ra.acme-server-url}") String winraAcmeServerUrl,
                                          @Value("${win-ra.acme-server-name}") String winraAcmeServerName,
                                          AcmeServerConnectionInfoRepository connectionInfoRepository) {
        super(winraAcmeServerUrl, winraAcmeServerName, connectionInfoRepository);
    }


    @Override
    @PostMapping("/paged")
    public Page<CertificateAuthoritySettings> getPaged(@RequestParam Integer page,
                                                          @RequestParam Integer pageSize,
                                                          @RequestParam(defaultValue = "asc") String order,
                                                          @RequestParam(required = false) String sortBy,
                                                          @RequestParam Map<String, String> allRequestParams,
                                                          @RequestBody GridModel gridModel,
                                                          Authentication authentication) {

        AcmeServerService acmeServerService = services.get(defaultConnectionName);
        try {
            List<CertificateAuthoritySettings> allCertificateAuthoritySettings =
                    acmeServerService.getAllCertificateAuthoritySettings();
            return new PageImpl<>(allCertificateAuthoritySettings);
        } catch (AcmeConnectionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Page<CertificateAuthoritySettings> getMyPaged(Integer page, Integer pageSize, String order, String sortBy, Map<String, String> allRequestParams, GridModel gridModel, Authentication authentication) {
        return null;
    }

    @GetMapping("/all")
    @Override
    public List<CertificateAuthoritySettings> getAll(Authentication authentication) throws Exception {
        AcmeServerService acmeServerService = services.get(defaultConnectionName);
        List<CertificateAuthoritySettings> allCertificateAuthoritySettings =
                acmeServerService.getAllCertificateAuthoritySettings();


        return allCertificateAuthoritySettings;
    }

    @Override
    @GetMapping("/id/{id}")
    public CertificateAuthoritySettings findRest(@PathVariable String id, Authentication authentication) throws Exception {
        AcmeServerService acmeServerService = services.get(defaultConnectionName);
        return acmeServerService.getCertificateAuthoritySettingsByName(id);
    }

    @Override
    public CertificateAuthoritySettings addRest(CertificateAuthoritySettings entity, Map<String, String> allRequestParams, Authentication authentication) throws Exception {
        return null;
    }

    @Override
    public CertificateAuthoritySettings updateRest(CertificateAuthoritySettings entity, Map<String, String> allRequestParams, Authentication authentication) throws Exception {
        return null;
    }

    @Override
    public void deleteRest(String id, CertificateAuthoritySettings form, Authentication authentication) throws Exception {

    }


}
