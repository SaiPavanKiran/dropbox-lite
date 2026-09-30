package org.rspk.dropbox_lite.repository;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.QueryParameter;
import org.rspk.dropbox_lite.model.files.File;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class FileJpa {

    private SessionFactory sessionFactory;

    public FileJpa (
            SessionFactory sessionFactory
    ) {
        this.sessionFactory = sessionFactory;
    }

    public File save(File fileMetadata) {
        Session session = sessionFactory.getCurrentSession();
        session.persist(fileMetadata);
        return fileMetadata;
    }

    public Optional<File> findById(UUID fileId,UUID accountId) {
        Session session = sessionFactory.getCurrentSession();
        File file = session.find(File.class,fileId);

        if(file != null) {
            if(!file.getAccountId().equals(accountId)) return Optional.empty();
            return Optional.of(file);
        } else return Optional.empty();
    }


    public long countFilesByFolder(UUID folderId,UUID accountId) {
        Session session = sessionFactory.getCurrentSession();

        StringBuilder hql = new StringBuilder(
                String.format("SELECT COUNT(f) FROM %s f WHERE f.accountId=:accountId", File.class.getName())
        );

        if (folderId != null) {
            hql.append(" AND f.folderId = :folderId");
        } else {
            hql.append(" AND f.folderId IS NULL");
        }

        var query = session.createSelectionQuery(hql.toString(), Long.class)
                .setParameter("accountId",accountId);
        if (folderId != null) {
            query.setParameter("folderId", folderId);
        }

        return query.getSingleResult();
    }

    public List<File> findFilesByFolder(
            UUID folderId,
            UUID accountId,
            int page,
            int size,
            int startAt
    ) {
        Session session = sessionFactory.getCurrentSession();

        String condition = folderId != null ? " AND f.folderId=:folderId" : " AND f.folderId IS NULL";
        String hql = String.format(
                "SELECT f FROM %s f WHERE f.accountId=:accountId %s ",
                File.class.getName(),
                condition
        );


        var query = session.createSelectionQuery(hql,File.class)
                .setParameter("accountId",accountId);

        if(folderId != null) query.setParameter("folderId",folderId);

        int calculatedStart = startAt + page * size;
        query.setFirstResult(calculatedStart);
        query.setMaxResults(size);

        return query.getResultList();
    }


    public List<File> findByIds(
            List<UUID> fileIds,
            UUID accountId
    ) {
        Session session = sessionFactory.getCurrentSession();

        String hql = String.format(
                "FROM %s f WHERE f.accountId=:accountId AND f.fileId IN :fileIds ORDER BY name",
                File.class.getName()
        );


        var query = session.createSelectionQuery(hql,File.class)
                .setParameter("accountId",accountId)
                .setParameter("fileIds",fileIds);

        return query.getResultList();
    }

    public List<UUID> getIdsByAccountId(
            UUID accountId
    ) {
        Session session = sessionFactory.getCurrentSession();

        String hql = String.format(
                "SELECT fileId FROM %s f WHERE f.accountId=:accountId",
                File.class.getName()
        );


        return session.createSelectionQuery(hql,UUID.class)
                .setParameter("accountId",accountId)
                .getResultList();
    }


    public List<String> findS3KeysByIds(
            List<UUID> fileIds,
            UUID accountId
    ) {
        Session session = sessionFactory.getCurrentSession();

        String hql = String.format(
                "SELECT f.s3Key FROM %s f WHERE f.accountId=:accountId AND f.fileId IN :fileIds",
                File.class.getName()
        );


        var query = session.createSelectionQuery(hql,String.class)
                .setParameter("accountId",accountId)
                .setParameter("fileIds",fileIds);

        return query.getResultList();
    }

    public List<String> findS3KeysByParentId(
            UUID parentId,
            UUID accountId
    ) {
        Session session = sessionFactory.getCurrentSession();

        String hql = String.format(
                "SELECT f.s3Key FROM %s f WHERE f.accountId=:accountId AND f.folderId=folderId",
                File.class.getName()
        );


        var query = session.createSelectionQuery(hql,String.class)
                .setParameter("accountId",accountId)
                .setParameter("folderId",parentId);

        return query.getResultList();
    }

    public Long countByName(String name,UUID folderId,UUID accountId) {
        Session session = sessionFactory.getCurrentSession();

        StringBuilder hql = new StringBuilder();
        hql.append(String.format(
                "SELECT COUNT(f) FROM %s f WHERE LOWER(f.name)=LOWER(:name) AND f.accountId=:accountId",
                File.class.getName()
        ));

        if(folderId != null) hql.append(" AND f.folderId=:folderId");
        else hql.append(" AND f.folderId IS NULL");

        var query = session.createSelectionQuery(hql.toString(), Long.class)
                .setParameter("accountId",accountId)
                .setParameter("name",name);
        if(folderId != null) query.setParameter("folderId",folderId);

        return query.getSingleResult();
    }

    public void deleteById(UUID fileId,UUID accountId) {
        Session session = sessionFactory.getCurrentSession();

        String hql = String.format(
                "DELETE FROM %s f WHERE f.fileId = :fileId AND f.accountId=:accountId",
                File.class.getName()
        );

        session.createMutationQuery(hql)
                .setParameter("accountId",accountId)
                .setParameter("fileId", fileId)
                .executeUpdate();
    }

}
