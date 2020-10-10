package com.winllc.pki.ra.service;

import com.winllc.pki.ra.beans.form.UniqueEntityLookupForm;
import com.winllc.pki.ra.beans.info.InfoObject;
import com.winllc.pki.ra.constants.AuditRecordType;
import com.winllc.pki.ra.domain.AuditRecord;
import com.winllc.pki.ra.domain.UniqueEntity;
import com.winllc.pki.ra.repository.AuditRecordRepository;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/auditRecord")
public class AuditRecordService {

    private final AuditRecordRepository repository;

    public AuditRecordService(AuditRecordRepository repository) {
        this.repository = repository;
    }

    public void save(AuditRecord auditRecord){
        repository.save(auditRecord);
    }

    @GetMapping("/byType/{type}")
    @ResponseStatus(HttpStatus.OK)
    public Page<AuditRecord> getPagedRecordsByType(@PathVariable String type, Pageable pageable){
        AuditRecordType auditRecordType = AuditRecordType.valueOf(type);
        return repository.findAllByType(auditRecordType, pageable);
    }

    @PostMapping("/all")
    @ResponseStatus(HttpStatus.OK)
    public Page<AuditRecord> getAll(Pageable pageable){

        return repository.findAll(pageable);
    }

    @PostMapping("/forEntity")
    @ResponseStatus(HttpStatus.OK)
    public Page<AuditRecord> getRecordsForEntity(@Valid @RequestBody UniqueEntityLookupForm lookupForm, Pageable pageable){
        return repository.findAllByObjectClassAndObjectUuid(lookupForm.getObjectClass(), lookupForm.getObjectUuid(), pageable);
    }
}
