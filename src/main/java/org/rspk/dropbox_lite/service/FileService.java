package org.rspk.dropbox_lite.service;

import org.rspk.dropbox_lite.model.files.File;
import org.rspk.dropbox_lite.model.files.FileUploadReq;
import org.rspk.dropbox_lite.model.files.FilesArchiveReq;
import org.rspk.dropbox_lite.model.files.UploadStatus;
import org.rspk.dropbox_lite.model.folders.Folder;
import org.rspk.dropbox_lite.model.objects.FilesRelation;
import org.rspk.dropbox_lite.model.objects.ObjectType;
import org.rspk.dropbox_lite.repository.FileJpa;
import org.rspk.dropbox_lite.repository.FilesRelationJpa;
import org.rspk.dropbox_lite.repository.FolderJpa;
import org.rspk.dropbox_lite.utils.common_functions.StringUtils;
import org.rspk.dropbox_lite.utils.exceptions.DataNotUniqueException;
import org.rspk.dropbox_lite.utils.exceptions.InvalidRequestException;
import org.rspk.dropbox_lite.utils.exceptions.ResourceNotFoundException;
import org.rspk.dropbox_lite.utils.exceptions.SomethingWentWrongException;
import org.rspk.dropbox_lite.utils.logs.CommonLogging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.function.BiFunction;

import static org.rspk.dropbox_lite.utils.common_functions.Formatters.DATE_TIME_FORMATTER_WITH_MILLIS;

@Service
public class FileService {

    private final static Logger logger = LoggerFactory.getLogger(FileService.class);
    private final FileJpa fileJpa;
    private final FolderJpa folderJpa;
    private final FilesRelationJpa filesRelationJpa;
    private final FolderDependentService folderDependentService;
    private final S3DependentService s3DependentService;

    @Value("${aws.s3.users_bucket}")
    private String usersBucketName;

    public FileService(
            FileJpa fileJpa,
            FilesRelationJpa filesRelationJpa,
            FolderJpa folderJpa,
            FolderDependentService folderDependentService,
            S3DependentService s3DependentService
    ) {
        this.fileJpa = fileJpa;
        this.filesRelationJpa = filesRelationJpa;
        this.folderJpa = folderJpa;
        this.folderDependentService = folderDependentService;
        this.s3DependentService = s3DependentService;
    }


    @Transactional
    public File upload(
            FileUploadReq fileUploadReq,
            UUID accountId,
            boolean isArchived,
            UploadStatus uploadStatus
    ) {
        UUID folderId = StringUtils.toUUIDorNull(fileUploadReq.folderId());
        if(folderId !=null && folderJpa.findById(folderId, accountId).isEmpty())
            throw new ResourceNotFoundException("folder not found");

        if(fileJpa.countByName(fileUploadReq.name(), folderId,accountId) > 0)
            throw new InvalidRequestException("filename must be unique");

        File file = new File(
                accountId,
                folderId,
                "",
                fileUploadReq.name(),
                fileUploadReq.contentType(), /*need to fix this since user can send any content type*/
                fileUploadReq.size(),
                isArchived,
                uploadStatus
        );

        File savedFile = fileJpa.save(file);
        savedFile.setS3Key(buildS3Key(savedFile.getAccountId(),savedFile.getFileId(),savedFile.getName()));

        FilesRelation or = new FilesRelation(
                savedFile.getAccountId(),
                savedFile.getFolderId(),
                savedFile.getFileId(),
                savedFile.getName(),
                ObjectType.FILE
        );
        filesRelationJpa.save(or);

        return savedFile;
    }

    @Transactional
    public File uploadFile(
            MultipartFile file,
            UUID accountId,
            UUID fileId
    ) {
        File savedFile = fileJpa.findById(fileId,accountId)
                .orElseThrow(() -> new ResourceNotFoundException("file metadata not found"));

        if(savedFile.getUploadStatus() == UploadStatus.COMPLETED)
            throw new InvalidRequestException("file already uploaded");

        if(!savedFile.getAccountId().equals(accountId))
            throw  new ResourceNotFoundException("file metadata not found");

        savedFile.setContentType(file.getContentType());
        savedFile.setSize(file.getSize());
        savedFile.setUploadStatus(UploadStatus.COMPLETED);
        fileJpa.save(savedFile);

        try{
            s3DependentService.save(
                    usersBucketName,
                    savedFile.getS3Key(),
                    savedFile.getContentType(),
                    savedFile.getSize(),
                    null,
                    file
            );
        } catch (IOException e) {
            savedFile.setUploadStatus(UploadStatus.FAILED);
            fileJpa.save(savedFile);
            throw new SomethingWentWrongException("Failed to upload file");
        }
        return savedFile;
    }

    @Transactional
    public Map.Entry<File,String> viewFile(
            UUID accountId,
            UUID fileId
    ) {
        File file = fileJpa.findById(fileId,accountId).orElseThrow(
                () -> new ResourceNotFoundException("file not found")
        );

        if(file.getUploadStatus() != UploadStatus.COMPLETED)
            throw new InvalidRequestException("file upload is still pending");

        return Map.entry(
                file,
                s3DependentService.getPreSignedUrl(usersBucketName,file.getS3Key(),15).toString()
        );
    }

    @Async("archiveExecutor")
    @Transactional
    public void archiveFiles(
            FilesArchiveReq filesArchiveReq,
            UUID accountId
    ) {
        /*allFileIds.addAll(filesRelationJpa.findByParentIds(StringUtils.toUUIDList(filesArchiveReq.folderIds()), accountId));
        if(filesArchiveReq.allFiles()) {
            allFileIds.addAll(fileJpa.getIdsByAccountId(accountId));
        }*/

        createZip(StringUtils.toUUIDList(filesArchiveReq.fileIds()), accountId);
    }

    private void createZip(
            List<UUID> fileIds,
            UUID accountId
    ) {
        BiFunction<String, Long, File> uploadZip =
                (zipFileName, zipSize) -> {
                    FileUploadReq uploadReq = new FileUploadReq(
                            folderDependentService.createOrFindFolderUUID(".zip", accountId).toString(),
                            zipFileName,
                            "application/zip",
                            zipSize
                    );

                    return upload(uploadReq, accountId,true,UploadStatus.COMPLETED);
                };

        s3DependentService.createZipOfFiles(
                (() -> {
                    return fileJpa.findS3KeysByIds(fileIds, accountId);
                }),
                uploadZip,
                usersBucketName
        );
    }

    @Transactional
    public File copyToFolder(
            UUID accountId,
            UUID fileId,
            UUID toFolder
    ) {
        File file = fileJpa.findById(fileId,accountId).orElseThrow(() ->
                new ResourceNotFoundException("file not found")
        );

        Folder folder = folderJpa.findById(toFolder,accountId)
                .orElseThrow(() -> new ResourceNotFoundException("folder not found"));

        FileUploadReq uploadReq = new FileUploadReq(
                folder.getFolderId().toString(),
                file.getName() + '_' + DATE_TIME_FORMATTER_WITH_MILLIS.format(Instant.now()),
                file.getContentType(),
                file.getSize()
        );

        return upload(uploadReq, accountId,false,file.getUploadStatus());
    }

    @Transactional
    public File moveToFolder(
            UUID accountId,
            UUID fileId,
            UUID toFolder,
            boolean replace
    ) {
        File file = fileJpa.findById(fileId,accountId).orElseThrow(() ->
                new ResourceNotFoundException("file not found")
        );

        if(file.getFolderId().equals(toFolder))
            throw new InvalidRequestException("file can't be moved to same folder");

        folderJpa.findById(toFolder,accountId)
                .orElseThrow(() -> new ResourceNotFoundException("folder not found"));

        FilesRelation fileWithSameName = filesRelationJpa.findByName(toFolder,accountId,ObjectType.FILE,file.getName());

        if(fileWithSameName != null) {
            if (!replace) {
                throw new DataNotUniqueException("folder already contains a file with same name");
            }
            fileJpa.deleteById(fileWithSameName.getObjectId(),accountId);
            filesRelationJpa.delete(fileWithSameName);
        }


        FilesRelation filesRelation = filesRelationJpa.findByObject(file.getFileId(),file.getFolderId(),accountId,ObjectType.FILE,file.getName());
        filesRelation.setParentId(toFolder);
        filesRelationJpa.save(filesRelation);

        file.setFolderId(toFolder);
        return fileJpa.save(file);
    }

    @Transactional
    public File renameFileName(
            UUID accountId,
            UUID fileId,
            String name
    ) {
        File file = fileJpa.findById(fileId,accountId).orElseThrow(() ->
                new ResourceNotFoundException("file not found")
        );

        if(fileJpa.countByName(name,file.getFolderId(),accountId) > 0)
            throw new InvalidRequestException("filename must be unique");


        FilesRelation filesRelation = filesRelationJpa.findByObject(file.getFileId(),file.getFolderId(),accountId,ObjectType.FILE,file.getName());
        filesRelation.setName(name);
        filesRelationJpa.save(filesRelation);

        file.setName(name);
        return fileJpa.save(file);
    }

    @Transactional
    public void deleteFile(
            UUID fileId,
            UUID accountId
    ) {
        File file = fileJpa.findById(fileId,accountId).orElseThrow(
                () -> new ResourceNotFoundException("file not found")
        );

        if(file.getUploadStatus() == UploadStatus.COMPLETED)
            s3DependentService.deleteS3File(usersBucketName,file.getS3Key());

        fileJpa.deleteById(fileId,accountId);
    }



    private String buildS3Key(
        UUID accountId,
        UUID fileId,
        String filename
    ) {
        String s3KeyFormat = "%s/%s/%s";
        return s3KeyFormat.formatted(accountId,fileId,filename);
    }

}
