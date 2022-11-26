package com.winllc.pki.ra.service;

import com.winllc.acme.common.domain.ServerEntry;
import com.winllc.acme.common.repository.ServerEntryRepository;
import com.winllc.pki.ra.beans.form.SansForm;
import com.winllc.pki.ra.beans.search.GridModel;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sans")
@Transactional
public class SansService implements UpdatedDataService<SansForm, Long> {

    private final ServerEntryRepository serverEntryRepository;

    public SansService(ServerEntryRepository serverEntryRepository) {
        this.serverEntryRepository = serverEntryRepository;
    }

    @PostMapping("/paged")
    @Override
    public Page<SansForm> getPaged(@RequestParam Integer page,
                                   @RequestParam Integer pageSize,
                                   @RequestParam(defaultValue = "asc") String order,
                                   @RequestParam(required = false) String sortBy,
                                   @RequestParam Map<String, String> allRequestParams,
                                   @RequestBody GridModel gridModel,
                                   Authentication authentication) {

        Optional<ServerEntry> optionalEntry = getServerEntry(allRequestParams);
        if(optionalEntry.isPresent()) {
            ServerEntry serverEntry = optionalEntry.get();
            Hibernate.initialize(serverEntry.getAlternateDnsValues());
            List<SansForm> forms = serverEntry.getAlternateDnsValues().stream()
                    .map(s -> new SansForm(s, serverEntry.getId()))
                    .collect(Collectors.toList());
            return new PageImpl<>(forms);
        }else{
            return Page.empty();
        }
    }

    @Override
    public Page<SansForm> getMyPaged(Integer page, Integer pageSize, String order, String sortBy, Map<String, String> allRequestParams, GridModel gridModel, Authentication authentication) {
        return null;
    }

    @Override
    public List<SansForm> getAll(Authentication authentication) throws Exception {
        return null;
    }

    @Override
    public SansForm findRest(Long id, Authentication authentication) throws Exception {
        return null;
    }

    @Override
    public SansForm addRest(SansForm entity, Map<String, String> allRequestParams, Authentication authentication) throws Exception {
        return null;
    }

    @Override
    @PostMapping("/update")
    public SansForm updateRest(@Valid @RequestBody SansForm entity, @RequestParam Map<String, String> allRequestParams,
                        Authentication authentication) throws Exception {

        Optional<ServerEntry> entryOptional = getServerEntry(allRequestParams);

        if(entryOptional.isPresent()) {
            ServerEntry serverEntry = entryOptional.get();
            serverEntry.getAlternateDnsValues()
                    .add(entity.buildFqdn());

            serverEntryRepository.save(serverEntry);
        }

        return entity;
    }

    @Override
    @PostMapping("/delete/{id}")
    public void deleteRest(@PathVariable Long id, @RequestBody SansForm form,
                           Authentication authentication) throws Exception {
        ServerEntry serverEntry = serverEntryRepository.findById(form.getServerId()).orElseThrow();

        serverEntry.getAlternateDnsValues()
                .removeIf(s -> s.equalsIgnoreCase(form.buildFqdn()));

        serverEntryRepository.save(serverEntry);
    }

    private Optional<ServerEntry> getServerEntry(Map<String, String> params){
        if(params.containsKey("serverId")){
            Long serverId = Long.valueOf(params.get("serverId"));
            return serverEntryRepository.findById(serverId);
        }else{
            return Optional.empty();
        }
    }
}
