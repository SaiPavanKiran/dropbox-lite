package org.rspk.dropbox_lite.service;

import org.rspk.dropbox_lite.model.common.TemporalRes;
import org.rspk.dropbox_lite.model.files.File;
import org.rspk.dropbox_lite.model.folders.Folder;
import org.rspk.dropbox_lite.model.objects.FilesRelation;
import org.rspk.dropbox_lite.model.objects.ObjectType;
import org.rspk.dropbox_lite.model.objects.ObjectsRes;
import org.rspk.dropbox_lite.repository.FileJpa;
import org.rspk.dropbox_lite.repository.FolderJpa;
import org.rspk.dropbox_lite.repository.FilesRelationJpa;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class FilesRelationService {

    private final FolderJpa folderJpa;
    private final FileJpa fileJpa;
    private final FilesRelationJpa filesRelationJpa;

    public FilesRelationService(
            FolderJpa folderJpa,
            FileJpa fileJpa,
            FilesRelationJpa filesRelationJpa
    ) {
        this.folderJpa = folderJpa;
        this.fileJpa = fileJpa;
        this.filesRelationJpa = filesRelationJpa;
    }


    @Transactional
    public List<ObjectsRes> getObjects(
            UUID parentFolderId,
            UUID accountId,
            int page,
            int size
    ) {
        List<ObjectsRes> objects = new ArrayList<>(size);
        List<FilesRelation> savedObjects = filesRelationJpa.findByParent(parentFolderId,accountId,page,size);


        Map<Boolean, List<UUID>> result =
                savedObjects.stream()
                        .collect(Collectors.partitioningBy(
                                obj -> ObjectType.FILE.equals(obj.getType()),
                                Collectors.mapping(mapper ->
                                        mapper.getObjectRelationId().getObjectId(),
                                        Collectors.toList()
                                )
                        ));

        List<UUID> fileIds = result.get(true);
        List<UUID> folderIds = result.get(false);


        fileJpa.findByIds(fileIds,accountId).forEach(file -> objects.add(getMappedFile(file)));
        folderJpa.findByIds(folderIds,accountId).forEach(folder -> objects.add(getMappedFolder(folder)));
        return objects;
    }


    private ObjectsRes getMappedFolder(Folder folder) {
        return new ObjectsRes.FolderObj(
                ObjectType.FOLDER,
                folder.getFolderId(),
                folder.getName(),
                folder.getParentFolderId(),
                new TemporalRes(folder.getCreatedAt(),folder.getUpdatedAt())
        );
    }

    private ObjectsRes getMappedFile(File file) {
        return new ObjectsRes.FileObj(
                ObjectType.FILE,
                file.getFolderId(),
                file.getName(),
                file.getContentType(),
                file.getSize(),
                file.getArchived(),
                file.getUploadStatus(),
                new TemporalRes(file.getCreatedAt(), file.getUpdatedAt())
        );
    }




}
