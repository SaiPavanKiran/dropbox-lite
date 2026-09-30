package org.rspk.dropbox_lite.service;

import org.rspk.dropbox_lite.model.files.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.rspk.dropbox_lite.utils.common_functions.Formatters.DATE_TIME_FORMATTER;
import static org.rspk.dropbox_lite.utils.common_functions.Formatters.DATE_TIME_FORMATTER_WITH_MILLIS;

@Service
public class S3Service implements S3DependentService{

    private final static Logger logger = LoggerFactory.getLogger(S3Service.class);

    @Override
    public void save(
            String usersBucketName,
            String s3key,
            String contentType,
            long size,
            Map<String, String> metadata,
            MultipartFile file
    ) throws IOException {
        try (S3Client s3Client = S3Client.builder().build()) {

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

            Map<String, String> allMetadata = new HashMap<>();

            allMetadata.put("uploadedAt", DATE_TIME_FORMATTER.format(Instant.now()));
            if (metadata != null) allMetadata.putAll(metadata);

            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(usersBucketName)
                            .key(s3key)
                            .contentType(contentType)
                            .contentLength(size)
                            .metadata(allMetadata)
                            .build(),
                    RequestBody.fromInputStream(
                            file.getInputStream(),
                            file.getSize()
                    )
            );
        }
    }


    @Override
    public URL getPreSignedUrl(
            String usersBucketName,
            String s3key,
            long signDurationInMin
    ) {
        GetObjectRequest objectRequest = GetObjectRequest.builder()
                .bucket(usersBucketName)
                .key(s3key) /*path inside the bucket excluding the bucketName*/
                .build();

        /*Create a signed URL for this S3 GET request, and allow that URL to be used for 15 minutes*/
        GetObjectPresignRequest preSignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(signDurationInMin))
                .getObjectRequest(objectRequest)
                .build();

        /*preSigner generates the cryptographic signature and constructs the URL.*/
        try(S3Presigner preSigner = S3Presigner.builder().build()) {
            PresignedGetObjectRequest preSigned = preSigner.presignGetObject(preSignRequest);
            return preSigned.url();
        }
    }

    @Override
    public void createZipOfFiles(
            Supplier<List<String>> fetchFiles,
            BiFunction<String,Long,File> uploadCreatedZipTo,
            String usersBucketName
    ) {
        Set<String> mappedNames = new HashSet<>();

        Path tempZip = null;
        try {
            tempZip = Files.createTempFile("archive-", ".zip");
            try (
                    ZipOutputStream zipOut = new ZipOutputStream(Files.newOutputStream(tempZip));
                    S3Client s3Client = S3Client.builder().build();
            ) {
                fetchFiles.get().forEach(s3Key -> {
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

            File savedFile = uploadCreatedZipTo.apply(zipFileName,zipSize);

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
        } catch (IOException ex) {
            logger.error("an io exception while processing file's",ex);
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
    }

    @Override
    public void deleteS3File(
            String usersBucketName,
            String s3key
    ) {
        try(S3Client s3Client = S3Client.builder().build()) {
            s3Client.deleteObject(
                    DeleteObjectRequest.builder()
                            .bucket(usersBucketName)
                            .key(s3key)
                            .build()
            );
        }
    }

    @Override
    public void bulkDeleteS3File(
            String usersBucketName,
            List<String> s3keys
    ) {
        try(S3Client s3Client = S3Client.builder().build()) {
            List<ObjectIdentifier> objects = s3keys.stream().map(s3key ->
                ObjectIdentifier.builder().key(s3key).build()
            ).toList();
            DeleteObjectsRequest request = DeleteObjectsRequest.builder()
                    .bucket(usersBucketName)
                    .delete(Delete.builder()
                            .objects(objects)
                            .build())
                    .build();
            s3Client.deleteObjects(request);
        }
    }
}
