package org.rspk.dropbox_lite.model.otp;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * {@snippet :
 *  CREATE TABLE otp_requests (
 *      email VARCHAR(350) PRIMARY KEY,
 *      otp VARCHAR(8) NOT NULL,
 *      expired_at TIMESTAMP WITHOUT TIME ZONE NOT NULL
 *  );
 * }
 */

@Entity
@Table(name = "otp_requests")
public class OtpRequest {

    @Id
    @Column(length = 350)
    private String email;
    @Column(nullable = false,length = 8)
    private String otp;
    @Column(nullable = false)
    private Instant expiredAt;

    public OtpRequest(String email, String otp, Instant expiredAt) {
        this.email = email;
        this.otp = otp;
        this.expiredAt = expiredAt;
    }

    public OtpRequest() {}

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

    public Instant getExpiredAt() {
        return expiredAt;
    }

    public void setExpiredAt(Instant expiredAt) {
        this.expiredAt = expiredAt;
    }
}
