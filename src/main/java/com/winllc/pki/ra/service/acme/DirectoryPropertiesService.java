package com.winllc.pki.ra.service.acme;

import com.winllc.acme.common.CertificateAuthoritySettings;
import com.winllc.acme.common.DirectoryDataSettings;
import com.winllc.acme.common.DirectoryDataSettings;
import com.winllc.acme.common.repository.AcmeServerConnectionInfoRepository;
import com.winllc.pki.ra.beans.search.GridModel;
import com.winllc.pki.ra.endpoint.acme.AcmeServerService;
import com.winllc.pki.ra.exception.AcmeConnectionException;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/acme/directory")
public class DirectoryPropertiesService extends AcmeServerManagementService<DirectoryDataSettings> {

    public DirectoryPropertiesService(@Value("${win-ra.acme-server-url}") String winraAcmeServerUrl,
                                      @Value("${win-ra.acme-server-name}") String winraAcmeServerName,
                                      AcmeServerConnectionInfoRepository connectionInfoRepository) {
        super(winraAcmeServerUrl, winraAcmeServerName, connectionInfoRepository);
    }


    @Override
    @PostMapping("/paged")
    public Page<DirectoryDataSettings> getPaged(@RequestParam Integer page,
                                                       @RequestParam Integer pageSize,
                                                       @RequestParam(defaultValue = "asc") String order,
                                                       @RequestParam(required = false) String sortBy,
                                                       @RequestParam Map<String, String> allRequestParams,
                                                       @RequestBody GridModel gridModel,
                                                       Authentication authentication) {

        AcmeServerService acmeServerService = services.get(defaultConnectionName);
        try {
            List<DirectoryDataSettings> settings =
                    acmeServerService.getAllDirectorySettings();
            return new PageImpl<>(settings);
        } catch (AcmeConnectionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Page<DirectoryDataSettings> getMyPaged(Integer page, Integer pageSize, String order, String sortBy, Map<String, String> allRequestParams, GridModel gridModel, Authentication authentication) {
        return null;
    }

    @GetMapping("/all")
    public List<DirectoryDataSettings> getAll(Authentication authentication) throws RAObjectNotFoundException, AcmeConnectionException {
        AcmeServerService acmeServerService = services.get(defaultConnectionName);
        List<DirectoryDataSettings> settings =
                acmeServerService.getAllDirectorySettings();

        return settings;
    }

    @Override
    @GetMapping("/id/{id}")
    public DirectoryDataSettings findRest(@PathVariable String id, Authentication authentication) throws Exception {
        AcmeServerService acmeServerService = services.get(defaultConnectionName);
        return acmeServerService.getDirectorySettingsByName(id);
    }

    @Override
    public DirectoryDataSettings addRest(DirectoryDataSettings entity, Map<String, String> allRequestParams, Authentication authentication) throws Exception {
        return null;
    }

    @Override
    public DirectoryDataSettings updateRest(DirectoryDataSettings entity, Map<String, String> allRequestParams, Authentication authentication) throws Exception {
        return null;
    }

    @Override
    public void deleteRest(String id, DirectoryDataSettings form, Authentication authentication) throws Exception {

    }

}
