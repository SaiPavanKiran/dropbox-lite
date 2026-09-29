package org.rspk.dropbox_lite.repository;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.rspk.dropbox_lite.model.otp.OtpRequest;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class OtpJpa {

    private final SessionFactory sessionFactory;

    public OtpJpa(
            SessionFactory sessionFactory
    ) {
        this.sessionFactory = sessionFactory;
    }

    public OtpRequest save(OtpRequest otpRequest) {
        Session session = sessionFactory.getCurrentSession();
        session.persist(otpRequest);
        return otpRequest;
    }


    public Optional<OtpRequest> findById(String email) {
        Session session = sessionFactory.getCurrentSession();
        OtpRequest otp = session.find(OtpRequest.class,email);
        if(otp == null) return Optional.empty();
        else return Optional.of(otp);
    }

    public void delete(OtpRequest otpRequest) {
        Session session = sessionFactory.getCurrentSession();
        session.remove(otpRequest);
    }

}
