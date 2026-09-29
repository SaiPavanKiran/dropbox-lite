package org.rspk.dropbox_lite.controller;

import jakarta.validation.Valid;
import org.rspk.dropbox_lite.model.account.AuthenticatedUser;
import org.rspk.dropbox_lite.model.common.TemporalRes;
import org.rspk.dropbox_lite.model.folders.Folder;
import org.rspk.dropbox_lite.model.folders.FolderReq;
import org.rspk.dropbox_lite.model.folders.FolderRes;
import org.rspk.dropbox_lite.model.folders.RenameFolder;
import org.rspk.dropbox_lite.service.FolderService;
import org.rspk.dropbox_lite.utils.exceptions.SomethingWentWrongException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/folders")
public class FolderController {

    private final FolderService folderService;

    public FolderController(
            FolderService folderService
    ) {
        this.folderService = folderService;
    }

    @PostMapping("/create")
    ResponseEntity<?> createFolder(
            @Valid @RequestBody FolderReq folderReq,
            Authentication authentication
    ) {
        AuthenticatedUser authUser = (AuthenticatedUser) authentication.getPrincipal();
        if (authUser == null) throw new SomethingWentWrongException("unable to find user auth details");

        Folder folder = folderService.save(authUser.accountId(), folderReq);

        return ResponseEntity.ok(new FolderRes(
                folder.getFolderId(),
                folder.getParentFolderId(),
                folder.getName(),
                new TemporalRes(folder.getCreatedAt(),folder.getUpdatedAt())
        ));
    }




    @PatchMapping("/{id}/rename")
    ResponseEntity<?> renameFolder(
            @PathVariable("id") UUID folderId,
            @Valid @RequestBody RenameFolder renameFolder,
            Authentication authentication
    ) {
        AuthenticatedUser authUser = (AuthenticatedUser) authentication.getPrincipal();
        if(authUser == null) throw new SomethingWentWrongException("unable to find user auth details");

        Folder folder = folderService.renameFolder(folderId,authUser.accountId(),renameFolder.name());

        return ResponseEntity.ok(
                new FolderRes(
                        folder.getFolderId(),
                        folder.getParentFolderId(),
                        folder.getName(),
                        new TemporalRes(folder.getCreatedAt(),folder.getUpdatedAt())
                ));
    }


    @DeleteMapping("/{id}")
    ResponseEntity<?> deleteFile(
            @PathVariable("id") UUID fileId,
            Authentication authentication
    ) {
        AuthenticatedUser authUser = (AuthenticatedUser) authentication.getPrincipal();
        if(authUser == null) throw new SomethingWentWrongException("unable to find user auth details");

        folderService.deleteFolder(fileId,authUser.accountId());
        return ResponseEntity.ok().build();
    }

}
