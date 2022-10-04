package com.winllc.pki.ra.service;

import com.winllc.acme.common.domain.LdapSchemaOverlay;
import com.winllc.acme.common.domain.LdapSchemaOverlayAttribute;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.acme.common.repository.LdapSchemaOverlayAttributeRepository;
import com.winllc.acme.common.repository.LdapSchemaOverlayRepository;
import org.apache.commons.collections.CollectionUtils;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ldapSchemaOverlay")
public class LdapSchemaOverlayService {

    private final LdapSchemaOverlayRepository repository;
    private final LdapSchemaOverlayAttributeRepository attributeRepository;

    public LdapSchemaOverlayService(LdapSchemaOverlayRepository repository, LdapSchemaOverlayAttributeRepository attributeRepository) {
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

    @PostMapping("/update")
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

    @DeleteMapping("/delete/{id}")
    @Transactional
    public ResponseEntity<?> deleteById(@PathVariable Long id){
        Optional<LdapSchemaOverlay> optionalOverlay = repository.findById(id);
        if(optionalOverlay.isPresent()){
            LdapSchemaOverlay overlay = optionalOverlay.get();

            repository.delete(overlay);
        }
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
