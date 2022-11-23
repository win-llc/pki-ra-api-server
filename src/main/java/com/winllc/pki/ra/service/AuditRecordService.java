package com.winllc.pki.ra.service;

import com.winllc.acme.common.domain.Account;
import com.winllc.acme.common.domain.AccountRestriction;
import com.winllc.pki.ra.beans.form.AccountRestrictionForm;
import com.winllc.pki.ra.beans.form.AuditRecordForm;
import com.winllc.pki.ra.beans.form.UniqueEntityLookupForm;
import com.winllc.acme.common.constants.AuditRecordType;
import com.winllc.acme.common.domain.AuditRecord;
import com.winllc.acme.common.repository.AuditRecordRepository;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/auditRecord")
public class AuditRecordService extends DataPagedService<AuditRecord, AuditRecordForm, AuditRecordRepository> {

    private final AuditRecordRepository repository;

    public AuditRecordService(ApplicationContext context,
                              AuditRecordRepository repository) {
        super(context, AuditRecord.class, repository);
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
        Sort sort = Sort.by(Sort.Direction.DESC, "timestamp");
        if(!pageable.isUnpaged()) {
            Pageable withSort = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
            return repository.findAllByObjectClassAndObjectUuid(lookupForm.getObjectClass(), lookupForm.getObjectUuid(), withSort);
        }else{
            return repository.findAllByObjectClassAndObjectUuid(lookupForm.getObjectClass(), lookupForm.getObjectUuid(), pageable);
        }
    }

    @Override
    protected AuditRecordForm entityToForm(AuditRecord entity) {
        return new AuditRecordForm(entity);
    }

    @Override
    protected AuditRecord formToEntity(AuditRecordForm form, Authentication authentication) throws Exception {
        return null;
    }

    @Override
    protected AuditRecord combine(AuditRecord original, AuditRecord updated, Authentication authentication) throws Exception {
        return null;
    }

    @Override
    public List<Predicate> buildFilter(Map<String, String> allRequestParams, Root<AuditRecord> root, CriteriaQuery<?> query, CriteriaBuilder cb) {

        String type = allRequestParams.get("type");
        String id = allRequestParams.get("id");

        Predicate typeEqual = cb.equal(root.get("objectClass"), type);
        Predicate uuidEqual = cb.equal(root.get("objectUuid"), id);

        return Stream.of(typeEqual, uuidEqual).collect(Collectors.toList());
    }


}
