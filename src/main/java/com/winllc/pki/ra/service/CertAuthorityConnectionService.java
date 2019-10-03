package com.winllc.pki.ra.service;

import com.winllc.acme.common.CertificateStatus;
import com.winllc.acme.common.util.CertUtil;
import com.winllc.pki.ra.ca.CertAuthority;
import com.winllc.pki.ra.ca.InternalCertAuthority;
import com.winllc.pki.ra.domain.CertAuthorityConnectionInfo;
import com.winllc.pki.ra.repository.CertAuthorityConnectionInfoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/ca")
public class CertAuthorityConnectionService {

    //todo remove this
    @PostConstruct
    private void init(){
        CertAuthorityConnectionInfo info = new CertAuthorityConnectionInfo();
        info.setType("internal");
        info.setName("internal");

        repository.save(info);
    }

    @Autowired
    private CertAuthorityConnectionInfoRepository repository;

    @PostMapping("/info/create")
    public ResponseEntity<?> createConnectionInfo(CertAuthorityConnectionInfo connectionInfo){
        //todo validate
        connectionInfo = repository.save(connectionInfo);

        return ResponseEntity.ok(connectionInfo);
    }

    @PostMapping("/info/update")
    public ResponseEntity<?> updateConnectionInfo(CertAuthorityConnectionInfo connectionInfo){
        //todo validate
        Optional<CertAuthorityConnectionInfo> optionalInfo = repository.findById(connectionInfo.getId());
        if(optionalInfo.isPresent()){
            CertAuthorityConnectionInfo info = optionalInfo.get();
            info.setType(connectionInfo.getType());
            //todo the rest
        }

        return ResponseEntity.notFound().build();
    }

    @GetMapping("/info/byName/{name}")
    public ResponseEntity<?> getConnectionInfoByName(@PathVariable String name){
        //todo
        CertAuthorityConnectionInfo info = repository.findByName(name);

        return ResponseEntity.ok(info);
    }

    @GetMapping("/info/all")
    public ResponseEntity<?> getAllConnectionInfo(){
        List<CertAuthorityConnectionInfo> list = repository.findAll();

        return ResponseEntity.ok(list);
    }

    @DeleteMapping("/info/delete/{id}")
    public ResponseEntity<?> deleteInfo(@PathVariable long id){
        repository.deleteById(id);

        return ResponseEntity.ok().build();
    }



    @PostMapping("/issueCertificate/{connectionName}")
    public ResponseEntity<?> issueCertificate(@PathVariable String connectionName, @RequestParam String pkcs10){
        //todo issue through here, not /ca/internal

        Optional<CertAuthority> certAuthorityOptional = buildCertAuthority(connectionName);
        if(certAuthorityOptional.isPresent()){
            CertAuthority ca = certAuthorityOptional.get();
            X509Certificate cert = ca.issueCertificate(pkcs10);
            try {
                return ResponseEntity.ok(CertUtil.convertToPem(cert));
            } catch (CertificateEncodingException e) {
                e.printStackTrace();
                return ResponseEntity.badRequest().build();
            }
        }else{
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/revokeCertificate/{connectionName}")
    public ResponseEntity<?> revokeCertificate(@PathVariable String connectionName,
                                               @RequestParam String serial, @RequestParam int reason){
        Optional<CertAuthority> certAuthorityOptional = buildCertAuthority(connectionName);
        if(certAuthorityOptional.isPresent()) {
            CertAuthority ca = certAuthorityOptional.get();
            boolean revoked = ca.revokeCertificate(serial, reason);
            if(revoked){
                return ResponseEntity.ok().build();
            }else{
                return ResponseEntity.badRequest().build();
            }
        }else{
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/certStatus/{connectionName}")
    public ResponseEntity<?> getCertificateStatus(@PathVariable String connectionName, @RequestParam String serial){
        CertificateStatus status = new CertificateStatus();

        Optional<CertAuthority> certAuthorityOptional = buildCertAuthority(connectionName);
        if(certAuthorityOptional.isPresent()) {
            CertAuthority ca = certAuthorityOptional.get();
            //todo
            return ResponseEntity.ok(status);
        }

        return ResponseEntity.notFound().build();
    }


    private Optional<CertAuthority> buildCertAuthority(String connectionName){
        CertAuthority certAuthority = null;
        CertAuthorityConnectionInfo info = repository.findByName(connectionName);
        if(info.getType().contentEquals("internal")){
            certAuthority = new InternalCertAuthority(info);
        }

        if(certAuthority != null){
            return Optional.of(certAuthority);
        }else{
            return Optional.empty();
        }
    }
}
