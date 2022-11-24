package com.winllc.pki.ra.service;

import com.winllc.acme.common.domain.Account;
import com.winllc.pki.ra.beans.form.AttributePolicyGroupForm;
import com.winllc.pki.ra.beans.search.GridFilterModel;
import com.winllc.pki.ra.cron.LdapObjectUpdater;
import com.winllc.acme.common.domain.*;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.acme.common.repository.*;
import com.winllc.pki.ra.service.validators.AttributePolicyGroupValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;
import javax.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/attributePolicy")
public class AttributePolicyService extends UpdatedDataPagedService<AttributePolicyGroup,
        AttributePolicyGroupForm, AttributePolicyGroupRepository> {

    private static final Logger log = LogManager.getLogger(AttributePolicyService.class);

    private final AccountRepository accountRepository;
    private final PocEntryRepository pocEntryRepository;
    private final AttributePolicyGroupRepository attributePolicyGroupRepository;
    private final AttributePolicyRepository attributePolicyRepository;
    private final LdapSchemaOverlayRepository ldapSchemaOverlayRepository;
    //todo integrate this with attribute policy
    private final SecurityPolicyService securityPolicyService;
    private final AttributePolicyGroupValidator attributePolicyGroupValidator;

    private final LdapObjectUpdater ldapObjectUpdater;

    public AttributePolicyService(ApplicationContext context,
                                  AccountRepository accountRepository, PocEntryRepository pocEntryRepository,
                                  AttributePolicyGroupRepository attributePolicyGroupRepository,
                                  AttributePolicyRepository attributePolicyRepository,
                                  SecurityPolicyService securityPolicyService,
                                  AttributePolicyGroupValidator attributePolicyGroupValidator,
                                  LdapSchemaOverlayRepository ldapSchemaOverlayRepository,
                                  LdapObjectUpdater ldapObjectUpdater) {
        super(context, AttributePolicyGroup.class, attributePolicyGroupRepository);
        this.accountRepository = accountRepository;
        this.pocEntryRepository = pocEntryRepository;
        this.attributePolicyGroupRepository = attributePolicyGroupRepository;
        this.attributePolicyRepository = attributePolicyRepository;
        this.securityPolicyService = securityPolicyService;
        this.attributePolicyGroupValidator = attributePolicyGroupValidator;
        this.ldapSchemaOverlayRepository = ldapSchemaOverlayRepository;
        this.ldapObjectUpdater = ldapObjectUpdater;
    }

    @InitBinder("attributePolicyGroupForm")
    public void initAppKeystoreEntryBinder(WebDataBinder binder) {
        binder.setValidator(attributePolicyGroupValidator);
    }

    /*
    @GetMapping("/policyService/connectionNames")
    @ResponseStatus(HttpStatus.OK)
    public List<String> getSecurityPolicyServiceNames(){
        List<SecurityPolicyConnection> allConnections = securityPolicyService.getAllConnections();
        if(!CollectionUtils.isEmpty(allConnections)){
            return allConnections.stream()
                    .map(c -> c.getConnectionName())
                    .collect(Collectors.toList());
        }else{
            return new ArrayList<>();
        }
    }

     */

    @GetMapping("/group/byId/{id}")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public AttributePolicyGroupForm findPolicyGroupById(@PathVariable Long id) throws RAObjectNotFoundException {
        Optional<AttributePolicyGroup> policyGroupOptional = attributePolicyGroupRepository.findById(id);
        if(policyGroupOptional.isPresent()){
            AttributePolicyGroup apg = policyGroupOptional.get();
            Hibernate.initialize(apg.getAttributePolicies());
            return new AttributePolicyGroupForm(apg);
        }else{
            throw new RAObjectNotFoundException(AttributePolicyGroup.class, id);
        }
    }

    @GetMapping("/group/byId/{id}/schemaOverlay")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public Map<String, Boolean> findSchemaOverlayForGroup(@PathVariable Long id) throws RAObjectNotFoundException {
        Optional<AttributePolicyGroup> policyGroupOptional = attributePolicyGroupRepository.findById(id);
        if(policyGroupOptional.isPresent()){
            AttributePolicyGroup apg = policyGroupOptional.get();
            LdapSchemaOverlay overlay = apg.getLdapSchemaOverlay();
            Hibernate.initialize(overlay.getAttributeMap());
            return overlay.getAttributeMap().stream()
                    .collect(Collectors.toMap(a -> a.getName(), a -> a.getEnabled()));
        }else{
            throw new RAObjectNotFoundException(AttributePolicyGroup.class, id);
        }
    }

    @GetMapping("/group/all")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public List<AttributePolicyGroupForm> allAttributePolicyGroups(){
        List<AttributePolicyGroup> accountPolicyGroups = attributePolicyGroupRepository.findAll();
        List<AttributePolicyGroupForm> forms = accountPolicyGroups.stream()
                .map(apg -> new AttributePolicyGroupForm(apg))
                .collect(Collectors.toList());
        return forms;
    }

    @GetMapping("/group/my")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public List<AttributePolicyGroupForm> myAttributePolicyGroups(@AuthenticationPrincipal UserDetails raUser){
        List<PocEntry> allByEmailEquals = pocEntryRepository.findAllByEmailEquals(raUser.getUsername());
        List<Account> allUserAccounts = accountRepository.findAllByPocsIn(allByEmailEquals);

        List<AttributePolicyGroupForm> groups = new ArrayList<>();
        for(Account account : allUserAccounts){
            List<AttributePolicyGroup> accountPolicyGroups = attributePolicyGroupRepository.findAllByAccount(account);
            List<AttributePolicyGroupForm> forms = accountPolicyGroups.stream()
                    .map(apg -> new AttributePolicyGroupForm(apg))
                    .collect(Collectors.toList());
            groups.addAll(forms);
        }

        return groups;
    }

    @PostMapping("/group/create")
    @ResponseStatus(HttpStatus.CREATED)
    public Long createGroupPolicyGroup(@Valid @RequestBody AttributePolicyGroupForm form)
            throws RAObjectNotFoundException {

        Optional<Account> optionalAccount = accountRepository.findById(form.getAccountId());
        Optional<LdapSchemaOverlay> optionalSchema = ldapSchemaOverlayRepository.findById(form.getAttributeSchemaId());

        if(optionalAccount.isPresent() && optionalSchema.isPresent()) {
            Account account = optionalAccount.get();
            LdapSchemaOverlay schemaOverlay = optionalSchema.get();

            AttributePolicyGroup attributePolicyGroup = new AttributePolicyGroup();
            attributePolicyGroup.setName(form.getName());
            attributePolicyGroup.setAccount(account);
            attributePolicyGroup.setLdapSchemaOverlay(schemaOverlay);

            attributePolicyGroup = attributePolicyGroupRepository.save(attributePolicyGroup);

            if (!CollectionUtils.isEmpty(form.getAttributePolicies())) {
                for(AttributePolicy ap : form.getAttributePolicies()){
                    ap.setAttributePolicyGroup(attributePolicyGroup);
                    ap = attributePolicyRepository.save(ap);
                    attributePolicyGroup.getAttributePolicies().add(ap);
                }
            }
            return attributePolicyGroup.getId();
        }else{
            throw new RAObjectNotFoundException(Account.class, form.getAccountId());
        }
    }

    @PostMapping("/group/update")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public AttributePolicyGroup updateGroupPolicyGroup(@Valid @RequestBody AttributePolicyGroupForm form) throws RAObjectNotFoundException {
        AttributePolicyGroup apg = attributePolicyGroupRepository.findById(form.getId())
                .orElseThrow(() -> new RAObjectNotFoundException(AttributePolicyGroup.class, form.getId()));

        //todo generify the process of updating a set of child objects given the existing and updated lists
        //todo use an Updateable interface on the domain object with the update method
        final Set<AttributePolicy> existing = apg.getAttributePolicies();
        final Map<Long, AttributePolicy> updated = form.getAttributePolicies().stream()
                .filter(ep -> ep.getId() != null)
                .collect(Collectors.toMap(ep -> ep.getId(), ep -> ep));
        List<AttributePolicy> newPolicies = form.getAttributePolicies().stream()
                .filter(ep -> ep.getId() == null)
                .collect(Collectors.toList());

        Map<Boolean, List<AttributePolicy>> updateDeleteMap = existing.stream()
                .collect(Collectors.groupingBy(e -> updated.containsKey(e.getId())));

        List<AttributePolicy> toUpdate = updateDeleteMap.get(true);
        List<AttributePolicy> toDelete = updateDeleteMap.get(false);

        Set<AttributePolicy> attributePolicies = new HashSet<>();

        if(toDelete != null) toDelete.forEach(ap -> attributePolicyRepository.delete(ap));
        if(toUpdate != null) {
            AttributePolicyGroup finalApg = apg;
            toUpdate.forEach(ap -> {
                ap.update(updated.get(ap.getId()));
                ap.setAttributePolicyGroup(finalApg);
                attributePolicies.add(attributePolicyRepository.save(ap));
            });
        }
        AttributePolicyGroup finalApg1 = apg;
        newPolicies.forEach(np -> {
            np.setAttributePolicyGroup(finalApg1);
            attributePolicies.add(attributePolicyRepository.save(np));;
        });

        apg.setAttributePolicies(attributePolicies);
        apg = attributePolicyGroupRepository.save(apg);

        ldapObjectUpdater.update(apg);

        //return new AttributePolicyGroupForm(apg);
        return apg;
    }

    @DeleteMapping("/group/delete/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteAttributePolicyGroup(@PathVariable Long id){
        attributePolicyGroupRepository.deleteById(id);
    }




    @Override
    protected void postSave(AttributePolicyGroup entity, AttributePolicyGroupForm form) {

    }

    @Override
    protected AttributePolicyGroupForm entityToForm(AttributePolicyGroup entity, Authentication authentication) {
        return new AttributePolicyGroupForm(entity);
    }

    @Override
    protected AttributePolicyGroup formToEntity(AttributePolicyGroupForm form, Map<String, String> params, Authentication authentication) throws Exception {
        Account account = accountRepository.findById(form.getAccountId())
                .orElseThrow(() -> new RAObjectNotFoundException(Account.class, form.getAccountId()));

        AttributePolicyGroup attributePolicyGroup = updateGroupPolicyGroup(form);
        return attributePolicyGroup;
    }

    @Override
    protected AttributePolicyGroup combine(AttributePolicyGroup original, AttributePolicyGroup updated, Authentication authentication) throws Exception {
        //todo
        return original;
    }

    @Override
    public List<Predicate> buildFilter(Map<String, String> allRequestParams, GridFilterModel filterModel, Root<AttributePolicyGroup> root, CriteriaQuery<?> query, CriteriaBuilder cb, Authentication authentication) {
        return null;
    }


}
