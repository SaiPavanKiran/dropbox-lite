package org.rspk.dropbox_lite.service;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public interface FolderDependentService {
    UUID createOrFindFolderUUID(String name, UUID accountId);
}
