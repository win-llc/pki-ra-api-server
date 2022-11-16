package com.winllc.pki.ra.service.acme;

import com.winllc.acme.common.DirectoryDataSettings;
import com.winllc.acme.common.DirectoryDataSettings;
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
@RequestMapping("/acme/directory")
public class DirectoryPropertiesService extends AcmeServerManagementService<DirectoryDataSettings> {

    public DirectoryPropertiesService(@Value("${win-ra.acme-server-url}") String winraAcmeServerUrl,
                                      @Value("${win-ra.acme-server-name}") String winraAcmeServerName,
                                      AcmeServerConnectionInfoRepository connectionInfoRepository) {
        super(winraAcmeServerUrl, winraAcmeServerName, connectionInfoRepository);
    }

    @GetMapping("/paged")
    public Page<DirectoryDataSettings> getPaged(@RequestParam Integer page,
                     @RequestParam Integer pageSize,
                     @RequestParam(defaultValue = "asc") String order,
                     @RequestParam(required = false) String sortBy,
                     @RequestParam Map<String, String> allRequestParams){

        return null;
    }

    @GetMapping("/my/paged")
    public Page<DirectoryDataSettings> getMyPaged(@RequestParam Integer page,
                       @RequestParam Integer pageSize,
                       @RequestParam(defaultValue = "asc") String order,
                       @RequestParam(required = false) String sortBy,
                       @RequestParam Map<String, String> allRequestParams,
                       Authentication authentication){
        return null;
    }

    @GetMapping("/all")
    public List<DirectoryDataSettings> getAll(Authentication authentication) throws RAObjectNotFoundException, AcmeConnectionException {
        AcmeServerService acmeServerService = services.get(defaultConnectionName);
        List<DirectoryDataSettings> settings =
                acmeServerService.getAllDirectorySettings();

        return settings;
    }

    @GetMapping("/id/{id}")
    public DirectoryDataSettings findRest(@PathVariable Long id, Authentication authentication) throws Exception{

        return null;
    }

    @PostMapping("/add")
    public DirectoryDataSettings addRest(@RequestBody DirectoryDataSettings entity,
                                         BindingResult bindingResult,
                                         Authentication authentication) throws Exception {

        return null;
    }

    @PostMapping("/update")
    public DirectoryDataSettings updateRest(@RequestBody DirectoryDataSettings entity, Authentication authentication)
            throws Exception {

        return null;
    }

    @DeleteMapping("/delete/{id}")
    public void deleteRest(@PathVariable Long id, Authentication authentication) throws RAObjectNotFoundException {

    }
}
