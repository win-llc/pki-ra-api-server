package com.winllc.pki.ra.service;

import com.winllc.acme.common.domain.*;
import com.winllc.acme.common.repository.PagingRepository;
import com.winllc.pki.ra.beans.form.ValidForm;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import lombok.Getter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.persistence.criteria.*;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Transactional
@Getter
public abstract class DataPagedService<T extends BaseEntity, F extends ValidForm<T>,
        R extends PagingRepository<T>> extends AbstractService
        implements DataService<F> {

    private final Class<T> clazz;
    private final R repository;

    protected DataPagedService(ApplicationContext context, Class<T> clazz, R repository) {
        super(context);
        this.clazz = clazz;
        this.repository = repository;
    }

    @Override
    @GetMapping("/all")
    public List<F> getAll(Authentication authentication) throws RAObjectNotFoundException {
        return repository.findAll().stream()
                .map(this::entityToForm)
                .collect(Collectors.toList());
    }

    @Override
    @GetMapping("/paged")
    public Page<F> getPaged(@RequestParam Integer page,
                            @RequestParam Integer pageSize,
                            @RequestParam(defaultValue = "asc") String order,
                            @RequestParam(required = false) String sortBy,
                            @RequestParam Map<String, String> allRequestParams) {

        return generatePage(page, pageSize, order, sortBy, allRequestParams, null);
    }

    @Override
    @GetMapping("/my/paged")
    public Page<F> getMyPaged(@RequestParam Integer page,
                              @RequestParam Integer pageSize,
                              @RequestParam(defaultValue = "asc") String order,
                              @RequestParam(required = false) String sortBy,
                              @RequestParam Map<String, String> allRequestParams,
                              Authentication authentication) {

        return generatePage(page, pageSize, order, sortBy, allRequestParams, authentication.getName());
    }

    @Override
    @GetMapping("/id/{id}")
    public F findRest(@PathVariable Long id, Authentication authentication) throws Exception {
        T byId = repository.findById(id).orElseThrow(() -> new RAObjectNotFoundException(clazz, id));
        return entityToForm(byId);
    }

    @Override
    @PostMapping("/add")
    public F addRest(@Valid @RequestBody F entity, BindingResult bindingResult, Authentication authentication) throws Exception {
        return add(entity, authentication);
    }

    @Override
    @PostMapping("/update")
    public F updateRest(@Valid @RequestBody F entity, Authentication authentication) throws Exception {
        return update(entity, authentication);
    }

    @Override
    @DeleteMapping("/delete/{id}")
    public void deleteRest(@PathVariable Long id, Authentication authentication) throws RAObjectNotFoundException {
        delete(id, authentication);
    }

    public F add(F form, Authentication authentication) throws Exception {
        T entity = formToEntity(form, authentication);
        entity = repository.save(entity);
        return entityToForm(entity);
    }

    public F update(F form, Authentication authentication) throws Exception {
        T entity = repository.findById(form.getId()).orElseThrow(() -> new RAObjectNotFoundException(form));
        T newEntity = formToEntity(form, authentication);
        newEntity = combine(entity, newEntity, authentication);
        entity = repository.save(newEntity);
        return entityToForm(entity);
    }

    public void delete(Long id, Authentication authentication) throws RAObjectNotFoundException{
        repository.deleteById(id);
    }

    protected abstract F entityToForm(T entity);
    protected abstract T formToEntity(F form, Authentication authentication) throws Exception;
    protected abstract T combine(T original, T updated, Authentication authentication) throws Exception;


    public Page<F> generatePage(Integer page, Integer pageSize, String order,
                                String sortBy, Map<String, String> allRequestParams, String forEmail){
        try {
            Sort.Direction direction = order.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
            String sort = StringUtils.isNotBlank(sortBy) ? sortBy : "id";

            Pageable pageable = PageRequest.of(page, pageSize, direction, sort);

            Specification<T> spec = buildSpec(allRequestParams, forEmail);

            Page<T> all;
            if (spec != null) {
                all = repository.findAll(spec, pageable);

            } else {
                all = repository.findAll(pageable);
            }

            List<F> forms = all.getContent().stream()
                    .map((T entity) -> entityToForm(entity))
                    .collect(Collectors.toList());

            return new PageImpl<>(forms, all.getPageable(), all.getTotalElements());
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }


    public Specification<T> buildSpec(Map<String, String> allRequestParams, String forEmail){
        return (root, query, cb) -> {
            List<Predicate> list = new ArrayList<Predicate>();

            List<Predicate> predicates = buildFilter(allRequestParams, root, query, cb);
            if(CollectionUtils.isNotEmpty(predicates)){
                list.addAll(predicates);
            }

            if(StringUtils.isNotBlank(forEmail)){
                List<Predicate> myPredicates = buildMyFilter(forEmail, query, cb);
                if(CollectionUtils.isNotEmpty(myPredicates)){
                    list.addAll(myPredicates);
                }
            }

            return cb.and(list.toArray(new Predicate[0]));
        };
    }

    public abstract List<Predicate> buildFilter(Map<String, String> allRequestParams, Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb);

    public List<Predicate> buildMyFilter(String email, CriteriaQuery<?> query, CriteriaBuilder cb){
        List<Predicate> list = new ArrayList<>();
        query.distinct(true);
        Root<T> fromUpdates = query.from(clazz);
        Join<T, Account> details = fromUpdates.join("account");
        Join<Account, PocEntry> associate = details.join("pocs");

        list.add(cb.equal(associate.get("email"), email));

        return list;
    }
}
