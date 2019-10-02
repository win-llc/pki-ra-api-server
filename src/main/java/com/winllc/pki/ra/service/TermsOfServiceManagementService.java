package com.winllc.pki.ra.service;

import com.winllc.acme.common.DirectoryDataSettings;
import com.winllc.pki.ra.domain.TermsOfService;
import com.winllc.pki.ra.repository.TermsOfServiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/tos")
public class TermsOfServiceManagementService {

    @Autowired
    private TermsOfServiceRepository repository;
    @Autowired
    private AcmeServerManagementService acmeServerManagementService;

    @GetMapping("/all")
    public ResponseEntity<?> getAll(){

        List<TermsOfService> termsList = repository.findAll();

        return ResponseEntity.ok(termsList);
    }

    @GetMapping("/getAllForDirectory/{directory}")
    public ResponseEntity<?> getAllForDirectory(@PathVariable String directory){

        List<TermsOfService> termsList = repository.findAllByForDirectoryName(directory);

        return ResponseEntity.ok(termsList);
    }

    @GetMapping("/getById/{id}")
    public ResponseEntity<?> getById(@PathVariable long id){

        Optional<TermsOfService> optionalTermsOfService = repository.findById(id);

        if(optionalTermsOfService.isPresent()){
            return ResponseEntity.ok(optionalTermsOfService.get());
        }else{
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/version/{versionId}")
    public ResponseEntity<?> getByVersionId(@PathVariable String versionId){
        Optional<TermsOfService> optionalTermsOfService = repository.findByVersionId(versionId);
        if(optionalTermsOfService.isPresent()){
            return ResponseEntity.ok(optionalTermsOfService.get());
        }else{
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/save/{connectionName}")
    public ResponseEntity<?> save(@PathVariable("connectionName") String acmeServerConnectionName, @RequestBody TermsOfService tos){
        DirectoryDataSettings settings = acmeServerManagementService.getDirectorySettingsByName(acmeServerConnectionName, tos.getForDirectoryName());

        TermsOfService newTos = TermsOfService.buildNew(tos.getText(), tos.getForDirectoryName());

        createAndUpdateAcmeServer(newTos, settings, acmeServerConnectionName);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/update/{connectionName}")
    public ResponseEntity<?> update(@PathVariable("connectionName") String acmeServerConnectionName, @RequestBody TermsOfService tos){
        DirectoryDataSettings settings = acmeServerManagementService.getDirectorySettingsByName(acmeServerConnectionName, tos.getForDirectoryName());

        Optional<TermsOfService> latestVersionOptional = repository.findByVersionId(tos.getVersionId());

        if(latestVersionOptional.isPresent()){
            createAndUpdateAcmeServer(tos, settings, acmeServerConnectionName);

            return ResponseEntity.ok().build();
        }else{
            return ResponseEntity.badRequest().build();
        }
    }

    private TermsOfService createAndUpdateAcmeServer(TermsOfService tos, DirectoryDataSettings settings, String acmeServerConnectionName){
        TermsOfService newTos = TermsOfService.buildNew(tos.getText(), tos.getForDirectoryName());
        newTos = repository.save(newTos);

        //todo proper url
        String tosUrl = "localhost/tos/version/"+newTos.getVersionId();

        settings.updateTermsOfService(tosUrl);

        acmeServerManagementService.saveDirectorySettings(acmeServerConnectionName, settings);

        return newTos;
    }

}
