package org.rspk.dropbox_lite.model.objects;

import jakarta.persistence.*;

import java.util.UUID;

/**
 * {@snippet :
 *  CREATE TABLE parent_object_relation (
 *          id BIGSERIAL PRIMARY KEY,
 *          parent_id UUID,
 *          object_id UUID NOT NULL,
 *          name VARCHAR(255) NOT NULL,
 *          account_id UUID NOT NULL,
 *          _type VARCHAR(50) NOT NULL,
 *          created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now(),
 *          updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now(),
 *          CONSTRAINT unique_parent_object_relation_parent_id_object_id_name UNIQUE (parent_id,object_id,name),
 *          CONSTRAINT fk_parent_object_relation_account_id FOREIGN KEY (account_id) REFERENCES accounts(account_id),
 *          CONSTRAINT fk_parent_object_relation_parent_id FOREIGN KEY (parent_id) REFERENCES folders(folder_id) ON DELETE CASCADE,
 *          CONSTRAINT check_parent_object_relation__type CHECK ( _type IN ('FOLDER', 'FILE'))
 *  );
 * }
 */

@Entity
@Table(
        name = "parent_object_relation",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "unique_parent_object_relation_parent_id_object_id_name",
                        columnNames = { "parent_id","object_id", "name"}
                )
        }
)
public class FilesRelation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private UUID parentId;
    @Column(nullable = false)
    private UUID objectId;
    @Column(nullable = false,length = 255)
    private String name;
    @Column(nullable = false)
    private UUID accountId;
    @Column(nullable = false,length = 50,name = "_type")
    @Enumerated(EnumType.STRING)
    private ObjectType type;

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
        this.parentId = parentId;
        this.objectId = objectId;
        this.name = name;
        this.type = type;
    }


    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public UUID getParentId() {
        return parentId;
    }

    public void setParentId(UUID parentId) {
        this.parentId = parentId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UUID getObjectId() {
        return objectId;
    }

    public void setObjectId(UUID objectId) {
        this.objectId = objectId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

