package org.rspk.dropbox_lite.repository;

import org.rspk.dropbox_lite.model.account.Account;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.rspk.dropbox_lite.model.account.AccountLogin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/* For practice, we use SessionFactory instead of Spring Data JPA to write SQL queries manually. */
@Repository
public class AccountJpa {

    private final SessionFactory sessionFactory;

    @Autowired
    public AccountJpa(
            SessionFactory sessionFactory
    ) {
        this.sessionFactory = sessionFactory;
    }

    public Account save(Account account) {
        Session session = sessionFactory.getCurrentSession();
        /*session closed via transactional*/
        session.persist(account);
        return account;/*hibernate will populate all generated ids and timestamps*/
    }

    public Optional<Account> findByEmail(String email) {
        Session session = sessionFactory.getCurrentSession();

        Account res = session.createQuery(
                String.format("FROM %s a WHERE a.email= :email",Account.class.getName()),
                Account.class
        ).setParameter("email",email)
                .uniqueResult();
        if(res == null) return Optional.empty(); else return Optional.of(res);
    }

    public List<Account> findByIds(List<UUID> ids) {
        Session session = sessionFactory.getCurrentSession();

        return session.createQuery(
                        String.format("FROM %s a WHERE a.accountId IN (:accountIds)",Account.class.getName()),
                        Account.class
                ).setParameter("accountIds",ids)
                .getResultList();
    }

    public void delete(Account account) {
        Session session = sessionFactory.getCurrentSession();
        session.remove(account);

    }
}
