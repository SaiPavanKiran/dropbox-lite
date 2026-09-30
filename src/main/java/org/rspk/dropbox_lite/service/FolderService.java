package org.rspk.dropbox_lite.service;

import org.rspk.dropbox_lite.model.folders.Folder;
import org.rspk.dropbox_lite.model.folders.FolderReq;
import org.rspk.dropbox_lite.model.objects.FilesRelation;
import org.rspk.dropbox_lite.model.objects.ObjectType;
import org.rspk.dropbox_lite.repository.FileJpa;
import org.rspk.dropbox_lite.repository.FolderJpa;
import org.rspk.dropbox_lite.repository.FilesRelationJpa;
import org.rspk.dropbox_lite.utils.common_functions.StringUtils;
import org.rspk.dropbox_lite.utils.exceptions.InvalidRequestException;
import org.rspk.dropbox_lite.utils.exceptions.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class FolderService implements FolderDependentService {

    @Value("${aws.s3.users_bucket}")
    private String usersBucketName;

    private final FolderJpa folderJpa;
    private final FileJpa fileJpa;
    private final FilesRelationJpa filesRelationJpa;
    private final S3DependentService s3DependentService;
    private final static List<String> reservedFolderNames = List.of(".zip",".share",".shared");

    public FolderService(
            FolderJpa folderJpa, FileJpa fileJpa,
            FilesRelationJpa filesRelationJpa, S3DependentService s3DependentService
    ) {
        this.folderJpa = folderJpa;
        this.fileJpa = fileJpa;
        this.filesRelationJpa = filesRelationJpa;
        this.s3DependentService = s3DependentService;
    }


    @Transactional
    public Folder save(
            UUID accountId,
            FolderReq folderReq
    ) {
        if(reservedFolderNames.contains(folderReq.name()))
            throw new InvalidRequestException("can't use system reserved name as folder name");

        UUID parentFolderId = StringUtils.toUUIDorNull(folderReq.parentFolderId());

        if(parentFolderId != null){
            if(!folderJpa.existsById(parentFolderId,accountId))
                throw new ResourceNotFoundException("unable to find parent folder id");
        }

        if(folderJpa.existsByName(folderReq.name(),parentFolderId,accountId)) {
            throw new InvalidRequestException("folder Name must be unique");
        }

        Folder folder = new Folder(
                accountId,
                parentFolderId,
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

        if(reservedFolderNames.contains(folder.getName()) || reservedFolderNames.contains(name))
            throw new InvalidRequestException("can't accept renaming reserved folder names");

        if(folderJpa.existsByName(name,folder.getParentFolderId(),accountId))
            throw new InvalidRequestException("folderName must be unique");

        FilesRelation filesRelation = filesRelationJpa.findByObject(folder.getFolderId(),folder.getParentFolderId(),accountId,ObjectType.FOLDER,folder.getName());
        filesRelation.setName(name);
        filesRelationJpa.save(filesRelation);

        folder.setName(name);
        return folderJpa.save(folder);
    }

    @Transactional
    public void deleteFolder(UUID folderId, UUID accountId, boolean recursive) {
        Folder folder = folderJpa.findById(folderId,accountId).orElseThrow(() ->
                new ResourceNotFoundException("folder not found")
        );

        if(reservedFolderNames.contains(folder.getName()))
            throw new InvalidRequestException("can't delete reserved folder names");

        List<String> s3Keys = fileJpa.findS3KeysByParentId(folder.getFolderId(),accountId);


        if(!recursive && !s3Keys.isEmpty())
            throw new InvalidRequestException("folder contains files , you can force delete by applying recursive");

        /*obj relation,child files,child folders is deleted by on delete cascade*/
        s3DependentService.bulkDeleteS3File(usersBucketName,s3Keys);

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
