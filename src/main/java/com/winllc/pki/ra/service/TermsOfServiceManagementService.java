package com.winllc.pki.ra.service;

import com.winllc.acme.common.DirectoryDataSettings;
import com.winllc.pki.ra.domain.TermsOfService;
import com.winllc.pki.ra.exception.AcmeConnectionException;
import com.winllc.pki.ra.repository.TermsOfServiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
    public ResponseEntity<?> getAll(){

        List<TermsOfService> termsList = repository.findAll();

        return ResponseEntity.ok(termsList);
    }

    @GetMapping("/api/tos/getAllForDirectory/{directory}")
    public ResponseEntity<?> getAllForDirectory(@PathVariable String directory){

        List<TermsOfService> termsList = repository.findAllByForDirectoryName(directory);

        return ResponseEntity.ok(termsList);
    }

    @GetMapping("/api/tos/getById/{id}")
    public ResponseEntity<?> getById(@PathVariable long id){

        Optional<TermsOfService> optionalTermsOfService = repository.findById(id);

        if(optionalTermsOfService.isPresent()){
            return ResponseEntity.ok(optionalTermsOfService.get());
        }else{
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/api/tos/version/{versionId}")
    public ResponseEntity<?> getByVersionId(@PathVariable String versionId){
        Optional<TermsOfService> optionalTermsOfService = repository.findByVersionId(versionId);
        if(optionalTermsOfService.isPresent()){
            return ResponseEntity.ok(optionalTermsOfService.get());
        }else{
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/tos/version/{versionId}/view")
    public ResponseEntity<?> getForView(@PathVariable String versionId){
        Optional<TermsOfService> optionalTermsOfService = repository.findByVersionId(versionId);
        if(optionalTermsOfService.isPresent()){
            return ResponseEntity.ok(optionalTermsOfService.get().getText());
        }else{
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/api/tos/save/{connectionName}")
    public ResponseEntity<?> save(@PathVariable("connectionName") String acmeServerConnectionName,
                                  @Valid @RequestBody TermsOfService tos) throws AcmeConnectionException, IOException {
        DirectoryDataSettings settings = acmeServerManagementService.getDirectorySettingsByName(acmeServerConnectionName, tos.getForDirectoryName());

        TermsOfService newTos = TermsOfService.buildNew(tos.getText(), tos.getForDirectoryName());

        newTos = createAndUpdateAcmeServer(newTos, settings, acmeServerConnectionName);

        return ResponseEntity.ok(newTos.getId());
    }

    @PostMapping("/api/tos/update/{connectionName}")
    public ResponseEntity<?> update(@PathVariable("connectionName") String acmeServerConnectionName,
                                    @Valid @RequestBody TermsOfService tos) throws AcmeConnectionException, IOException {
        DirectoryDataSettings settings = acmeServerManagementService.getDirectorySettingsByName(acmeServerConnectionName, tos.getForDirectoryName());

        Optional<TermsOfService> latestVersionOptional = repository.findByVersionId(tos.getVersionId());

        if(latestVersionOptional.isPresent()){
            tos = createAndUpdateAcmeServer(tos, settings, acmeServerConnectionName);

            return ResponseEntity.ok(tos);
        }else{
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/api/tos/delete/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id){

        repository.deleteById(id);

        return ResponseEntity.ok().build();
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
