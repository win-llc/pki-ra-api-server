package com.winllc.pki.ra.service;

import com.winllc.acme.common.DirectoryDataSettings;
import com.winllc.pki.ra.domain.TermsOfService;
import com.winllc.pki.ra.exception.AcmeConnectionException;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.pki.ra.repository.TermsOfServiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

@RestController
public class TermsOfServiceManagementService {

    @Autowired
    private TermsOfServiceRepository repository;
    @Autowired
    private AcmeServerManagementService acmeServerManagementService;
    @Value("${server.base-url}")
    private String serverBaseUrl;

    @GetMapping("/api/tos/all")
    @ResponseStatus(HttpStatus.OK)
    public List<TermsOfService> getAll(){

        List<TermsOfService> termsList = repository.findAll();

        return termsList;
    }

    @GetMapping("/api/tos/getAllForDirectory/{directory}")
    @ResponseStatus(HttpStatus.OK)
    public List<TermsOfService> getAllForDirectory(@PathVariable String directory){

        List<TermsOfService> termsList = repository.findAllByForDirectoryName(directory);

        return termsList;
    }

    @GetMapping("/api/tos/getById/{id}")
    @ResponseStatus(HttpStatus.OK)
    public TermsOfService getById(@PathVariable long id) throws RAObjectNotFoundException {

        Optional<TermsOfService> optionalTermsOfService = repository.findById(id);

        if(optionalTermsOfService.isPresent()){
            return optionalTermsOfService.get();
        }else{
            throw new RAObjectNotFoundException(TermsOfService.class, id);
        }
    }

    @GetMapping("/api/tos/version/{versionId}")
    @ResponseStatus(HttpStatus.OK)
    public TermsOfService getByVersionId(@PathVariable String versionId) throws RAObjectNotFoundException {
        Optional<TermsOfService> optionalTermsOfService = repository.findByVersionId(versionId);
        if(optionalTermsOfService.isPresent()){
            return optionalTermsOfService.get();
        }else{
            throw new RAObjectNotFoundException(TermsOfService.class, versionId);
        }
    }

    @GetMapping("/tos/version/{versionId}/view")
    @ResponseStatus(HttpStatus.OK)
    public String getForView(@PathVariable String versionId) throws RAObjectNotFoundException {
        Optional<TermsOfService> optionalTermsOfService = repository.findByVersionId(versionId);
        if(optionalTermsOfService.isPresent()){
            return optionalTermsOfService.get().getText();
        }else{
            throw new RAObjectNotFoundException(TermsOfService.class, versionId);
        }
    }

    @PostMapping("/api/tos/save/{connectionName}")
    @ResponseStatus(HttpStatus.CREATED)
    public Long save(@PathVariable("connectionName") String acmeServerConnectionName,
                                  @Valid @RequestBody TermsOfService tos) throws AcmeConnectionException, IOException {
        DirectoryDataSettings settings = acmeServerManagementService.getDirectorySettingsByName(acmeServerConnectionName, tos.getForDirectoryName());

        TermsOfService newTos = TermsOfService.buildNew(tos.getText(), tos.getForDirectoryName());

        newTos = createAndUpdateAcmeServer(newTos, settings, acmeServerConnectionName);

        return newTos.getId();
    }

    @PostMapping("/api/tos/update/{connectionName}")
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

    @DeleteMapping("/api/tos/delete/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void delete(@PathVariable Long id){

        repository.deleteById(id);
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

}
