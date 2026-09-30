package org.rspk.dropbox_lite.model.account;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.rspk.dropbox_lite.model.common.Temporal;

import java.time.Instant;
import java.util.UUID;

/**
 * {@snippet :
 * CREATE TABLE accounts (
 *     account_id UUID PRIMARY KEY,
 *     email VARCHAR(350) NOT NULL,
 *     password TEXT NOT NULL,
 *     created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now(),
 *     updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now()
 * );
 * }
 */

@Entity
@Table(name = "accounts")
public class Account extends Temporal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID accountId;
    @Column(nullable = false,length = 350)
    private String email;
    @Column(nullable = false)
    private String password;

    public Account(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public Account() {}

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}

