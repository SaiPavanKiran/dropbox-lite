package org.rspk.dropbox_lite.model.share;

import jakarta.persistence.*;
import org.rspk.dropbox_lite.model.common.Temporal;

import java.time.Instant;
import java.util.UUID;

    /**
 * {@snippet :
 *  CREATE TABLE shared_object (
 *      owner_id UUID NOT NULL,
 *      object_id UUID NOT NULL,
 *      object_name VARCHAR(255) NOT NULL,
 *      recipient_id UUID NOT NULL,
 *      expiry TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
 *      created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now(),
 *      updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now(),
 *      CONSTRAINT fk_shared_object_owner_id FOREIGN KEY (owner_id) REFERENCES accounts(account_id),
 *      CONSTRAINT fk_shared_object_recipient_id FOREIGN KEY (recipient_id) REFERENCES accounts(account_id)
 *  );
 * }
 */

@Entity
@Table(name = "shared_object")
public class ShareObject extends Temporal {

    @EmbeddedId
    private ShareObjectId id;
    private Instant expiry;
//    String permission;

    public ShareObject() {}

    public ShareObject(
            UUID ownerId,
            UUID objectId,
            String name,
            UUID recipientId,
            Instant expiry
    ) {
        this.id = new ShareObjectId(
                ownerId,
                objectId,
                name,
                recipientId
        );
        this.expiry = expiry;
    }

    public ShareObjectId getId() {
        return id;
    }

    public void setId(ShareObjectId id) {
        this.id = id;
    }

    public Instant getExpiry() {
        return expiry;
    }

    public void setExpiry(Instant expiry) {
        this.expiry = expiry;
    }

}
