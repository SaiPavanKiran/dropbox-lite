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
import org.rspk.dropbox_lite.utils.exceptions.InvalidRequestException;
import org.rspk.dropbox_lite.utils.exceptions.ResourceNotFoundException;
import org.rspk.dropbox_lite.utils.exceptions.SomethingWentWrongException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.rspk.dropbox_lite.utils.common_functions.Formatters.DATE_TIME_FORMATTER;
import static org.rspk.dropbox_lite.utils.common_functions.Formatters.DATE_TIME_FORMATTER_WITH_MILLIS;

@Service
public class FileService {

    private final static Logger logger = LoggerFactory.getLogger(FileService.class);
    private final FileJpa fileJpa;
    private final FolderJpa folderJpa;
    private final FilesRelationJpa filesRelationJpa;
    private final FolderDependentService folderDependentService;

    @Value("${aws.s3.users_bucket}")
    private String usersBucketName;

    public FileService(
            FileJpa fileJpa,
            FilesRelationJpa filesRelationJpa,
            FolderJpa folderJpa,
            FolderDependentService folderDependentService
    ) {
        this.fileJpa = fileJpa;
        this.filesRelationJpa = filesRelationJpa;
        this.folderJpa = folderJpa;
        this.folderDependentService = folderDependentService;
    }


    @Transactional
    public File upload(
            FileUploadReq fileUploadReq,
            UUID accountId
    ) {
        if(fileJpa.countByName(fileUploadReq.name(), fileUploadReq.folderId(),accountId) > 0)
            throw new InvalidRequestException("filename must be unique");

        File file = new File(
                accountId,
                fileUploadReq.folderId(),
                "",
                fileUploadReq.name(),
                fileUploadReq.contentType(), /*need to fix this since user can send any content type*/
                fileUploadReq.size(),
                false,
                UploadStatus.PENDING
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

    public File uploadFile(
            MultipartFile file,
            UUID accountId,
            UUID fileId
    ) {
        File savedFile = fileJpa.findById(fileId,accountId)
                .orElseThrow(() -> new ResourceNotFoundException("file metadata not found"));

        if(!savedFile.getAccountId().equals(accountId))
            throw  new ResourceNotFoundException("file metadata not found");

        try(S3Client s3Client = S3Client.builder().build()){

            try {
                s3Client.headBucket(
                        HeadBucketRequest.builder()
                                .bucket(usersBucketName)
                                .build()
                );
            } catch (NoSuchBucketException ex) {
                s3Client.createBucket(
                        CreateBucketRequest.builder()
                                .bucket(usersBucketName)
                                .build()
                );
            }

            savedFile.setContentType(file.getContentType());
            savedFile.setSize(file.getSize());
            savedFile.setUploadStatus(UploadStatus.COMPLETED);
            fileJpa.save(savedFile);


            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(usersBucketName)
                            .key(savedFile.getS3Key())
                            .contentType(file.getContentType())
                            .contentLength(file.getSize())
                            .metadata(Map.of("uploadedAt", DATE_TIME_FORMATTER.format(Instant.now())))
                            .build(),
                    RequestBody.fromInputStream(
                            file.getInputStream(),
                            file.getSize()
                    )
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

        GetObjectRequest objectRequest = GetObjectRequest.builder()
                .bucket(usersBucketName)
                .key(file.getS3Key()) /*path inside the bucket excluding the bucketName*/
                .build();

        /*Create a signed URL for this S3 GET request, and allow that URL to be used for 15 minutes*/
        GetObjectPresignRequest preSignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(15))
                .getObjectRequest(objectRequest)
                .build();

        /*preSigner generates the cryptographic signature and constructs the URL.*/
        try(S3Presigner preSigner = S3Presigner.builder().build()) {
            PresignedGetObjectRequest preSigned  = preSigner.presignGetObject(preSignRequest);

            return Map.entry(file,preSigned.url().toString());
        }
    }

    @Transactional
    public String archiveFiles(
            FilesArchiveReq filesArchiveReq,
            UUID accountId
    ) {
        List<UUID> allFileIds = new ArrayList<>(filesArchiveReq.fileIds());
        allFileIds.addAll(filesRelationJpa.findByParent(filesArchiveReq.folderIds(), accountId));
        if(filesArchiveReq.allFiles()) {
            allFileIds.addAll(fileJpa.getIdsByAccountId(accountId));
        }

        createZip(allFileIds, accountId);

        return "files archived successfully";
    }

    private void createZip(
            List<UUID> fileIds,
            UUID accountId
    ) {
        new Thread(() -> {
            Set<String> mappedNames = new HashSet<>();

            Path tempZip = null;
            try {
                tempZip = Files.createTempFile("archive-", ".zip");
                try (
                        ZipOutputStream zipOut = new ZipOutputStream(Files.newOutputStream(tempZip));
                        S3Client s3Client = S3Client.builder().build();
                ) {
                    fileJpa.findS3KeysByIds(fileIds, accountId).forEach(s3Key -> {
                        GetObjectRequest request = GetObjectRequest.builder()
                                .bucket(usersBucketName)
                                .key(s3Key)
                                .build();
                        try (ResponseInputStream<GetObjectResponse> input =
                                     s3Client.getObject(request)) {
                            String fileName = Path.of(s3Key)
                                    .getFileName()
                                    .toString();

                            if (!mappedNames.contains(fileName)) {
                                mappedNames.add(fileName);
                                zipOut.putNextEntry(new ZipEntry(fileName));
                            } else {
                                String mappedName = fileName + DATE_TIME_FORMATTER_WITH_MILLIS.format(Instant.now());
                                mappedNames.add(mappedName);
                                zipOut.putNextEntry(new ZipEntry(mappedName));
                            }

                            input.transferTo(zipOut);

                            zipOut.closeEntry();
                        } catch (IOException ex) {
                            logger.error("an io exception while processing file s3key - {}", s3Key);
                        }

                    });
                }
                long zipSize = Files.size(tempZip);

                String zipFileName = "archive_" +
                        DATE_TIME_FORMATTER_WITH_MILLIS.format(Instant.now()) +
                        ".zip";

                FileUploadReq uploadReq = new FileUploadReq(
                        folderDependentService.createOrFindFolderUUID(".zip", accountId),
                        zipFileName,
                        "application/zip",
                        zipSize
                );

                File savedFile = upload(uploadReq, accountId);

                PutObjectRequest putRequest = PutObjectRequest.builder()
                        .bucket(usersBucketName)
                        .key(savedFile.getS3Key())
                        .contentType("application/zip")
                        .build();

                try (S3Client s3Client = S3Client.builder().build()) {
                    s3Client.putObject(
                            putRequest,
                            RequestBody.fromFile(tempZip)
                    );
                }


                Files.deleteIfExists(tempZip);

            } catch (IOException ex) {
                logger.error("an io exception while processing file's - {}", fileIds);
            } finally {
                if (tempZip != null) {
                    try {
                        Files.deleteIfExists(tempZip);
                    } catch (IOException ex) {
                        logger.error(
                                "Failed to delete temporary ZIP: {}",
                                tempZip,
                                ex
                        );
                    }
                }
            }

        }).start();
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
                folder.getFolderId(),
                file.getName() + DATE_TIME_FORMATTER_WITH_MILLIS.format(Instant.now()),
                file.getContentType(),
                file.getSize()
        );

        return upload(uploadReq, accountId);
    }

    @Transactional
    public File moveToFolder(
            UUID accountId,
            UUID fileId,
            UUID toFolder
    ) {
        File file = fileJpa.findById(fileId,accountId).orElseThrow(() ->
                new ResourceNotFoundException("file not found")
        );

        folderJpa.findById(toFolder,accountId)
                .orElseThrow(() -> new ResourceNotFoundException("folder not found"));

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

        file.setName(name);

        return fileJpa.save(file);
    }

    @Transactional
    public void deleteFile(
            UUID fileId,
            UUID accountId
    ) {
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
