package org.rspk.dropbox_lite.model.objects;

import jakarta.persistence.*;

import java.util.UUID;

/**
 * {@snippet :
 *  CREATE TABLE parent_object_relation (
 *          parent_id UUID,
 *          object_id UUID NOT NULL,
 *          name VARCHAR(255) NOT NULL,
 *          account_id UUID NOT NULL,
 *          _type VARCHAR(50) NOT NULL,
 *          created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now(),
 *          updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now(),
 *          CONSTRAINT ck_parent_object_relation_id PRIMARY KEY (parent_id,object_id,name),
 *          CONSTRAINT fk_parent_object_relation_account_id FOREIGN KEY (account_id) REFERENCES accounts(account_id),
 *          CONSTRAINT fk_parent_object_relation_parent_id FOREIGN KEY (parent_id) REFERENCES folders(folder_id),
 *          CONSTRAINT check_parent_object_relation__type CHECK ( _type IN ('FOLDER', 'FILE'))
 *  );
 * }
 */

@Entity
@Table(name = "parent_object_relation")
public class FilesRelation {


    @EmbeddedId
    private FilesRelationId id;
    @Column(nullable = false)
    private UUID accountId;
    @Column(nullable = false,length = 50,name = "_type")
    @Enumerated(EnumType.STRING)
    private ObjectType type;

    public FilesRelationId getObjectRelationId() {
        return id;
    }

    public void setObjectRelationId(FilesRelationId filesRelationId) {
        this.id = filesRelationId;
    }

    public ObjectType getType() {
        return type;
    }

    public void setType(ObjectType type) {
        this.type = type;
    }

    public FilesRelation() {}
    public FilesRelation(
            UUID accountId,
            UUID parentId,
            UUID objectId,
            String name,
            ObjectType type
    ) {
        this.accountId = accountId;
        this.id = new FilesRelationId(parentId,objectId,name);
        this.type = type;
    }


    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }
}

