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
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/ca")
public class CertAuthorityConnectionService {

    private Map<String, CertAuthority> loadedCertAuthorities = new HashMap<>();

    //todo remove this
    @PostConstruct
    private void init(){
        CertAuthorityConnectionInfo info = new CertAuthorityConnectionInfo();
        info.setType("internal");
        info.setName("internal");

        info = repository.save(info);

        loadCertAuthority(info.getName());
    }

    @Autowired
    private CertAuthorityConnectionInfoRepository repository;

    //Build CA object for use
    private void loadCertAuthority(String name){
        Optional<CertAuthority> optionalCertAuthority = buildCertAuthority(name);
        if(optionalCertAuthority.isPresent()){
            CertAuthority ca = optionalCertAuthority.get();
            loadedCertAuthorities.put(ca.getName(), ca);
        }
    }

    @PostMapping("/api/info/create")
    public ResponseEntity<?> createConnectionInfo(CertAuthorityConnectionInfo connectionInfo){
        //todo validate
        connectionInfo = repository.save(connectionInfo);

        loadCertAuthority(connectionInfo.getName());

        return ResponseEntity.ok(connectionInfo);
    }

    @PostMapping("/api/info/update")
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

    @GetMapping("/api/info/byName/{name}")
    public ResponseEntity<?> getConnectionInfoByName(@PathVariable String name){
        //todo
        CertAuthorityConnectionInfo info = repository.findByName(name);

        return ResponseEntity.ok(info);
    }

    @GetMapping("/api/info/all")
    public ResponseEntity<?> getAllConnectionInfo(){
        List<CertAuthorityConnectionInfo> list = repository.findAll();

        return ResponseEntity.ok(list);
    }

    @DeleteMapping("/api/info/delete/{id}")
    public ResponseEntity<?> deleteInfo(@PathVariable long id){
        repository.deleteById(id);

        return ResponseEntity.ok().build();
    }



    @PostMapping("/issueCertificate/{connectionName}")
    public ResponseEntity<?> issueCertificate(@PathVariable String connectionName, @RequestParam String pkcs10){
        //todo issue through here, not /ca/internal

        CertAuthority certAuthority = loadedCertAuthorities.get(connectionName);
        if(certAuthority != null){
            X509Certificate cert = certAuthority.issueCertificate(pkcs10);
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
        CertAuthority certAuthority = loadedCertAuthorities.get(connectionName);
        if(certAuthority != null) {
            boolean revoked = certAuthority.revokeCertificate(serial, reason);
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

        CertAuthority certAuthority = loadedCertAuthorities.get(connectionName);
        if(certAuthority != null) {
            //todo
            return ResponseEntity.ok(status);
        }

        return ResponseEntity.notFound().build();
    }

    @GetMapping("/trustChain/{connectionName}")
    public ResponseEntity<?> getTrustChain(@PathVariable String connectionName){
        CertAuthority certAuthority = loadedCertAuthorities.get(connectionName);

        Certificate[] trustChain = certAuthority.getTrustChain();

        StringBuilder stringBuilder = new StringBuilder();
        for(Certificate cert : trustChain){
            try {
                stringBuilder.append(CertUtil.convertToPem(cert)).append("\n");
            } catch (CertificateEncodingException e) {
                e.printStackTrace();
            }
        }
        return ResponseEntity.ok(stringBuilder.toString());
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
