package org.rspk.dropbox_lite.repository;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.rspk.dropbox_lite.model.folders.Folder;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class FolderJpa {

    private final SessionFactory sessionFactory;

    public FolderJpa(
            SessionFactory sessionFactory
    ) {
        this.sessionFactory = sessionFactory;
    }


    public Folder save(Folder folder) {
        Session session = sessionFactory.getCurrentSession();
        session.persist(folder);
        return folder;
    }


    public Optional<Folder> findById(UUID folderId,UUID accountId) {
        Session session = sessionFactory.getCurrentSession();
        Folder folder = session.find(Folder.class,folderId);
        if(folder != null) {
            if(!folder.getAccountId().equals(accountId)) return Optional.empty();
            return Optional.of(folder);
        }
        else return Optional.empty();
    }

    public List<UUID> findIdsByParentFolderIdRecursively(
            UUID folderId,
            UUID accountId
    ) {
        Session session = sessionFactory.getCurrentSession();

        String sql = """
            WITH RECURSIVE folder_tree AS (
                SELECT folder_id
                FROM folders
                WHERE parent_folder_id = :folderId
                  AND account_id = :accountId

                UNION ALL

                SELECT f.folder_id
                FROM folders f
                JOIN folder_tree ft
                    ON f.parent_folder_id = ft.folder_id
                WHERE f.account_id = :accountId
            )
            SELECT folder_id
            FROM folder_tree
            """;

        return session
                .createNativeQuery(sql, UUID.class)
                .setParameter("folderId", folderId)
                .setParameter("accountId", accountId)
                .getResultList();
    }


    public List<Folder> findFoldersByParent(
            UUID parentFolderId,
            UUID accountId,
            int page,
            int size
    ) {
        Session session = sessionFactory.getCurrentSession();

        String condition = parentFolderId != null
                ? " AND f.parentFolderId=:parentFolderId" : " AND f.parentFolderId IS NULL";
        String hql = String.format(
                "SELECT f FROM %s f WHERE f.accountId=:accountId %s",
                Folder.class.getName(),
                condition
        );


        var query = session.createSelectionQuery(hql,Folder.class)
                .setParameter("accountId",accountId);

        if(parentFolderId != null) query.setParameter("parentFolderId",parentFolderId);

        query.setFirstResult(page * size)
                .setMaxResults(size);

        return query.getResultList();
    }


    public List<Folder> findByIds(
            List<UUID> folderIds,
            UUID accountId
    ) {
        Session session = sessionFactory.getCurrentSession();

        String hql = String.format(
                "FROM %s f WHERE f.accountId=:accountId AND folderId IN :folderIds ORDER BY name",
                Folder.class.getName()
        );


        var query = session.createSelectionQuery(hql,Folder.class)
                .setParameter("accountId",accountId)
                .setParameter("folderIds",folderIds);

        return query.getResultList();
    }

    public Boolean existsById(UUID folderId,UUID accountId) {
        Session session = sessionFactory.getCurrentSession();

        String hql = String.format(
                "SELECT EXISTS (FROM %s f WHERE f.folderId=:folderId AND f.accountId=:accountId)",
                Folder.class.getName()
        );

        return session.createSelectionQuery(hql, Boolean.class)
                .setParameter("accountId",accountId)
                .setParameter("folderId",folderId)
                .uniqueResult();
    }

    public Folder findByName(String name,UUID parentFolderId,UUID accountId) {
        Session session = sessionFactory.getCurrentSession();

        StringBuilder hql = new StringBuilder(String.format(
                "FROM %s f WHERE LOWER(f.name)=LOWER(:name) AND f.accountId=:accountId",
                Folder.class.getName()
        ));

        if(parentFolderId != null) hql.append(" AND f.parentFolderId=:parentFolderId");
        else hql.append(" AND f.parentFolderId IS NULL");

        var query = session.createSelectionQuery(hql.toString(), Folder.class)
                .setParameter("accountId",accountId)
                .setParameter("name",name);

        if(parentFolderId != null) query.setParameter("parentFolderId",parentFolderId);

        return query.uniqueResult();
    }

    public Boolean existsByName(String name,UUID parentFolderId,UUID accountId) {
        Session session = sessionFactory.getCurrentSession();

        StringBuilder hql = new StringBuilder(String.format(
                "SELECT EXISTS (FROM %s f WHERE LOWER(f.name)=LOWER(:name) AND f.accountId=:accountId",
                Folder.class.getName()
        ));

        if(parentFolderId != null) hql.append(" AND f.parentFolderId=:parentFolderId)");
        else hql.append(" AND f.parentFolderId IS NULL)");

        var query = session.createSelectionQuery(hql.toString(), Boolean.class)
                .setParameter("accountId",accountId)
                .setParameter("name",name);

        if(parentFolderId != null) query.setParameter("parentFolderId",parentFolderId);

        return query.uniqueResult();
    }


    public void delete(UUID folderId,UUID accountId) {
        Session session = sessionFactory.getCurrentSession();

        String hql = String.format(
                "DELETE FROM %s f WHERE f.folderId=:folderId AND f.accountId=:accountId",
                Folder.class.getName()
        );

        session.createMutationQuery(hql)
                .setParameter("accountId",accountId)
                .setParameter("folderId",folderId)
                .executeUpdate();
    }

}
