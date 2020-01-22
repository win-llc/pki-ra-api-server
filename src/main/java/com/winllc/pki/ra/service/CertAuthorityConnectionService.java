package com.winllc.pki.ra.service;

import com.winllc.acme.common.*;
import com.winllc.acme.common.util.CertUtil;
import com.winllc.pki.ra.beans.validator.CertAuthorityConnectionInfoValidator;
import com.winllc.pki.ra.ca.CertAuthority;
import com.winllc.pki.ra.ca.InternalCertAuthority;
import com.winllc.pki.ra.domain.CertAuthorityConnectionInfo;
import com.winllc.pki.ra.repository.CertAuthorityConnectionInfoRepository;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.swing.text.html.Option;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.*;

@RestController
@RequestMapping("/ca")
public class CertAuthorityConnectionService {

    private static final Logger log = LogManager.getLogger(CertAuthorityConnectionService.class);

    @Autowired
    private CertAuthorityConnectionInfoRepository repository;
    @Autowired
    private EntityManagerFactory entityManagerFactory;
    @Autowired
    private CertAuthorityConnectionInfoValidator validator;

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


    //Build CA object for use
    private void loadCertAuthority(String name){
        Optional<CertAuthority> optionalCertAuthority = buildCertAuthority(name);
        if(optionalCertAuthority.isPresent()){
            CertAuthority ca = optionalCertAuthority.get();

            if(ca instanceof InternalCertAuthority){
                ((InternalCertAuthority) ca).setEntityManager(entityManagerFactory);
            }

            loadedCertAuthorities.put(ca.getName(), ca);
        }
    }

    @PostMapping("/api/info/create")
    public ResponseEntity<?> createConnectionInfo(@RequestBody CertAuthorityConnectionInfo connectionInfo){
        //todo validate

        boolean valid = validator.validate(connectionInfo, false);

        if(valid) {
            connectionInfo = repository.save(connectionInfo);

            loadCertAuthority(connectionInfo.getName());

            return ResponseEntity.ok(connectionInfo.getId());
        }else{
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/api/info/update")
    public ResponseEntity<?> updateConnectionInfo(CertAuthorityConnectionInfo connectionInfo){
        //todo validate
        Optional<CertAuthorityConnectionInfo> optionalInfo = repository.findById(connectionInfo.getId());
        if(optionalInfo.isPresent()){
            CertAuthorityConnectionInfo info = optionalInfo.get();
            info.setType(connectionInfo.getType());
            //todo the rest

            info = repository.save(info);
            return ResponseEntity.ok(info);
        }

        return ResponseEntity.notFound().build();
    }

    @GetMapping("/api/info/byName/{name}")
    public ResponseEntity<?> getConnectionInfoByName(@PathVariable String name){
        Optional<CertAuthorityConnectionInfo> infoOptional = repository.findByName(name);

        if(infoOptional.isPresent()){
            return ResponseEntity.ok(infoOptional.get());
        }else{
            return ResponseEntity.noContent().build();
        }
    }

    @GetMapping("/api/info/byId/{id}")
    public ResponseEntity<?> getConnectionInfoById(@PathVariable Long id){
        Optional<CertAuthorityConnectionInfo> infoOptional = repository.findById(id);

        if(infoOptional.isPresent()){
            return ResponseEntity.ok(infoOptional.get());
        }

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/info/all")
    public ResponseEntity<?> getAllConnectionInfo(){
        List<CertAuthorityConnectionInfo> list = repository.findAll();

        return ResponseEntity.ok(list);
    }

    @DeleteMapping("/api/info/delete/{id}")
    public ResponseEntity<?> deleteInfo(@PathVariable Long id){
        repository.deleteById(id);

        return ResponseEntity.ok().build();
    }



    @PostMapping("/issueCertificate/{connectionName}")
    public ResponseEntity<?> issueCertificate(@PathVariable String connectionName,
                                              @RequestParam String pkcs10, @RequestParam(required = false) String dnsNames){
        //todo issue through here, not /ca/internal

        CertAuthority certAuthority = loadedCertAuthorities.get(connectionName);
        if(certAuthority != null){
            SubjectAltNames subjectAltNames = null;
            if(StringUtils.isNotBlank(dnsNames)){
                subjectAltNames = new SubjectAltNames();
                for(String dnsName : dnsNames.split(",")){
                    subjectAltNames.addValue(SubjectAltNames.SubjAltNameType.DNS, dnsName);
                }
            }

            X509Certificate cert = certAuthority.issueCertificate(pkcs10, subjectAltNames);
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

    @GetMapping("/certDetails/{connectionName}")
    public ResponseEntity<?> getCertificateStatus(@PathVariable String connectionName, @RequestParam String serial){

        CertAuthority certAuthority = loadedCertAuthorities.get(connectionName);
        if(certAuthority != null) {
            CertificateDetails details = new CertificateDetails();
            X509Certificate cert = certAuthority.getCertificateBySerial(serial);
            if(cert != null){
                String status = certAuthority.getCertificateStatus(serial);
                try {
                    details.setCertificateBase64(CertUtil.convertToPem(cert));
                    details.setStatus(status);
                    details.setSerial(serial);

                    return ResponseEntity.ok(details);
                } catch (CertificateEncodingException e) {
                    e.printStackTrace();
                    return ResponseEntity.status(500).build();
                }
            }
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


    @PostMapping("/search/{connectionName}")
    public ResponseEntity<?> search(@PathVariable String connectionName, @RequestBody CertSearchParam certSearchParam){
        CertAuthority certAuthority = loadedCertAuthorities.get(connectionName);

        /*
        CertSearchParam param = new CertSearchParam(CertSearchParams.CertSearchParamRelation.AND);

        CertSearchParam sub1 = new CertSearchParam(CertSearchParams.CertField.SUBJECT, "te", CertSearchParams.CertSearchParamRelation.CONTAINS);
        CertSearchParam sub2 = new CertSearchParam(CertSearchParams.CertField.SUBJECT, "st", CertSearchParams.CertSearchParamRelation.CONTAINS);

        param.addSearchParam(sub1);
        param.addSearchParam(sub2);

        CertSearchParam param2 = new CertSearchParam(CertSearchParams.CertSearchParamRelation.OR);
        param2.addSearchParam(param);
        param2.addSearchParam(sub1);

        List<CertificateDetails> temp1 = certAuthority.search(param2);

        List<CertificateDetails> temp2 = certAuthority.search(sub1);
        */
        //todo clean this up

        return ResponseEntity.ok(certAuthority.search(certSearchParam));
    }

    public Optional<CertAuthority> getCertAuthorityByName(String name){
        CertAuthority ca = loadedCertAuthorities.get(name);

        if(ca != null){
            return Optional.of(ca);
        }else{
            return Optional.empty();
        }
    }

    public List<CertAuthority> getAllCertAuthorities(){
        return new ArrayList<>(loadedCertAuthorities.values());
    }

    private Optional<CertAuthority> buildCertAuthority(String connectionName){
        CertAuthority certAuthority = null;
        Optional<CertAuthorityConnectionInfo> infoOptional = repository.findByName(connectionName);

        if(infoOptional.isPresent()) {
            CertAuthorityConnectionInfo info = infoOptional.get();
            if (info.getType().contentEquals("internal")) {
                certAuthority = new InternalCertAuthority(info);
            }

            if (certAuthority != null) {
                return Optional.of(certAuthority);
            }
        }
        return Optional.empty();
    }
}
