package org.rspk.dropbox_lite.service;

import org.rspk.dropbox_lite.model.files.File;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;

@Service
public interface S3DependentService {
    void save(
            String usersBucketName,
            String s3key,
            String contentType,
            long size,
            Map<String, String> metadata,
            MultipartFile file
    ) throws IOException;

    URL getPreSignedUrl(
            String usersBucketName,
            String s3key,
            long signDurationInMin
    );

    void createZipOfFiles(
            Supplier<List<String>> fetchFiles,
            BiFunction<String,Long, File> uploadCreatedZipTo,
            String usersBucketName
    );

    void deleteS3File(
            String usersBucketName,
            String s3key
    );

    void bulkDeleteS3File(
            String usersBucketName,
            List<String> s3keys
    );
}
