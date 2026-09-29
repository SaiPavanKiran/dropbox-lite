package org.rspk.dropbox_lite.controller;

import jakarta.validation.Valid;
import org.rspk.dropbox_lite.model.account.AuthenticatedUser;
import org.rspk.dropbox_lite.model.common.TemporalRes;
import org.rspk.dropbox_lite.model.files.*;
import org.rspk.dropbox_lite.service.FileService;
import org.rspk.dropbox_lite.service.FilesRelationService;
import org.rspk.dropbox_lite.utils.exceptions.SomethingWentWrongException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/files")
public class FileController {

    private final FileService fileService;
    private final FilesRelationService filesRelationService;

    public FileController(
            FileService fileService,
            FilesRelationService filesRelationService
    ) {
        this.fileService = fileService;
        this.filesRelationService = filesRelationService;
    }


    @GetMapping
    ResponseEntity<?> getChildren(
            @RequestParam(value = "parentFolderId", required = false) UUID parentFolderId,
            @RequestParam("page") int page,
            @RequestParam("size") int size,
            Authentication authentication
    ) {
        AuthenticatedUser authUser = (AuthenticatedUser) authentication.getPrincipal();
        if(authUser == null) throw new SomethingWentWrongException("unable to find user auth details");

        return ResponseEntity.ok(
                filesRelationService.getObjects(parentFolderId,authUser.accountId(),page,size)
        );
    }

    @GetMapping("/view")
    ResponseEntity<?> viewFile(
            @RequestParam("fileId") UUID fileId,
            Authentication authentication
    ){
        AuthenticatedUser authUser = (AuthenticatedUser) authentication.getPrincipal();
        if(authUser == null) throw new SomethingWentWrongException("unable to find user auth details");

        Map.Entry<File,String> fileDetails = fileService.viewFile(authUser.accountId(),fileId);
        File file = fileDetails.getKey();
        return ResponseEntity.ok(
                new DownloadFileRes(
                        file.getName(),
                        file.getContentType(),
                        file.getSize(),
                        fileDetails.getValue(),
                        new TemporalRes(
                                file.getCreatedAt(),
                                file.getUpdatedAt()
                        )
                )
        );
    }

    @PostMapping("/upload")
    ResponseEntity<?> uploadMetadata(
            @Valid @RequestBody FileUploadReq fileUploadReq,
            Authentication authentication
    ) {
        AuthenticatedUser authUser = (AuthenticatedUser) authentication.getPrincipal();
        if(authUser == null) throw new SomethingWentWrongException("unable to find user auth details");

        File file = fileService.upload(fileUploadReq,authUser.accountId());
        return ResponseEntity.ok(getFileResponse(file));
    }


    @PostMapping(
            value = "/{id}/complete",
            consumes = {MediaType.MULTIPART_FORM_DATA_VALUE },
            produces = { MediaType.APPLICATION_JSON_VALUE }
    )
    ResponseEntity<?> uploadActualFile(
            @PathVariable("id") UUID fileId,
            @RequestParam("file") MultipartFile actualFile,
            Authentication authentication
    ) {
        AuthenticatedUser authUser = (AuthenticatedUser) authentication.getPrincipal();
        if(authUser == null) throw new SomethingWentWrongException("unable to find user auth details");

        File file = fileService.uploadFile(actualFile,authUser.accountId(),fileId);
        return ResponseEntity.ok(getFileResponse(file));
    }

    @PostMapping("/archive")
    ResponseEntity<?> archiveFiles(
            @Valid @RequestBody FilesArchiveReq archiveReq,
            Authentication authentication
    ) {
        AuthenticatedUser authUser = (AuthenticatedUser) authentication.getPrincipal();
        if(authUser == null) throw new SomethingWentWrongException("unable to find user auth details");

        return ResponseEntity.ok(fileService.archiveFiles(archiveReq,authUser.accountId()));
    }

    @PostMapping("/{id}/copy")
    ResponseEntity<?> copyToFolder(
            @PathVariable("id") UUID fileId,
            @Valid @RequestBody CopyOrMoveFile copyFile,
            Authentication authentication
    ) {
        AuthenticatedUser authUser = (AuthenticatedUser) authentication.getPrincipal();
        if(authUser == null) throw new SomethingWentWrongException("unable to find user auth details");

        return ResponseEntity.ok(
                getFileResponse(fileService.copyToFolder(authUser.accountId(),fileId,copyFile.toFolder()))
        );
    }

    @PostMapping("/{id}/move")
    ResponseEntity<?> moveToFolder(
            @PathVariable("id") UUID fileId,
            @Valid @RequestBody CopyOrMoveFile moveFile,
            Authentication authentication
    ) {
        AuthenticatedUser authUser = (AuthenticatedUser) authentication.getPrincipal();
        if(authUser == null) throw new SomethingWentWrongException("unable to find user auth details");

        return ResponseEntity.ok(
                getFileResponse(fileService.moveToFolder(authUser.accountId(),fileId,moveFile.toFolder()))
        );
    }

    @PatchMapping("/{id}/rename")
    ResponseEntity<?> renameFilename(
            @PathVariable("id") UUID fileId,
            @Valid @RequestBody RenameFile renameFile,
            Authentication authentication
    ) {
        AuthenticatedUser authUser = (AuthenticatedUser) authentication.getPrincipal();
        if(authUser == null) throw new SomethingWentWrongException("unable to find user auth details");

        return ResponseEntity.ok(
                getFileResponse(fileService.renameFileName(authUser.accountId(),fileId,renameFile.name()))
        );
    }


    @DeleteMapping("/{id}")
    ResponseEntity<?> deleteFile(
            @PathVariable("id") UUID fileId,
            Authentication authentication
    ) {
        AuthenticatedUser authUser = (AuthenticatedUser) authentication.getPrincipal();
        if(authUser == null) throw new SomethingWentWrongException("unable to find user auth details");

        fileService.deleteFile(fileId,authUser.accountId());
        return ResponseEntity.ok().build();
    }


    private FileResponse getFileResponse(File file) {
        return new FileResponse(
                file.getFolderId(),
                file.getName(),
                file.getContentType(),
                file.getSize(),
                file.getArchived(),
                file.getUploadStatus(),
                new TemporalRes(
                        file.getCreatedAt(),
                        file.getUpdatedAt()
                )
        );
    }
}
