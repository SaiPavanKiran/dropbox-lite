package org.rspk.dropbox_lite.model.objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;
import java.util.UUID;



/*@Embeddable
public class FilesRelationId {
    private UUID parentId;
    @Column(nullable = false)
    private UUID objectId;
    @Column(nullable = false,length = 255)
    private String name;

    public FilesRelationId() {}
    public FilesRelationId(
            UUID parentId,
            UUID objectId,
            String name
    ) {
        this.parentId = parentId;
        this.objectId = objectId;
        this.name = name;
    }

    public UUID getParentId() {
        return parentId;
    }

    public void setParentId(UUID parentId) {
        this.parentId = parentId;
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

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof FilesRelationId that)) return false;

        return Objects.equals(parentId, that.parentId)
                && Objects.equals(objectId, that.objectId)
                && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parentId,objectId,name);
    }
}*/
