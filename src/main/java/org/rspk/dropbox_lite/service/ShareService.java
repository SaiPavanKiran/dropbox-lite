package org.rspk.dropbox_lite.service;

import org.rspk.dropbox_lite.model.account.Account;
import org.rspk.dropbox_lite.model.files.File;
import org.rspk.dropbox_lite.model.share.*;
import org.rspk.dropbox_lite.repository.AccountJpa;
import org.rspk.dropbox_lite.repository.FileJpa;
import org.rspk.dropbox_lite.repository.ShareJpa;
import org.rspk.dropbox_lite.utils.common_functions.StringUtils;
import org.rspk.dropbox_lite.utils.exceptions.ResourceNotFoundException;
import org.rspk.dropbox_lite.utils.logs.CommonLogging;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URL;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ShareService {

    private final ShareJpa shareJpa;
    private final FileJpa fileJpa;
    private final FolderDependentService folderDependentService;
    private final AccountJpa accountJpa;
    private final S3DependentService s3DependentService;

    @Value("${aws.s3.users_bucket}")
    private String usersBucketName;

    public ShareService(
            ShareJpa shareJpa,
            FileJpa fileJpa,
            AccountJpa accountJpa,
            FolderDependentService folderDependentService, S3Service s3Service, S3DependentService s3DependentService
    ){
        this.accountJpa = accountJpa;
        this.shareJpa = shareJpa;
        this.fileJpa = fileJpa;
        this.folderDependentService = folderDependentService;
        this.s3DependentService = s3DependentService;
    }

    @Transactional
    public ShareObject shareObject(
            UUID accountId,
            ShareObjectReq shareObjectReq
    ) {
        Account recipient = accountJpa.findByEmail(shareObjectReq.recipientEmail())
                .orElseThrow(() -> new ResourceNotFoundException("recipient not found"));
        folderDependentService.createOrFindFolderUUID(".share",recipient.getAccountId());
        folderDependentService.createOrFindFolderUUID(".shared",accountId);
        /*we only use files for now*/
        UUID objectId = StringUtils.toUUIDorNull(shareObjectReq.objectId());
        File file = fileJpa.findById(objectId, accountId).orElseThrow(() -> new ResourceNotFoundException("object not found -- we only accept sharing files for now"));
        ShareObject shareObject = shareJpa.findSharedObjectByRecipientBy(accountId, recipient.getAccountId(), objectId);
        Instant expiry = Instant.now().plus(shareObjectReq.shareDurationInMin(), ChronoUnit.MINUTES);
        if (shareObject == null) {
            ShareObject newShared = new ShareObject(
                    accountId,
                    StringUtils.toUUIDorNull(shareObjectReq.objectId()),
                    file.getName(),
                    recipient.getAccountId(),
                    expiry
            );
            return shareJpa.save(newShared);
        }
        shareObject.setExpiry(expiry);
        return shareJpa.save(shareObject);
    }

    @Transactional
    public Slice<SharedObjectsRes> getSharedFiles(
            UUID accountId,
            int page,
            int size
    ) {
        List<ShareObject> shareObjects = shareJpa.findByRecipientAccountId(accountId, page, size);

        CommonLogging.logger.info("the shared object is - {}",shareObjects.stream().map(ShareObject::getId).toList());
        List<UUID> ownerIds = shareObjects.stream()
                .map(shareObject -> shareObject.getId().getOwnerId())
                .toList();

        Map<UUID, String> accountMap = accountJpa.findByIds(ownerIds).stream()
                .collect(Collectors.toMap(Account::getAccountId, Account::getEmail));

        CommonLogging.logger.info("the account map is - {}",accountMap);


        List<SharedObjectsRes> responses =  shareObjects.stream().map(shareObject -> {
                    String ownerEmail;
                    String mappedEmail = accountMap.get(shareObject.getId().getOwnerId());
                    if(mappedEmail != null) ownerEmail = mappedEmail; else ownerEmail = "unknown";
                    return new SharedObjectsRes(
                            ownerEmail,
                            shareObject.getId().getObjectId(),
                            shareObject.getId().getObjectName(),
                            shareObject.getCreatedAt(),
                            shareObject.getUpdatedAt(),
                            shareObject.getExpiry()
                    );
                }
        ).toList();

        return new SliceImpl<>(responses.subList(0,Math.min(responses.size(),size)), PageRequest.of(page,size),responses.size() > size);
    }

    @Transactional
    public Slice<SharingObjectRes> getSharingFiles(
            UUID accountId,
            int page,
            int size
    ) {
        List<ShareObject> shareObjects = shareJpa.findByOwnerAccountId(accountId,page,size);

        List<UUID> recipientIds = shareObjects.stream()
                .map(shareObject -> shareObject.getId().getRecipientId())
                .toList();

        Map<UUID, String> accountMap = accountJpa.findByIds(recipientIds).stream()
                .collect(Collectors.toMap(Account::getAccountId, Account::getEmail));

        List<SharingObjectRes> responses = shareObjects.stream().map(obj -> {
            String recipientEmail;
            String mappedEmail = accountMap.get(obj.getId().getRecipientId());
            if(mappedEmail != null) recipientEmail = mappedEmail; else recipientEmail = "unknown";
            return new SharingObjectRes(
                    obj.getId().getObjectId(),
                    obj.getId().getObjectName(),
                    recipientEmail,
                    obj.getExpiry().isAfter(Instant.now()),
                    obj.getCreatedAt(),
                    obj.getUpdatedAt(),
                    obj.getExpiry()
            );
        }).toList();

        return new SliceImpl<>(responses.subList(0,Math.min(responses.size(),size)), PageRequest.of(page,size),responses.size() > size);

    }

    @Transactional
    public SharedObjectRes viewSharedObject(
            String ownerEmail,
            UUID objectId,
            UUID accountId
    ) {
        if (shareJpa.hasObjectShared(accountId, objectId)) {

            Account owner = accountJpa.findByEmail(ownerEmail).orElseThrow(() ->
                    new ResourceNotFoundException("account not found")
            );

            File sharedFile = fileJpa.findById(objectId, owner.getAccountId()).orElseThrow(() ->
                    new ResourceNotFoundException("file not found")
            );

            URL url = s3DependentService.getPreSignedUrl(usersBucketName, sharedFile.getS3Key(), 15);

            return new SharedObjectRes(
                    sharedFile.getFileId(),
                    sharedFile.getName(),
                    url.toString(),
                    Instant.now().plus(15, ChronoUnit.MINUTES)
            );

        } else throw new ResourceNotFoundException("can't find shared record");
    }


    @Transactional
    public void deleteSharedObject(
            String recipientEmail,
            UUID objectId,
            UUID accountId
    ) {
        Account recipient = accountJpa.findByEmail(recipientEmail).orElseThrow(() ->
                new ResourceNotFoundException("account not found")
        );

        if(shareJpa.findSharedObjectByRecipientBy(accountId,recipient.getAccountId(),objectId) == null) {
            throw new ResourceNotFoundException("couldn't find shared record");
        }

        shareJpa.deleteSharedObject(accountId,recipient.getAccountId(),objectId);
    }

}
