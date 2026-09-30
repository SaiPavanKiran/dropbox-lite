package org.rspk.dropbox_lite.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.rspk.dropbox_lite.model.account.AuthenticatedUser;
import org.rspk.dropbox_lite.model.share.ShareObject;
import org.rspk.dropbox_lite.model.share.ShareObjectReq;
import org.rspk.dropbox_lite.model.share.SharingObjectRes;
import org.rspk.dropbox_lite.service.ShareService;
import org.rspk.dropbox_lite.utils.common_functions.StringUtils;
import org.rspk.dropbox_lite.utils.exceptions.SomethingWentWrongException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/sharing")
public class ShareController {

    private final ShareService shareService;

    public ShareController(
            ShareService shareService
    ) {
        this.shareService = shareService;
    }

    @PostMapping
    ResponseEntity<?> shareObject(
            @Valid @RequestBody ShareObjectReq shareReq,
            Authentication authentication
    ) {
        AuthenticatedUser authUser = (AuthenticatedUser) authentication.getPrincipal();
        if(authUser == null) throw new SomethingWentWrongException("unable to find user auth details");

        ShareObject shareObject = shareService.shareObject(authUser.accountId(),shareReq);
        return ResponseEntity.ok(
                new SharingObjectRes(
                       shareObject.getId().getObjectId(),
                        shareObject.getId().getObjectName(),
                        shareReq.recipientEmail(),
                        shareObject.getExpiry().isAfter(Instant.now()),
                        shareObject.getCreatedAt(),
                        shareObject.getUpdatedAt(),
                        shareObject.getExpiry()
                )
        );
    }


    @GetMapping("/{id}/view")
    ResponseEntity<?> viewSharedObject(
            @NotBlank
            @org.hibernate.validator.constraints.UUID(message = "not a valid id")
            @PathVariable("id")
            String objectId,
            @NotBlank
            @Email @RequestParam("sharedBy") String ownerEmail,
            Authentication authentication
    ) {
        AuthenticatedUser authUser = (AuthenticatedUser) authentication.getPrincipal();
        if(authUser == null) throw new SomethingWentWrongException("unable to find user auth details");
        return ResponseEntity.ok(shareService.viewSharedObject(ownerEmail, StringUtils.toUUIDorNull(objectId),authUser.accountId()));
    }

    @GetMapping("/.shared")
    ResponseEntity<?> getSharedObjects(
            @RequestParam("page") int page,
            @RequestParam("size") int size,
            Authentication authentication
    ) {
        AuthenticatedUser authUser = (AuthenticatedUser) authentication.getPrincipal();
        if(authUser == null) throw new SomethingWentWrongException("unable to find user auth details");
        return ResponseEntity.ok(shareService.getSharedFiles(authUser.accountId(),page,size));
    }


    @GetMapping("/.sharing")
    ResponseEntity<?> getSharingObjects(
            @RequestParam("page") int page,
            @RequestParam("size") int size,
            Authentication authentication
    ) {
        AuthenticatedUser authUser = (AuthenticatedUser) authentication.getPrincipal();
        if(authUser == null) throw new SomethingWentWrongException("unable to find user auth details");
        return ResponseEntity.ok(shareService.getSharingFiles(authUser.accountId(),page,size));
    }


    @DeleteMapping("/{id}")
    ResponseEntity<?> deleteSharing(
            @NotBlank
            @org.hibernate.validator.constraints.UUID(message = "not a valid id")
            @PathVariable("id")
            String objectId,
            @Email
            @NotBlank
            @RequestParam("sharedTo") String recipientEmail,
            Authentication authentication
    ) {
        AuthenticatedUser authUser = (AuthenticatedUser) authentication.getPrincipal();
        if(authUser == null) throw new SomethingWentWrongException("unable to find user auth details");
        shareService.deleteSharedObject(recipientEmail,StringUtils.toUUIDorNull(objectId),authUser.accountId());
        return ResponseEntity.ok().build();
    }


}
