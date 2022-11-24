package com.winllc.pki.ra.service;



import com.winllc.acme.common.domain.*;
import com.winllc.acme.common.repository.AccountRepository;
import com.winllc.acme.common.repository.PagingRepository;
import com.winllc.acme.common.repository.ServerEntryRepository;
import com.winllc.pki.ra.beans.form.ValidForm;
import com.winllc.pki.ra.beans.search.*;
import lombok.Getter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.persistence.criteria.*;
import javax.persistence.metamodel.Attribute;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Transactional
@Getter
public abstract class UpdatedDataPagedService<T extends BaseEntity, F extends ValidForm<T>,
        R extends PagingRepository<T>> extends AbstractService
        implements UpdatedDataService<F> {

    private final Class<T> clazz;
    private final R repository;

    private final AccountRepository accountRepository;
    private final ServerEntryRepository serverEntryRepository;

    protected UpdatedDataPagedService(ApplicationContext context, Class<T> clazz, R repository) {
        super(context);
        this.clazz = clazz;
        this.repository = repository;

        accountRepository = context.getBean(AccountRepository.class);
        serverEntryRepository = context.getBean(ServerEntryRepository.class);
    }

    @Override
    @GetMapping("/all")
    public List<F> getAll(Authentication authentication) throws Exception {
        return repository.findAll().stream()
                .map(a -> entityToForm(a, authentication))
                .collect(Collectors.toList());
    }

    @Override
    @PostMapping("/paged")
    public Page<F> getPaged(@RequestParam Integer page,
                            @RequestParam Integer pageSize,
                            @RequestParam(defaultValue = "asc") String order,
                            @RequestParam(required = false) String sortBy,
                            @RequestParam Map<String, String> allRequestParams,
                            @RequestBody GridModel gridModel,
                            Authentication authentication) {

        return generatePage(page, pageSize, gridModel,
                allRequestParams, null, authentication);
    }

    @Override
    @PostMapping("/my/paged")
    public Page<F> getMyPaged(@RequestParam Integer page,
                              @RequestParam Integer pageSize,
                              @RequestParam(defaultValue = "asc") String order,
                              @RequestParam(required = false) String sortBy,
                              @RequestParam Map<String, String> allRequestParams,
                              @RequestBody GridModel gridModel,
                              Authentication authentication) {

        return generatePage(page, pageSize, gridModel, allRequestParams,
                authentication.getName(), authentication);
    }

    @Override
    @GetMapping("/id/{id}")
    public F findRest(@PathVariable Long id, Authentication authentication) throws Exception {
        T byId = repository.findById(id).orElseThrow(() -> new Exception(id+" not found"));
        return entityToForm(byId, authentication);
    }

    @Override
    @PostMapping("/add")
    public F addRest(@Valid @RequestBody F entity, @RequestParam Map<String, String> allRequestParams,
                     Authentication authentication) throws Exception {
        return add(entity, allRequestParams, authentication);
    }

    @Override
    @PostMapping("/update")
    public F updateRest(@Valid @RequestBody F entity, @RequestParam Map<String, String> allRequestParams,
                        Authentication authentication) throws Exception {
        return update(entity, allRequestParams, authentication);
    }

    @Override
    @PostMapping("/delete/{id}")
    public void deleteRest(@PathVariable Long id, @RequestBody F form, Authentication authentication) throws Exception {
        delete(id, form, authentication);
    }

    public F add(F form, Map<String, String> allRequestParams, Authentication authentication) throws Exception {
        T entity = formToEntity(form, allRequestParams, authentication);
        entity = save(entity, form);
        return entityToForm(entity, authentication);
    }

    protected F update(F form, Map<String, String> allRequestParams, Authentication authentication) throws Exception {
        if(form.isNewRecord()){
            return add(form, allRequestParams, authentication);
        }else {
            T entity = repository.findById(form.getId()).orElseThrow(() -> new Exception(form.getId() + " not found"));
            T newEntity = formToEntity(form, allRequestParams, authentication);
            newEntity = combine(entity, newEntity, authentication);
            entity = save(newEntity, form);
            return entityToForm(entity, authentication);
        }
    }

    protected void delete(Long id, F form, Authentication authentication) throws Exception{
        repository.deleteById(id);
    }

    private T save(T entity, F form){
        T saved = repository.save(entity);
        postSave(entity, form);
        return saved;
    }

    protected abstract void postSave(T entity, F form);

    protected abstract F entityToForm(T entity, Authentication authentication);
    protected abstract T formToEntity(F form, Map<String, String> params, Authentication authentication) throws Exception;
    protected abstract T combine(T original, T updated, Authentication authentication) throws Exception;


    public Page<F> generatePage(Integer page, Integer pageSize,
                                GridModel gridModel,
                                Map<String, String> allRequestParams, String forEmail,
                                Authentication authentication){
        try {
            Pageable pageable;
            if(page == -1 || pageSize == -1) {
                pageable = Pageable.unpaged();
            } else if(gridModel.firstSortItem().isPresent()){
                GridSortItem sortItem = gridModel.firstSortItem().get();

                Sort.Direction direction = sortItem.getSort() == GridSortDirection.asc ? Sort.Direction.ASC : Sort.Direction.DESC;
                String sort = StringUtils.isNotBlank(sortItem.getField()) ? sortItem.getField() : "id";

                pageable = PageRequest.of(page, pageSize, direction, sort);
            }else{
                pageable = PageRequest.of(page, pageSize);
            }

            Specification<T> spec = buildSpec(allRequestParams, gridModel.getFilterModel(),
                    forEmail, authentication);

            Page<T> all;
            if (spec != null) {
                all = repository.findAll(spec, pageable);
            } else {
                all = repository.findAll(pageable);
            }

            List<F> forms = all.getContent().stream()
                    .map((T entity) -> entityToForm(entity, authentication))
                    .collect(Collectors.toList());

            return new PageImpl<>(forms, all.getPageable(), all.getTotalElements());
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }


    public Specification<T> buildSpec(Map<String, String> allRequestParams,
                                      GridFilterModel filterModel,
                                      String forEmail, Authentication authentication){
        return (root, query, cb) -> {
            List<Predicate> list = new ArrayList<Predicate>();

            //Build custom filter
            List<Predicate> predicates = buildFilter(allRequestParams, filterModel,
                    root, query, cb, authentication);
            if(CollectionUtils.isNotEmpty(predicates)){
                list.addAll(predicates);
            }

            //If a child element, create filter for parent
            List<Predicate> childPredicates = buildChildFilter(root, cb, allRequestParams);
            if(CollectionUtils.isNotEmpty(childPredicates)){
                list.addAll(childPredicates);
            }

            //Build common filter
            if(filterModel != null) {
                //Column search
                if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(filterModel.getItems())) {
                    for (GridFilterItem filterItem : filterModel.getItems()) {
                        Predicate predicate = filterItem.toPredicate(root, cb);
                        if (predicate != null) {
                            list.add(predicate);
                        }
                    }
                }

                //Quick search filter
                if (filterModel.firstQuickFilter().isPresent()) {
                    String text = filterModel.firstQuickFilter().get();
                    String finalText = "%" + text.toLowerCase() + "%";

                    List<String> stringAttrs = root.getModel().getAttributes().stream()
                            .filter(a -> a.getJavaType() == String.class)
                            .map(Attribute::getName).toList();

                    List<Predicate> searchPredicates = new ArrayList<>();
                    for (String search : stringAttrs) {
                        searchPredicates.add(cb.like(cb.lower(root.get(search)), finalText));
                    }

                    if (searchPredicates.size() > 0) {
                        Predicate orSearch = cb.or(searchPredicates.toArray(new Predicate[0]));
                        list.add(orSearch);
                    }
                }
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

    public abstract List<Predicate> buildFilter(Map<String, String> allRequestParams,
                                                GridFilterModel filterModel,
                                                Root<T> root, CriteriaQuery<?> query,
                                                CriteriaBuilder cb,
                                                Authentication authentication);

    public List<Predicate> buildMyFilter(String email, CriteriaQuery<?> query, CriteriaBuilder cb){
        List<Predicate> list = new ArrayList<>();
        query.distinct(true);
        Root<T> fromUpdates = query.from(clazz);
        Join<T, Account> details = fromUpdates.join("account");
        Join<Account, PocEntry> associate = details.join("pocs");

        list.add(cb.equal(associate.get("email"), email));

        return list;
    }

    protected List<Predicate> buildChildFilter(Root<T> root, CriteriaBuilder cb,
                                               Map<String, String> allRequestParams){
        Optional<String> parentEntityType = getParamValue("parentEntityType", allRequestParams);
        Optional<String> parentEntityId = getParamValue("parentEntityId", allRequestParams);

        if(parentEntityType.isPresent() && parentEntityId.isPresent()) {

            String type = parentEntityType.get();
            Long id = Long.valueOf(parentEntityId.get());

            Predicate joinPredicate;
            if (type.equalsIgnoreCase("account")) {
                Join<PocEntry, Account> servers = root.join("account");
                joinPredicate = cb.equal(servers.get("id"), id);
            } else {
                Join<PocEntry, ServerEntry> servers = root.join("serverEntry");
                joinPredicate = cb.equal(servers.get("id"), id);
            }

            return Stream.of(joinPredicate).collect(Collectors.toList());
        } else {
            return null;
        }
    }

    protected Optional<AuthCredentialHolderInteface> getParentObject(Map<String, String> allRequestParams){
        Optional<String> parentEntityType = getParamValue("parentEntityType", allRequestParams);
        Optional<String> parentEntityId = getParamValue("parentEntityId", allRequestParams);

        if(parentEntityType.isPresent() && parentEntityId.isPresent()) {
            String type = parentEntityType.get();
            Long id = Long.valueOf(parentEntityId.get());

            if (type.equalsIgnoreCase("account")) {
                Optional<Account> byId = accountRepository.findById(id);
                return Optional.of(byId.get());
            } else if(type.equalsIgnoreCase("server")) {
                Optional<ServerEntry> byId = serverEntryRepository.findById(id);
                return Optional.of(byId.get());
            } else {
                return Optional.empty();
            }
        }else{
            return Optional.empty();
        }
    }

    protected Optional<String> getParamValue(String param, Map<String, String> params){
        if(params != null && params.containsKey(param)){
            String value = params.get(param);
            if(StringUtils.isNotBlank(value)) {
                return Optional.of(value);
            }else{
                return Optional.empty();
            }
        }else{
            return Optional.empty();
        }
    }
}
