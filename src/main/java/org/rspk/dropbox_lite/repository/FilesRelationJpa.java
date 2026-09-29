package org.rspk.dropbox_lite.repository;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.rspk.dropbox_lite.model.objects.FilesRelation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class FilesRelationJpa {

    private final SessionFactory sessionFactory;

    @Autowired
    public FilesRelationJpa(
            SessionFactory sessionFactory
    ) {
        this.sessionFactory = sessionFactory;
    }

    public FilesRelation save(FilesRelation filesRelation) {
        Session session = sessionFactory.getCurrentSession();
        session.persist(filesRelation);
        return filesRelation;
    }

    public List<FilesRelation> findByParent(UUID parentId, UUID accountId, int page, int size) {
        Session session = sessionFactory.getCurrentSession();

        StringBuilder hql = new StringBuilder(
                String.format("FROM %s o WHERE o.accountId=:accountId ", FilesRelation.class.getName())
        );

        if (parentId == null) hql.append("AND o.id.parentId IS NULL");
        else hql.append("AND o.id.parentId=:parentId");

        var query = session.createSelectionQuery(hql.toString(), FilesRelation.class)
                .setParameter("accountId",accountId);

        query.setFirstResult(page * size);
        query.setMaxResults(size);


        if(parentId != null) query.setParameter("parentId",parentId);
        return query.getResultList();
    }

    public List<UUID> findByParent(List<UUID> parentIds, UUID accountId) {
        Session session = sessionFactory.getCurrentSession();

        StringBuilder hql = new StringBuilder(
                String.format("SELECT o.id.objectId FROM %s o WHERE o.accountId=:accountId AND o.id.parentId IN (:parentIds)", FilesRelation.class.getName())
        );

        var query = session.createSelectionQuery(hql.toString(), UUID.class)
                .setParameter("accountId",accountId)
                .setParameter("parentIds",parentIds);
        return query.getResultList();
    }

    public void delete(UUID parentId,UUID objectId,String name,UUID accountId) {
        Session session = sessionFactory.getCurrentSession();

        StringBuilder hql = new StringBuilder(
                String.format("DELETE FROM %s o WHERE id.objectId=:objectId AND id.name=:name AND accountId=:accountId", FilesRelation.class.getName())
        );

        if (parentId == null) hql.append("AND o.id.parentId IS NULL");
        else hql.append("AND o.id.parentId=:parentId");

        var query = session.createMutationQuery(hql.toString())
                .setParameter("accountId",accountId)
                .setParameter("objectId",objectId)
                .setParameter("name",name);

        if(parentId != null) query.setParameter("parentId",parentId);

        query.executeUpdate();
    }


}
