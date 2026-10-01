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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
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
    public Slice<ObjectsRes> getObjects(
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
                                Collectors.mapping(FilesRelation::getObjectId,
                                        Collectors.toList()
                                )
                        ));

        List<UUID> folderIds = result.get(false);
        List<UUID> fileIds = result.get(true);


        folderJpa.findByIds(folderIds,accountId).forEach(folder -> objects.add(getMappedFolder(folder)));
        fileJpa.findByIds(fileIds,accountId).forEach(file -> objects.add(getMappedFile(file)));
        return new SliceImpl<>(objects.subList(0,Math.min(objects.size(), size)), PageRequest.of(page,size),savedObjects.size() > size);
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
                file.getFileId(),
                file.getName(),
                file.getFolderId(),
                file.getContentType(),
                file.getSize(),
                file.getArchived(),
                file.getUploadStatus(),
                new TemporalRes(file.getCreatedAt(), file.getUpdatedAt())
        );
    }




}
