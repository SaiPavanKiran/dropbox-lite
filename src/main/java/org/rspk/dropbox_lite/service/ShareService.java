package org.rspk.dropbox_lite.service;

import jakarta.transaction.Transactional;
import org.rspk.dropbox_lite.model.account.Account;
import org.rspk.dropbox_lite.model.files.File;
import org.rspk.dropbox_lite.model.share.ShareObject;
import org.rspk.dropbox_lite.model.share.ShareObjectReq;
import org.rspk.dropbox_lite.model.share.SharedObjectRes;
import org.rspk.dropbox_lite.model.share.SharedObjectsRes;
import org.rspk.dropbox_lite.repository.AccountJpa;
import org.rspk.dropbox_lite.repository.FileJpa;
import org.rspk.dropbox_lite.repository.ShareJpa;
import org.rspk.dropbox_lite.utils.exceptions.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.time.Duration;
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

    @Value("${aws.s3.users_bucket}")
    private String usersBucketName;

    public ShareService(
            ShareJpa shareJpa,
            FileJpa fileJpa,
            AccountJpa accountJpa,
            FolderDependentService folderDependentService
    ){
        this.accountJpa = accountJpa;
        this.shareJpa = shareJpa;
        this.fileJpa = fileJpa;
        this.folderDependentService = folderDependentService;
    }

    @Transactional
    public void shareObject(
            UUID accountId,
            ShareObjectReq shareObjectReq
    ) {
        Account recipient = accountJpa.findByEmail(shareObjectReq.recipientEmail())
                .orElseThrow(() -> new ResourceNotFoundException("recipient not found"));
        folderDependentService.createOrFindFolderUUID(".share",recipient.getAccountId());
        /*we only use files for now*/
        File file = fileJpa.findById(shareObjectReq.objectId(), accountId).orElseThrow(() -> new ResourceNotFoundException("object not found"));
        ShareObject shareObject = shareJpa.findSharedObjectByRecipientBy(accountId, recipient.getAccountId(), shareObjectReq.objectId());
        Instant expiry = Instant.now().plus(shareObjectReq.shareDurationInMin(), ChronoUnit.MINUTES);
        if (shareObject == null) {
            ShareObject newShared = new ShareObject(
                    accountId,
                    shareObjectReq.objectId(),
                    file.getName(),
                    recipient.getAccountId(),
                    expiry
            );
            shareJpa.save(newShared);
            return;
        }
        shareObject.setExpiry(expiry);
        shareJpa.save(shareObject);
    }

    @Transactional
    public List<SharedObjectsRes> getSharedFiles(
            UUID accountId,
            int page,
            int size
    ) {
        List<ShareObject> shareObjects = shareJpa.findByRecipientAccountId(accountId, page, size);

        List<UUID> ownerIds = shareObjects.stream()
                .map(shareObject -> shareObject.getId().getOwnerId())
                .toList();

        Map<UUID, String> accountMap = accountJpa.findByIds(ownerIds).stream()
                .collect(Collectors.toMap(Account::getAccountId, Account::getEmail));

        return shareObjects.stream().map(shareObject -> {
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
    }

    public SharedObjectRes viewSharedObject(
            String email,
            UUID objectId,
            UUID accountId
    ) {
        if(shareJpa.hasObjectShared(accountId,objectId)) {

            Account owner = accountJpa.findByEmail(email).orElseThrow(() ->
                    new ResourceNotFoundException("account not found")
            );

            File sharedFile = fileJpa.findById(objectId,owner.getAccountId()).orElseThrow(() ->
                    new ResourceNotFoundException("file not found")
            );

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(usersBucketName)
                    .key(sharedFile.getS3Key())
                    .build();

            GetObjectPresignRequest preSignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(15))
                    .getObjectRequest(getObjectRequest)
                    .build();

            try(S3Presigner preSigner = S3Presigner.builder().build()) {
               PresignedGetObjectRequest preSigned = preSigner.presignGetObject(preSignRequest);


               return new SharedObjectRes(
                       sharedFile.getFileId(),
                       sharedFile.getName(),
                       preSigned.url().toString(),
                       Instant.now().plus(15,ChronoUnit.MINUTES)
               );
            }

        } else throw new ResourceNotFoundException("can't find shared record");
    }

}
