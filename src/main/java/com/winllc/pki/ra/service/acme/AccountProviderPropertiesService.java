package com.winllc.pki.ra.service.acme;

import com.winllc.acme.common.CertificateAuthoritySettings;
import com.winllc.acme.common.ExternalAccountProviderSettings;
import com.winllc.acme.common.repository.AcmeServerConnectionInfoRepository;
import com.winllc.pki.ra.endpoint.acme.AcmeServerService;
import com.winllc.pki.ra.exception.AcmeConnectionException;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/acme/accountProvider")
public class AccountProviderPropertiesService extends AcmeServerManagementService<ExternalAccountProviderSettings> {

    public AccountProviderPropertiesService(@Value("${win-ra.acme-server-url}") String winraAcmeServerUrl,
                                            @Value("${win-ra.acme-server-name}") String winraAcmeServerName,
                                            AcmeServerConnectionInfoRepository connectionInfoRepository) {
        super(winraAcmeServerUrl, winraAcmeServerName, connectionInfoRepository);
    }

    @GetMapping("/paged")
    public Page<ExternalAccountProviderSettings> getPaged(@RequestParam Integer page,
                     @RequestParam Integer pageSize,
                     @RequestParam(defaultValue = "asc") String order,
                     @RequestParam(required = false) String sortBy,
                     @RequestParam Map<String, String> allRequestParams){

        return null;
    }

    @GetMapping("/my/paged")
    public Page<ExternalAccountProviderSettings> getMyPaged(@RequestParam Integer page,
                       @RequestParam Integer pageSize,
                       @RequestParam(defaultValue = "asc") String order,
                       @RequestParam(required = false) String sortBy,
                       @RequestParam Map<String, String> allRequestParams,
                       Authentication authentication){
        return null;
    }

    @GetMapping("/all")
    public List<ExternalAccountProviderSettings> getAll(Authentication authentication) throws RAObjectNotFoundException, AcmeConnectionException {
        AcmeServerService acmeServerService = services.get(defaultConnectionName);
        List<ExternalAccountProviderSettings> settings =
                acmeServerService.getAllExternalAccountProviderSettings();


        return settings;
    }

    @GetMapping("/id/{id}")
    public ExternalAccountProviderSettings findRest(@PathVariable Long id, Authentication authentication) throws Exception{

        return null;
    }

    @PostMapping("/add")
    public ExternalAccountProviderSettings addRest(@RequestBody ExternalAccountProviderSettings entity,
                                                   BindingResult bindingResult,
                                                   Authentication authentication) throws Exception {

        return null;
    }

    @PostMapping("/update")
    public ExternalAccountProviderSettings updateRest(@RequestBody ExternalAccountProviderSettings entity, Authentication authentication)
            throws Exception {

        return null;
    }

    @DeleteMapping("/delete/{id}")
    public void deleteRest(@PathVariable Long id, Authentication authentication) throws RAObjectNotFoundException {

    }
}
