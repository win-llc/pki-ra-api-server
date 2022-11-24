package com.winllc.pki.ra.service;

import com.winllc.acme.common.domain.LdapSchemaOverlay;
import com.winllc.acme.common.domain.LdapSchemaOverlayAttribute;
import com.winllc.pki.ra.beans.form.LdapSchemaOverlayForm;
import com.winllc.pki.ra.beans.search.GridFilterModel;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.acme.common.repository.LdapSchemaOverlayAttributeRepository;
import com.winllc.acme.common.repository.LdapSchemaOverlayRepository;
import org.apache.commons.collections.CollectionUtils;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Transactional
@RestController
@RequestMapping("/api/ldapSchemaOverlay")
public class LdapSchemaOverlayService extends
        UpdatedDataPagedService<LdapSchemaOverlay, LdapSchemaOverlayForm, LdapSchemaOverlayRepository> {

    private final LdapSchemaOverlayRepository repository;
    private final LdapSchemaOverlayAttributeRepository attributeRepository;

    public LdapSchemaOverlayService(ApplicationContext context,
            LdapSchemaOverlayRepository repository,
                                    LdapSchemaOverlayAttributeRepository attributeRepository) {
        super(context, LdapSchemaOverlay.class, repository);
        this.repository = repository;
        this.attributeRepository = attributeRepository;
    }

    @GetMapping("/getAll")
    @Transactional
    public List<LdapSchemaOverlay> getAll(){
        return repository.findAll().stream()
                .peek(o -> Hibernate.initialize(o.getAttributeMap()))
                .collect(Collectors.toList());
    }

    @GetMapping("/getOptions")
    public Map<Long, String> getOptions(){
        return repository.findAll().stream()
                .collect(Collectors.toMap(o -> o.getId(), o -> o.getLdapObjectType()));
    }

    @GetMapping("/byId/{id}")
    @Transactional
    public LdapSchemaOverlay findById(@PathVariable Long id) throws RAObjectNotFoundException {
        Optional<LdapSchemaOverlay> overlayOptional = repository.findById(id);
        if(overlayOptional.isPresent()){
            LdapSchemaOverlay overlay = overlayOptional.get();
            Hibernate.initialize(overlay.getAttributeMap());
            List<LdapSchemaOverlayAttribute> attributes
                    = attributeRepository.findAllByLdapSchemaOverlay(overlay);
            overlay.setAttributeMap(new HashSet<>(attributes));
            return overlay;
        }else{
            throw new RAObjectNotFoundException(LdapSchemaOverlay.class, id);
        }
    }

    @PostMapping("/create")
    @Transactional
    public Long create(@RequestBody LdapSchemaOverlay form){

        Set<LdapSchemaOverlayAttribute> attrs = new HashSet<>(form.getAttributeMap());
        form.getAttributeMap().clear();

        LdapSchemaOverlay saved = repository.save(form);
        applyFormAttributes(saved, attrs);

        return saved.getId();
    }

    //@PostMapping("/update")
    @Transactional
    public Long update(@RequestBody LdapSchemaOverlay form) throws RAObjectNotFoundException {
        Optional<LdapSchemaOverlay> overlayOptional = repository.findById(form.getId());
        if(overlayOptional.isPresent()){
            LdapSchemaOverlay overlay = overlayOptional.get();
            overlay = repository.save(overlay);

            applyFormAttributes(overlay, form.getAttributeMap());

            return overlay.getId();
        }else{
            throw new RAObjectNotFoundException(LdapSchemaOverlay.class, form.getId());
        }
    }

    private void applyFormAttributes(LdapSchemaOverlay saved, Set<LdapSchemaOverlayAttribute> attrs){
        Set<LdapSchemaOverlayAttribute> newAttrs = new HashSet<>();
        if(CollectionUtils.isNotEmpty(attrs)){
            for(LdapSchemaOverlayAttribute attribute : attrs){
                LdapSchemaOverlayAttribute toSave;
                if(attribute.getId() != null){
                    toSave = attributeRepository.findById(attribute.getId()).get();
                }else{
                    attribute.setLdapSchemaOverlay(saved);
                    toSave = attributeRepository.save(attribute);
                }
                newAttrs.add(toSave);
            }
        }

        Hibernate.initialize(saved.getAttributeMap());
        Set<LdapSchemaOverlayAttribute> existingAttrs = saved.getAttributeMap();

        List<LdapSchemaOverlayAttribute> toDelete = existingAttrs.stream()
                .filter(a -> !newAttrs.contains(a))
                .collect(Collectors.toList());

        for(LdapSchemaOverlayAttribute attr : toDelete) {
            //saved.getAttributeMap().remove(attr);
            attributeRepository.delete(attr);
        }

        saved.getAttributeMap().addAll(newAttrs);
        repository.save(saved);
    }


    @Override
    protected void postSave(LdapSchemaOverlay entity, LdapSchemaOverlayForm form) {

    }

    @Override
    protected LdapSchemaOverlayForm entityToForm(LdapSchemaOverlay entity, Authentication authentication) {
        return new LdapSchemaOverlayForm(entity);
    }

    @Override
    protected LdapSchemaOverlay formToEntity(LdapSchemaOverlayForm form, Map<String, String> params, Authentication authentication) throws Exception {
        LdapSchemaOverlay overlay = new LdapSchemaOverlay();
        overlay.setLdapObjectType(form.getLdapObjectType());
        overlay.setAttributeMap(form.getAttributeMap());
        return overlay;
    }

    @Override
    protected LdapSchemaOverlay combine(LdapSchemaOverlay original, LdapSchemaOverlay updated, Authentication authentication) throws Exception {
        //todo
        applyFormAttributes(original, updated.getAttributeMap());

        return original;
    }

    @Override
    public List<Predicate> buildFilter(Map<String, String> allRequestParams, GridFilterModel filterModel, Root<LdapSchemaOverlay> root, CriteriaQuery<?> query, CriteriaBuilder cb, Authentication authentication) {
        return null;
    }

}
