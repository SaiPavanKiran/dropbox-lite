package org.rspk.dropbox_lite.model.folders;

import jakarta.persistence.*;
import org.rspk.dropbox_lite.model.common.Temporal;

import java.util.UUID;

/**
 * {@snippet :
 *  CREATE TABLE folders (
 *      folder_id UUID PRIMARY KEY,
 *      account_id UUID NOT NULL,
 *      parent_folder_id UUID,
 *      name VARCHAR(255) NOT NULL,
 *      created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now(),
 *      updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now(),
 *      CONSTRAINT fk_folders_account_id FOREIGN KEY (account_id) REFERENCES accounts(account_id),
 *      CONSTRAINT fk_folders_parent_folder_id FOREIGN KEY (parent_folder_id) REFERENCES folders(folder_id)
 *  );
 * }
 */

@Entity
@Table(name = "folders")
public class Folder extends Temporal {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID folderId;
    @Column(nullable = false)
    UUID accountId;
    UUID parentFolderId;
    @Column(nullable = false, length = 255)
    String name;

    public Folder(UUID accountId, UUID parentFolderId, String name) {
        this.accountId = accountId;
        this.parentFolderId = parentFolderId;
        this.name = name;
    }

    public Folder() {}

    public UUID getFolderId() {
        return folderId;
    }

    public void setFolderId(UUID folderId) {
        this.folderId = folderId;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public UUID getParentFolderId() {
        return parentFolderId;
    }

    public void setParentFolderId(UUID parentFolderId) {
        this.parentFolderId = parentFolderId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
