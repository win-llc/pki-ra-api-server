package com.winllc.pki.ra.service;

import com.winllc.pki.ra.beans.form.AttributePolicyGroupForm;
import com.winllc.pki.ra.domain.*;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.pki.ra.repository.AccountRepository;
import com.winllc.pki.ra.repository.AttributePolicyGroupRepository;
import com.winllc.pki.ra.repository.AttributePolicyRepository;
import com.winllc.pki.ra.repository.PocEntryRepository;
import com.winllc.pki.ra.service.external.SecurityPolicyService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.AbstractPersistable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import javax.validation.Valid;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/attributePolicy")
public class AttributePolicyService {

    private static final Logger log = LogManager.getLogger(AttributePolicyService.class);

    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private PocEntryRepository pocEntryRepository;
    @Autowired
    private AttributePolicyGroupRepository attributePolicyGroupRepository;
    @Autowired
    private AttributePolicyRepository attributePolicyRepository;
    //todo integrate this with attribute policy
    @Autowired
    private SecurityPolicyService securityPolicyService;

    @GetMapping("/group/byId/{id}")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public AttributePolicyGroupForm findPolicyGroupById(@PathVariable Long id) throws RAObjectNotFoundException {
        Optional<AttributePolicyGroup> policyGroupOptional = attributePolicyGroupRepository.findById(id);
        if(policyGroupOptional.isPresent()){
            AttributePolicyGroupForm form = new AttributePolicyGroupForm(policyGroupOptional.get());
            return form;
        }else{
            throw new RAObjectNotFoundException(AttributePolicyGroup.class, id);
        }
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

        if(optionalAccount.isPresent()) {
            Account account = optionalAccount.get();
            AttributePolicyGroup attributePolicyGroup = new AttributePolicyGroup();
            attributePolicyGroup.setName(form.getName());
            attributePolicyGroup.setAccount(account);
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
    public AttributePolicyGroupForm updateGroupPolicyGroup(@Valid @RequestBody AttributePolicyGroupForm form) throws RAObjectNotFoundException {
        Optional<AttributePolicyGroup> optionalAttributePolicyGroup = attributePolicyGroupRepository.findById(form.getId());

        if(optionalAttributePolicyGroup.isPresent()){
            //todo generify the process of updating a set of child objects given the existing and updated lists
            //todo use an Updateable interface on the domain object with the update method
            AttributePolicyGroup apg = optionalAttributePolicyGroup.get();
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

            if(toDelete != null) toDelete.forEach(ap -> attributePolicyRepository.delete(ap));
            if(toUpdate != null) toUpdate.forEach(ap -> {
                ap.update(updated.get(ap.getId()));
                attributePolicyRepository.save(ap);
            });
            newPolicies.forEach(np -> attributePolicyRepository.save(np));

            Optional<AttributePolicyGroup> optionalUpdatedGroup = attributePolicyGroupRepository.findById(form.getId());

            return new AttributePolicyGroupForm(optionalUpdatedGroup.get());
        }else{
            throw new RAObjectNotFoundException(AttributePolicyGroup.class, form.getId());
        }
    }

    @DeleteMapping("/group/delete/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteAttributePolicyGroup(@PathVariable Long id){
        attributePolicyGroupRepository.deleteById(id);
    }


}
