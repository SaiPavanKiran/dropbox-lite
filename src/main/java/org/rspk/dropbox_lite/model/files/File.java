package org.rspk.dropbox_lite.model.files;

import jakarta.persistence.*;
import org.rspk.dropbox_lite.model.common.Temporal;
import java.util.UUID;

/**
 * {@snippet :
 *  CREATE TABLE files (
 *      file_id UUID PRIMARY KEY,
 *      account_id UUID NOT NULL,
 *      folder_id UUID NOT NULL,
 *      s3_key VARCHAR(1024) NOT NULL,
 *      name VARCHAR(255) NOT NULL,
 *      content_type VARCHAR(255) NOT NULL,
 *      size BIGINT NOT NULL DEFAULT 0,
 *      archived BOOLEAN NOT NULL DEFAULT false,
 *      upload_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
 *      created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now(),
 *      updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now(),
 *      CONSTRAINT fk_files_account_id FOREIGN KEY (account_id) REFERENCES accounts(account_id),
 *      CONSTRAINT fk_files_folder_id FOREIGN KEY (folder_id) REFERENCES folders(folder_id),
 *      CONSTRAINT check_files_upload_status CHECK (upload_status IN ('PENDING','COMPLETED','FAILED'))
 *  );
 * }
 */

@Entity
@Table(name = "files")
public class File extends Temporal {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID fileId;
    @Column(nullable = false)
    private UUID accountId;
    private UUID folderId;
    @Column(nullable = false, length = 1024)
    private String s3Key;
    @Column(nullable = false, length = 255)
    private String name;
    @Column(length = 255,nullable = false)
    private String contentType;
    @Column(nullable = false)
    private Long size;
    @Column(nullable = false)
    private Boolean archived = false;
    @Enumerated(value = EnumType.STRING)
    private UploadStatus uploadStatus;


    public File(
            UUID accountId,
            UUID folderId,
            String s3Key,
            String name,
            String contentType,
            Long size,
            Boolean archived,
            UploadStatus uploadStatus
    ) {
        this.accountId = accountId;
        this.folderId = folderId;
        this.s3Key = s3Key;
        this.name = name;
        this.contentType = contentType;
        this.size = size;
        this.archived = archived;
        this.uploadStatus = uploadStatus;
    }

    public File() {}

    public UUID getFileId() {
        return fileId;
    }

    public void setFileId(UUID fileId) {
        this.fileId = fileId;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public UUID getFolderId() {
        return folderId;
    }

    public void setFolderId(UUID folderId) {
        this.folderId = folderId;
    }

    public String getS3Key() {
        return s3Key;
    }

    public void setS3Key(String s3Key) {
        this.s3Key = s3Key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public Boolean getArchived() {
        return archived;
    }

    public void setArchived(Boolean archived) {
        this.archived = archived;
    }

    public UploadStatus getUploadStatus() {
        return uploadStatus;
    }

    public void setUploadStatus(UploadStatus uploadStatus) {
        this.uploadStatus = uploadStatus;
    }
}
