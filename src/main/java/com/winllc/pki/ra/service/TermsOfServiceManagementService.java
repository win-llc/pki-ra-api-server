package com.winllc.pki.ra.service;

import com.winllc.acme.common.DirectoryDataSettings;
import com.winllc.acme.common.domain.CertAuthorityConnectionInfo;
import com.winllc.acme.common.domain.TermsOfService;
import com.winllc.acme.common.repository.CertAuthorityConnectionInfoRepository;
import com.winllc.pki.ra.beans.form.CertAuthorityConnectionInfoForm;
import com.winllc.pki.ra.beans.form.TermsOfServiceForm;
import com.winllc.pki.ra.exception.AcmeConnectionException;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.acme.common.repository.TermsOfServiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/tos")
public class TermsOfServiceManagementService extends
        DataPagedService<TermsOfService, TermsOfServiceForm,
                TermsOfServiceRepository> {

    private final TermsOfServiceRepository repository;
    private final AcmeServerManagementService acmeServerManagementService;
    @Value("${server.base-url}")
    private String serverBaseUrl;

    public TermsOfServiceManagementService(ApplicationContext context,
                                           TermsOfServiceRepository repository,
                                           AcmeServerManagementService acmeServerManagementService) {
        super(context, TermsOfService.class, repository);
        this.repository = repository;
        this.acmeServerManagementService = acmeServerManagementService;
    }


    @GetMapping("/getAllForDirectory/{directory}")
    @ResponseStatus(HttpStatus.OK)
    public List<TermsOfService> getAllForDirectory(@PathVariable String directory){

        List<TermsOfService> termsList = repository.findAllByForDirectoryName(directory);

        return termsList;
    }


    @GetMapping("/version/{versionId}")
    @ResponseStatus(HttpStatus.OK)
    public TermsOfService getByVersionId(@PathVariable String versionId) throws RAObjectNotFoundException {
        Optional<TermsOfService> optionalTermsOfService = repository.findByVersionId(versionId);
        if(optionalTermsOfService.isPresent()){
            return optionalTermsOfService.get();
        }else{
            throw new RAObjectNotFoundException(TermsOfService.class, versionId);
        }
    }

    @GetMapping("/version/{versionId}/view")
    @ResponseStatus(HttpStatus.OK)
    public String getForView(@PathVariable String versionId) throws RAObjectNotFoundException {
        Optional<TermsOfService> optionalTermsOfService = repository.findByVersionId(versionId);
        if(optionalTermsOfService.isPresent()){
            return optionalTermsOfService.get().getText();
        }else{
            throw new RAObjectNotFoundException(TermsOfService.class, versionId);
        }
    }

    @PostMapping("/save/{connectionName}")
    @ResponseStatus(HttpStatus.CREATED)
    public Long save(@PathVariable("connectionName") String acmeServerConnectionName,
                                  @Valid @RequestBody TermsOfService tos) throws AcmeConnectionException, IOException {
        DirectoryDataSettings settings = acmeServerManagementService.getDirectorySettingsByName(acmeServerConnectionName, tos.getForDirectoryName());

        TermsOfService newTos = TermsOfService.buildNew(tos.getText(), tos.getForDirectoryName());

        newTos = createAndUpdateAcmeServer(newTos, settings, acmeServerConnectionName);

        return newTos.getId();
    }

    @PostMapping("/update/{connectionName}")
    @ResponseStatus(HttpStatus.OK)
    public TermsOfService update(@PathVariable("connectionName") String acmeServerConnectionName,
                                    @Valid @RequestBody TermsOfService tos) throws AcmeConnectionException,
            IOException, RAObjectNotFoundException {
        DirectoryDataSettings settings = acmeServerManagementService.getDirectorySettingsByName(acmeServerConnectionName, tos.getForDirectoryName());

        Optional<TermsOfService> latestVersionOptional = repository.findByVersionId(tos.getVersionId());

        if(latestVersionOptional.isPresent()){
            tos = createAndUpdateAcmeServer(tos, settings, acmeServerConnectionName);

            return tos;
        }else{
            throw new RAObjectNotFoundException(TermsOfService.class, tos.getVersionId());
        }
    }



    private TermsOfService createAndUpdateAcmeServer(TermsOfService tos, DirectoryDataSettings settings,
                                                     String acmeServerConnectionName) throws AcmeConnectionException, IOException {
        TermsOfService newTos = TermsOfService.buildNew(tos.getText(), tos.getForDirectoryName());
        newTos = repository.save(newTos);

        //todo proper url
        String tosUrl = serverBaseUrl+"/tos/version/"+newTos.getVersionId()+"/view";

        settings.updateTermsOfService(tosUrl);

        acmeServerManagementService.saveDirectorySettings(acmeServerConnectionName, settings);

        return newTos;
    }

    @Override
    protected TermsOfServiceForm entityToForm(TermsOfService entity) {
        return new TermsOfServiceForm(entity);
    }

    @Override
    protected TermsOfService formToEntity(TermsOfServiceForm form, Authentication authentication) throws Exception {
        return null;
    }

    @Override
    protected TermsOfService combine(TermsOfService original,
                                     TermsOfService updated, Authentication authentication) {
        return null;
    }

    @Override
    public List<Predicate> buildFilter(Map<String, String> allRequestParams, Root<TermsOfService> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        return null;
    }
}
