package org.rspk.dropbox_lite.service;

import org.rspk.dropbox_lite.model.files.File;
import org.rspk.dropbox_lite.model.folders.Folder;
import org.rspk.dropbox_lite.model.folders.FolderReq;
import org.rspk.dropbox_lite.model.objects.FilesRelation;
import org.rspk.dropbox_lite.model.objects.ObjectType;
import org.rspk.dropbox_lite.repository.FolderJpa;
import org.rspk.dropbox_lite.repository.FilesRelationJpa;
import org.rspk.dropbox_lite.utils.exceptions.InvalidRequestException;
import org.rspk.dropbox_lite.utils.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class FolderService implements FolderDependentService {

    private final FolderJpa folderJpa;
    private final FilesRelationJpa filesRelationJpa;
    private final static List<String> reservedFolderNames = List.of(".zip",".share");

    public FolderService(
            FolderJpa folderJpa,
            FilesRelationJpa filesRelationJpa
    ) {
        this.folderJpa = folderJpa;
        this.filesRelationJpa = filesRelationJpa;
    }


    @Transactional
    public Folder save(
            UUID accountId,
            FolderReq folderReq
    ) {
        if(reservedFolderNames.contains(folderReq.name()))
            throw new InvalidRequestException("can't use system reserved name as folder name");

        if(folderReq.parentFolderId() != null){
            if(!folderJpa.existsById(folderReq.parentFolderId(),accountId))
                throw new ResourceNotFoundException("unable to find parent folder id");
        }

        if(!folderJpa.existsByName(folderReq.name(),folderReq.parentFolderId(),accountId)) {
            throw new InvalidRequestException("filename must be unique");
        }

        Folder folder = new Folder(
                accountId,
                folderReq.parentFolderId(),
                folderReq.name()
        );

        Folder savedFolder = folderJpa.save(folder);

        FilesRelation or = new FilesRelation(
                savedFolder.getAccountId(),
                savedFolder.getParentFolderId(),
                savedFolder.getFolderId(),
                savedFolder.getName(),
                ObjectType.FOLDER
        );
        filesRelationJpa.save(or);

        return savedFolder;
    }

    @Transactional
    public Folder renameFolder(UUID folderId, UUID accountId,String name) {
        Folder folder = folderJpa.findById(folderId,accountId).orElseThrow(() ->
                new ResourceNotFoundException("folder not found")
        );

        if(folderJpa.existsByName(name,folder.getParentFolderId(),accountId))
            throw new InvalidRequestException("folderName must be unique");

        folder.setName(name);
        return folderJpa.save(folder);
    }

    @Transactional
    public void deleteFolder(UUID folderId,UUID accountId) {
        folderJpa.delete(folderId,accountId);
    }

    @Override
    public UUID createOrFindFolderUUID(String name,UUID accountId) {
        Folder savedFolder = folderJpa.findByName(name,null,accountId);
        if(savedFolder != null) return savedFolder.getFolderId();
        else {
            Folder folder = new Folder(
                    accountId,
                    null,
                    name
            );

            Folder newlySavedFolder = folderJpa.save(folder);

            FilesRelation or = new FilesRelation(
                    newlySavedFolder.getAccountId(),
                    newlySavedFolder.getParentFolderId(),
                    newlySavedFolder.getFolderId(),
                    newlySavedFolder.getName(),
                    ObjectType.FOLDER
            );
            filesRelationJpa.save(or);
            return newlySavedFolder.getFolderId();
        }
    }

}
