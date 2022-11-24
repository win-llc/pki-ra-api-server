package com.winllc.pki.ra.service;

import com.winllc.acme.common.domain.EstServerProperties;
import com.winllc.acme.common.repository.EstServerPropertiesRepository;
import com.winllc.pki.ra.beans.form.EstServerPropertiesForm;
import com.winllc.pki.ra.beans.search.GridFilterModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/estServerManagement")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class EstServerManagementService extends UpdatedDataPagedService<EstServerProperties,
        EstServerPropertiesForm, EstServerPropertiesRepository> {

    protected EstServerManagementService(ApplicationContext context,
                                         EstServerPropertiesRepository repository) {
        super(context, EstServerProperties.class, repository);
    }

    @GetMapping("/properties/byName/{name}")
    public EstServerProperties getProperties(@PathVariable String name){
        return getRepository().findByName(name);
    }

    @GetMapping("/properties/all")
    public List<EstServerProperties> getAll(){
        return getRepository().findAll();
    }

    @PostMapping("/properties/save")
    public EstServerProperties save(@RequestBody EstServerProperties properties){
        return getRepository().save(properties);
    }

    @DeleteMapping("/properties/delete/{name}")
    public void delete(@PathVariable String name){
        EstServerProperties properties = getRepository().findByName(name);
        if(properties != null){
            getRepository().delete(properties);
        }
    }



    @Override
    protected void postSave(EstServerProperties entity, EstServerPropertiesForm form) {

    }

    @Override
    protected EstServerPropertiesForm entityToForm(EstServerProperties entity, Authentication authentication) {
        return new EstServerPropertiesForm(entity);
    }

    @Override
    protected EstServerProperties formToEntity(EstServerPropertiesForm form, Map<String, String> params, Authentication authentication) throws Exception {
        EstServerProperties estServerProperties = new EstServerProperties();
        estServerProperties.setName(form.getName());
        estServerProperties.setCaConnectionName(form.getCaConnectionName());
        estServerProperties.setCreationDate(Date.valueOf(LocalDate.now()));
        return estServerProperties;
    }

    @Override
    protected EstServerProperties combine(EstServerProperties original, EstServerProperties updated, Authentication authentication) throws Exception {
        //todo
        return original;
    }

    @Override
    public List<Predicate> buildFilter(Map<String, String> allRequestParams, GridFilterModel filterModel, Root<EstServerProperties> root, CriteriaQuery<?> query, CriteriaBuilder cb, Authentication authentication) {
        return null;
    }

}
