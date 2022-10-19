package com.winllc.pki.ra.service;

import com.winllc.pki.ra.beans.info.CurrentUserDisplayItems;
import com.winllc.pki.ra.beans.info.PocEntryInfo;
import com.winllc.acme.common.domain.PocEntry;
import com.winllc.acme.common.repository.PocEntryRepository;
import com.winllc.pki.ra.security.RAUser;
import com.winllc.pki.ra.service.external.beans.IdentityExternal;
import com.winllc.pki.ra.service.external.vendorimpl.KeycloakIdentityProviderConnection;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user")
public class UserService {

    private final PocEntryRepository pocEntryRepository;
    private final KeycloakIdentityProviderConnection keycloakService;

    private final AccountRequestService accountRequestService;
    private final DomainLinkToAccountRequestService domainLinkToAccountRequestService;
    private final CertificateRequestService certificateRequestService;
    private final NotificationService notificationService;

    public UserService(KeycloakIdentityProviderConnection keycloakService, PocEntryRepository pocEntryRepository,
                       AccountRequestService accountRequestService, DomainLinkToAccountRequestService domainLinkToAccountRequestService,
                       CertificateRequestService certificateRequestService, NotificationService notificationService) {
        this.keycloakService = keycloakService;
        this.pocEntryRepository = pocEntryRepository;
        this.accountRequestService = accountRequestService;
        this.domainLinkToAccountRequestService = domainLinkToAccountRequestService;
        this.certificateRequestService = certificateRequestService;
        this.notificationService = notificationService;
    }


    @GetMapping("/profile")
    @ResponseStatus(HttpStatus.OK)
    public RAUser getProfile(@AuthenticationPrincipal UserDetails userDetails){
        RAUser raUser = new RAUser(userDetails.getUsername());
        raUser.setPermissions(userDetails.getAuthorities().stream().map(ga -> ga.toString()).collect(Collectors.toList()));
        return raUser;
    }

    @GetMapping("/displayCounts")
    public CurrentUserDisplayItems getDisplayItems(Authentication authentication){
        int accountCount = accountRequestService.findPendingCount();
        int domainCount = domainLinkToAccountRequestService.findByStatusCount();
        int certCount = certificateRequestService.findByStatusCount("new");

        int notifications = notificationService.getCurrentNotificationsCountForUser(authentication);

        CurrentUserDisplayItems display = new CurrentUserDisplayItems();
        display.setDomainLinkRequestsCount(domainCount);
        display.setAccountRequestsCount(accountCount);
        display.setManualCertRequestsCount(certCount);
        display.setNotificationsCount(notifications);
        display.updateRequestCount();

        return display;
    }

    @PostMapping("/search/{search}")
    @ResponseStatus(HttpStatus.OK)
    public List<String> searchUsersFromIdentityProvider(@PathVariable String search){

        List<IdentityExternal> identityExternals = keycloakService.searchByEmailLike(search);
        return identityExternals.stream()
                .map(e -> e.getEmail())
                .collect(Collectors.toList());
    }

    @GetMapping("/profile/info")
    @Transactional
    public PocEntryInfo getUserInfo(Authentication authentication){
        PocEntryInfo info = new PocEntryInfo();
        info.setUserId(authentication.getName());

        Optional<IdentityExternal> externalOptional = keycloakService.findByEmail(authentication.getName());
        if(externalOptional.isPresent()){
            IdentityExternal identityExternal = externalOptional.get();
            info.setFullName(identityExternal.getFirstName() + " " + identityExternal.getLastName());
        }

        List<PocEntry> allByEmailEquals = pocEntryRepository.findAllByEmailEquals(authentication.getName());
        if(CollectionUtils.isNotEmpty(allByEmailEquals)){
            PocEntry pocEntry = allByEmailEquals.get(0);
            if(pocEntry.getAddedOn() != null) {
                info.setAddedOn(pocEntry.getAddedOn().toString());
            }
        }

        info.setRoles(authentication.getAuthorities().stream().map(a -> a.getAuthority()).collect(Collectors.toList()));

        return info;
    }
}
