package org.rspk.dropbox_lite.controller;

import org.rspk.dropbox_lite.model.account.AuthenticatedUser;
import org.rspk.dropbox_lite.model.share.ShareObjectReq;
import org.rspk.dropbox_lite.service.ShareService;
import org.rspk.dropbox_lite.utils.exceptions.SomethingWentWrongException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/share")
public class ShareController {

    private final ShareService shareService;

    public ShareController(
            ShareService shareService
    ) {
        this.shareService = shareService;
    }

    @PostMapping
    ResponseEntity<?> shareObject(
            @RequestBody ShareObjectReq shareReq,
            Authentication authentication
    ) {
        AuthenticatedUser authUser = (AuthenticatedUser) authentication.getPrincipal();
        if(authUser == null) throw new SomethingWentWrongException("unable to find user auth details");

        shareService.shareObject(authUser.accountId(),shareReq);
        return ResponseEntity.ok("object shared successfully");
    }


    @GetMapping("/{id}/view")
    ResponseEntity<?> viewSharedObject(
            @PathVariable("id") UUID objectId,
            @RequestParam("sharedBy") String ownerEmail,
            Authentication authentication
    ) {
        AuthenticatedUser authUser = (AuthenticatedUser) authentication.getPrincipal();
        if(authUser == null) throw new SomethingWentWrongException("unable to find user auth details");
        return ResponseEntity.ok(shareService.viewSharedObject(ownerEmail,objectId,authUser.accountId()));
    }

    @GetMapping
    ResponseEntity<?> getSharedObjects(
            @RequestParam("page") int page,
            @RequestParam("size") int size,
            Authentication authentication
    ) {
        AuthenticatedUser authUser = (AuthenticatedUser) authentication.getPrincipal();
        if(authUser == null) throw new SomethingWentWrongException("unable to find user auth details");
        return ResponseEntity.ok(shareService.getSharedFiles(authUser.accountId(),page,size));
    }

}
