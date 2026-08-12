package com.fan.mixmyfit.tag;

import com.fan.mixmyfit.domain.ClothingTag;
import com.fan.mixmyfit.domain.OutfitTag;
import com.fan.mixmyfit.domain.User;
import com.fan.mixmyfit.domain.repository.ClothingTagRepository;
import com.fan.mixmyfit.domain.repository.OutfitTagRepository;
import com.fan.mixmyfit.security.CurrentUserResolver;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class TagService {
    private final ObjectProvider<ClothingTagRepository> clothingTagRepositories;
    private final ObjectProvider<OutfitTagRepository> outfitTagRepositories;
    private final CurrentUserResolver currentUsers;

    TagService(
            ObjectProvider<ClothingTagRepository> clothingTagRepositories,
            ObjectProvider<OutfitTagRepository> outfitTagRepositories,
            CurrentUserResolver currentUsers) {
        this.clothingTagRepositories = clothingTagRepositories;
        this.outfitTagRepositories = outfitTagRepositories;
        this.currentUsers = currentUsers;
    }

    @Transactional(readOnly = true)
    List<TagResponse> listClothingTags(String sessionId) {
        Long userId = currentUsers.requireUserId(sessionId);
        return clothingTags().findByUserUserIdOrderByClothingTagId(userId).stream()
                .map(TagResponse::fromClothing)
                .toList();
    }

    @Transactional
    TagResponse createClothingTag(String sessionId, TagRequest request) {
        User user = currentUsers.requireUser(sessionId);
        String name = requiredName(request);
        if (clothingTags().existsByUserUserIdAndName(user.getUserId(), name)) {
            throw new TagException("TAG_NAME_EXISTS", "Tag name already exists");
        }
        try {
            return TagResponse.fromClothing(clothingTags().saveAndFlush(new ClothingTag(user, name)));
        } catch (DataIntegrityViolationException exception) {
            throw new TagException("TAG_NAME_EXISTS", "Tag name already exists");
        }
    }

    @Transactional(readOnly = true)
    List<TagResponse> listOutfitTags(String sessionId) {
        Long userId = currentUsers.requireUserId(sessionId);
        return outfitTags().findByUserUserIdOrderByOutfitTagId(userId).stream()
                .map(TagResponse::fromOutfit)
                .toList();
    }

    @Transactional
    TagResponse createOutfitTag(String sessionId, TagRequest request) {
        User user = currentUsers.requireUser(sessionId);
        String name = requiredName(request);
        if (outfitTags().existsByUserUserIdAndName(user.getUserId(), name)) {
            throw new TagException("TAG_NAME_EXISTS", "Tag name already exists");
        }
        try {
            return TagResponse.fromOutfit(outfitTags().saveAndFlush(new OutfitTag(user, name)));
        } catch (DataIntegrityViolationException exception) {
            throw new TagException("TAG_NAME_EXISTS", "Tag name already exists");
        }
    }

    private ClothingTagRepository clothingTags() {
        return clothingTagRepositories.getObject();
    }

    private OutfitTagRepository outfitTags() {
        return outfitTagRepositories.getObject();
    }

    private static String requiredName(TagRequest request) {
        if (request == null || request.name() == null || request.name().isBlank()) {
            throw new TagException("TAG_NAME_REQUIRED", "Tag name is required");
        }
        return request.name().trim();
    }
}
