package com.fan.mixmyfit.clothing;

import com.fan.mixmyfit.domain.Clothing;
import com.fan.mixmyfit.domain.User;
import com.fan.mixmyfit.domain.repository.ClothingRepository;
import com.fan.mixmyfit.file.ClothingImage;
import com.fan.mixmyfit.file.StoredFile;
import com.fan.mixmyfit.file.StoredFileService;
import com.fan.mixmyfit.security.AccessDeniedException;
import com.fan.mixmyfit.security.CurrentUserResolver;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ClothingService {
    private final CurrentUserResolver currentUsers;
    private final ObjectProvider<ClothingRepository> clothingRepositories;
    private final StoredFileService storedFiles;

    public ClothingService(
            CurrentUserResolver currentUsers,
            ObjectProvider<ClothingRepository> clothingRepositories,
            StoredFileService storedFiles) {
        this.currentUsers = currentUsers;
        this.clothingRepositories = clothingRepositories;
        this.storedFiles = storedFiles;
    }

    @Transactional
    public ClothingUploadResponse upload(String sessionId, MultipartFile file) {
        User user = currentUsers.requireUser(sessionId);
        StoredFile stored = storedFiles.store(file);
        Clothing clothing = clothes().save(Clothing.draft(
                user,
                stored.path(),
                stored.originalFilename(),
                stored.contentType(),
                stored.fileSize()));
        return ClothingUploadResponse.from(clothing);
    }

    @Transactional(readOnly = true)
    public ClothingImage image(String sessionId, Long clothingId) {
        Long userId = currentUsers.requireUserId(sessionId);
        Clothing clothing = clothes().findById(clothingId)
                .orElseThrow(() -> new AccessDeniedException("RESOURCE_NOT_FOUND", "Resource not found"));
        if (!clothing.getUser().getUserId().equals(userId)) {
            throw new AccessDeniedException("RESOURCE_NOT_FOUND", "Resource not found");
        }
        return new ClothingImage(storedFiles.read(clothing.getImagePath()), clothing.getContentType());
    }

    private ClothingRepository clothes() {
        return clothingRepositories.getObject();
    }
}
