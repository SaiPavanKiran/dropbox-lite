package org.rspk.dropbox_lite.model.share;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.UUID;

@Embeddable
public class ShareObjectId {
    @Column(nullable = false)
    private UUID ownerId;
    @Column(nullable = false)
    private UUID objectId;
    @Column(length = 255,nullable = false)
    private String objectName;
    @Column(nullable = false)
    private UUID recipientId;

    public ShareObjectId() {}

    public ShareObjectId(
            UUID ownerId,
            UUID objectId,
            String objectName,
            UUID recipientId
    ) {
        this.ownerId = ownerId;
        this.objectId = objectId;
        this.objectName = objectName;
        this.recipientId = recipientId;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public UUID getObjectId() {
        return objectId;
    }

    public void setObjectId(UUID objectId) {
        this.objectId = objectId;
    }

    public UUID getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(UUID recipientId) {
        this.recipientId = recipientId;
    }

    public String getObjectName() {
        return objectName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }
}
