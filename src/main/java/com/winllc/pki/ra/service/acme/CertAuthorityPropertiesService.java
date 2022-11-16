package com.winllc.pki.ra.service.acme;

import com.winllc.acme.common.CertificateAuthoritySettings;
import com.winllc.acme.common.domain.AcmeServerConnectionInfo;
import com.winllc.acme.common.repository.AcmeServerConnectionInfoRepository;
import com.winllc.pki.ra.endpoint.acme.AcmeServerService;
import com.winllc.pki.ra.exception.AcmeConnectionException;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.pki.ra.service.DataService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
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

    @GetMapping("/paged")
    public Page<CertificateAuthoritySettings> getPaged(@RequestParam Integer page,
                     @RequestParam Integer pageSize,
                     @RequestParam(defaultValue = "asc") String order,
                     @RequestParam(required = false) String sortBy,
                     @RequestParam Map<String, String> allRequestParams){

        return null;
    }

    @GetMapping("/my/paged")
    public Page<CertificateAuthoritySettings> getMyPaged(@RequestParam Integer page,
                       @RequestParam Integer pageSize,
                       @RequestParam(defaultValue = "asc") String order,
                       @RequestParam(required = false) String sortBy,
                       @RequestParam Map<String, String> allRequestParams,
                       Authentication authentication){
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


    @GetMapping("/id/{id}")
    public CertificateAuthoritySettings findRest(@PathVariable Long id, Authentication authentication) throws Exception{

        return null;
    }

    @PostMapping("/add")
    public CertificateAuthoritySettings addRest(@RequestBody CertificateAuthoritySettings entity,
                                                BindingResult bindingResult,
                                                Authentication authentication) throws Exception {

        return null;
    }

    @PostMapping("/update")
    public CertificateAuthoritySettings updateRest(@RequestBody CertificateAuthoritySettings entity, Authentication authentication)
            throws Exception {

        return null;
    }

    @DeleteMapping("/delete/{id}")
    public void deleteRest(@PathVariable Long id, Authentication authentication) throws RAObjectNotFoundException {

    }
}
