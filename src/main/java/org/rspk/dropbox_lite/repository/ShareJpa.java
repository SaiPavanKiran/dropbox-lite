package org.rspk.dropbox_lite.repository;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.rspk.dropbox_lite.model.share.ShareObject;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class ShareJpa {

    private final SessionFactory sessionFactory;

    public ShareJpa(
            SessionFactory sessionFactory
    ) {
        this.sessionFactory = sessionFactory;
    }


    public ShareObject save(ShareObject shareObject) {
        Session session = sessionFactory.getCurrentSession();
        session.persist(shareObject);
        return shareObject;
    }


    public List<ShareObject> findByRecipientAccountId(UUID recipientId,int page,int size) {
        Session session = sessionFactory.getCurrentSession();
        String hql = String.format(
                "FROM %s s WHERE s.id.recipientId=:recipientId AND s.expiry > :currentTime",
                ShareObject.class.getName()
        );

        return session.createSelectionQuery(hql,ShareObject.class)
                .setParameter("recipientId", recipientId)
                .setParameter("currentTime", Instant.now())
                .setFirstResult(page * size)
                .setMaxResults(size + 1)
                .getResultList();
    }


    public List<ShareObject> findByOwnerAccountId(UUID ownerId,int page,int size) {
        Session session = sessionFactory.getCurrentSession();
        String hql = String.format(
                "FROM %s s WHERE s.id.ownerId=:ownerId",
                ShareObject.class.getName()
        );

        return session.createSelectionQuery(hql,ShareObject.class)
                .setParameter("ownerId",ownerId)
                .setFirstResult(page * size)
                .setMaxResults(size + 1)
                .getResultList();
    }

    public ShareObject findSharedObjectByRecipientBy(UUID ownerId,UUID recipientId,UUID objectId) {
        Session session = sessionFactory.getCurrentSession();
        String hql = String.format(
                "FROM %s s WHERE s.id.ownerId=:ownerId AND s.id.recipientId=:recipientId AND s.id.objectId=:objectId",
                ShareObject.class.getName()
        );

        return session.createSelectionQuery(hql,ShareObject.class)
                .setParameter("ownerId",ownerId)
                .setParameter("recipientId",recipientId)
                .setParameter("objectId",objectId)
                .uniqueResult();
    }

    public boolean hasObjectShared(UUID recipientId,UUID objectId) {
        Session session = sessionFactory.getCurrentSession();
        String hql = String.format(
                "SELECT EXISTS (SELECT 1 FROM %s s WHERE s.id.recipientId=:recipientId AND s.expiry > :currentTime AND s.id.objectId=:objectId)",
                ShareObject.class.getName()
        );

        return session.createSelectionQuery(hql,Boolean.class)
                .setParameter("recipientId",recipientId)
                .setParameter("currentTime", Instant.now())
                .setParameter("objectId",objectId)
                .uniqueResult();
    }

    public void deleteSharedObject(UUID ownerId, UUID recipientId, UUID objectId) {
        Session session = sessionFactory.getCurrentSession();
        String hql = String.format(
                "DELETE FROM %s s WHERE s.id.ownerId=:ownerId AND s.id.recipientId=:recipientId AND s.id.objectId=:objectId",
                ShareObject.class.getName()
        );

        session.createMutationQuery(hql)
                .setParameter("ownerId", ownerId)
                .setParameter("recipientId", recipientId)
                .setParameter("objectId", objectId)
                .executeUpdate();
    }

}
