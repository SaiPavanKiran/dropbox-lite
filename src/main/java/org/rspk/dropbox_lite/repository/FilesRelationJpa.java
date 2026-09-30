package org.rspk.dropbox_lite.repository;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.rspk.dropbox_lite.model.objects.FilesRelation;
import org.rspk.dropbox_lite.model.objects.ObjectType;
import org.rspk.dropbox_lite.utils.logs.CommonLogging;
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

    public long countByParent(UUID parentId, UUID accountId) {
        Session session = sessionFactory.getCurrentSession();

        StringBuilder hql = new StringBuilder(
                String.format("SELECT COUNT(o) FROM %s o WHERE o.accountId=:accountId ", FilesRelation.class.getName())
        );

        if (parentId == null) hql.append("AND o.parentId IS NULL");
        else hql.append("AND o.parentId=:parentId");

        var query = session.createSelectionQuery(hql.toString(), Long.class)
                .setParameter("accountId",accountId);


        if(parentId != null) query.setParameter("parentId",parentId);
        return query.getSingleResult();
    }

    public FilesRelation findByObject(UUID objectId, UUID parentId, UUID accountId, ObjectType type,String name) {
        Session session = sessionFactory.getCurrentSession();


        StringBuilder hql = new StringBuilder(
                String.format("FROM %s o WHERE o.accountId=:accountId AND o.type=:type AND o.name=:name AND o.objectId=:objectId ", FilesRelation.class.getName())
        );

        if (parentId == null) hql.append("AND o.parentId IS NULL");
        else hql.append("AND o.parentId=:parentId");

        var query = session.createSelectionQuery(hql.toString(), FilesRelation.class)
                .setParameter("accountId",accountId)
                .setParameter("type",type)
                .setParameter("name",name)
                .setParameter("objectId",objectId);



        if(parentId != null) query.setParameter("parentId",parentId);
        return query.getSingleResult();
    }

    public FilesRelation findByName(UUID parentId, UUID accountId, ObjectType type,String name) {
        Session session = sessionFactory.getCurrentSession();

        StringBuilder hql = new StringBuilder(
                String.format("SELECT o FROM %s o WHERE o.accountId=:accountId AND o.type=:type AND o.name=:name ", FilesRelation.class.getName())
        );

        if (parentId == null) hql.append("AND o.parentId IS NULL");
        else hql.append("AND o.parentId=:parentId");

        var query = session.createSelectionQuery(hql.toString(), FilesRelation.class)
                .setParameter("accountId",accountId)
                .setParameter("type",type)
                .setParameter("name",name);
        if(parentId != null) query.setParameter("parentId",parentId);
        return query.uniqueResult();
    }

    public List<FilesRelation> findByParent(UUID parentId, UUID accountId, int page, int size) {
        Session session = sessionFactory.getCurrentSession();

        StringBuilder hql = new StringBuilder(
                String.format("FROM %s o WHERE o.accountId=:accountId ", FilesRelation.class.getName())
        );

        if (parentId == null) hql.append("AND o.parentId IS NULL");
        else hql.append("AND o.parentId=:parentId");

        hql.append(" ORDER BY CASE WHEN o.type='FOLDER' THEN 0 ELSE 1 END,name");

        var query = session.createSelectionQuery(hql.toString(), FilesRelation.class)
                .setParameter("accountId",accountId);

        query.setFirstResult(page * size);
        query.setMaxResults(size + 1);


        if(parentId != null) query.setParameter("parentId",parentId);
        return query.getResultList();
    }


    public List<UUID> findByParentIds(List<UUID> parentIds, UUID accountId) {
        Session session = sessionFactory.getCurrentSession();

        StringBuilder hql = new StringBuilder(
                String.format("SELECT o.objectId FROM %s o WHERE o.accountId=:accountId AND o.parentId IN (:parentIds)", FilesRelation.class.getName())
        );

        var query = session.createSelectionQuery(hql.toString(), UUID.class)
                .setParameter("accountId",accountId)
                .setParameter("parentIds",parentIds);
        return query.getResultList();
    }



    public void delete(UUID parentId,UUID objectId,String name,UUID accountId) {
        Session session = sessionFactory.getCurrentSession();

        StringBuilder hql = new StringBuilder(
                String.format("DELETE FROM %s o WHERE o.objectId=:objectId AND o.name=:name AND o.accountId=:accountId", FilesRelation.class.getName())
        );

        if (parentId == null) hql.append("AND o.parentId IS NULL");
        else hql.append("AND o.parentId=:parentId");

        var query = session.createMutationQuery(hql.toString())
                .setParameter("accountId",accountId)
                .setParameter("objectId",objectId)
                .setParameter("name",name);

        if(parentId != null) query.setParameter("parentId",parentId);

        query.executeUpdate();
    }

    public void delete(FilesRelation filesRelation) {
        Session session = sessionFactory.getCurrentSession();
        session.remove(filesRelation);
    }


}
