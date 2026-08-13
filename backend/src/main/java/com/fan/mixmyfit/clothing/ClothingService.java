package com.fan.mixmyfit.clothing;

import com.fan.mixmyfit.domain.Category;
import com.fan.mixmyfit.domain.Clothing;
import com.fan.mixmyfit.domain.ClothingSeason;
import com.fan.mixmyfit.domain.ClothingStatus;
import com.fan.mixmyfit.domain.ClothingTag;
import com.fan.mixmyfit.domain.ClothingTagLink;
import com.fan.mixmyfit.domain.Season;
import com.fan.mixmyfit.domain.User;
import com.fan.mixmyfit.domain.repository.CategoryRepository;
import com.fan.mixmyfit.domain.repository.ClothingRepository;
import com.fan.mixmyfit.domain.repository.ClothingSeasonRepository;
import com.fan.mixmyfit.domain.repository.ClothingTagLinkRepository;
import com.fan.mixmyfit.domain.repository.ClothingTagRepository;
import com.fan.mixmyfit.file.ClothingImage;
import com.fan.mixmyfit.file.StoredFile;
import com.fan.mixmyfit.file.StoredFileService;
import com.fan.mixmyfit.security.AccessDeniedException;
import com.fan.mixmyfit.security.CurrentUserResolver;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ClothingService {
    private final CurrentUserResolver currentUsers;
    private final ObjectProvider<ClothingRepository> clothingRepositories;
    private final StoredFileService storedFiles;
    private final ObjectProvider<CategoryRepository> categoryRepositories;
    private final ObjectProvider<ClothingSeasonRepository> clothingSeasonRepositories;
    private final ObjectProvider<ClothingTagRepository> clothingTagRepositories;
    private final ObjectProvider<ClothingTagLinkRepository> clothingTagLinkRepositories;

    public ClothingService(
            CurrentUserResolver currentUsers,
            ObjectProvider<ClothingRepository> clothingRepositories,
            StoredFileService storedFiles,
            ObjectProvider<CategoryRepository> categoryRepositories,
            ObjectProvider<ClothingSeasonRepository> clothingSeasonRepositories,
            ObjectProvider<ClothingTagRepository> clothingTagRepositories,
            ObjectProvider<ClothingTagLinkRepository> clothingTagLinkRepositories) {
        this.currentUsers = currentUsers;
        this.clothingRepositories = clothingRepositories;
        this.storedFiles = storedFiles;
        this.categoryRepositories = categoryRepositories;
        this.clothingSeasonRepositories = clothingSeasonRepositories;
        this.clothingTagRepositories = clothingTagRepositories;
        this.clothingTagLinkRepositories = clothingTagLinkRepositories;
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
    public ClothingListResponse list(
            String sessionId,
            int page,
            int size,
            Long categoryId,
            String status,
            String color,
            String season,
            List<Long> tagIds) {
        Long userId = currentUsers.requireUserId(sessionId);
        List<Clothing> filtered = clothes().findByUserUserIdOrderByClothingId(userId).stream()
                .filter(clothing -> matchesCategory(clothing, categoryId))
                .filter(clothing -> matchesStatus(clothing, status))
                .filter(clothing -> matchesColor(clothing, color))
                .filter(clothing -> matchesSeason(clothing, season))
                .filter(clothing -> matchesTags(clothing, tagIds))
                .sorted(Comparator.comparing(
                                Clothing::getCreatedAt,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Clothing::getClothingId, Comparator.reverseOrder()))
                .toList();

        int safePage = Math.max(page, 0);
        int safeSize = size > 0 ? size : 20;
        int fromIndex = Math.min(safePage * safeSize, filtered.size());
        int toIndex = Math.min(fromIndex + safeSize, filtered.size());
        List<ClothingResponse> items = filtered.subList(fromIndex, toIndex).stream()
                .map(this::response)
                .toList();
        return new ClothingListResponse(items, safePage, safeSize, filtered.size());
    }

    @Transactional(readOnly = true)
    public List<String> colors(String sessionId) {
        Long userId = currentUsers.requireUserId(sessionId);
        return clothes().findByUserUserIdOrderByClothingId(userId).stream()
                .map(Clothing::getColor)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .sorted()
                .toList();
    }

    @Transactional(readOnly = true)
    public DraftCountResponse draftCount(String sessionId) {
        Long userId = currentUsers.requireUserId(sessionId);
        long count = clothes().findByUserUserIdOrderByClothingId(userId).stream()
                .filter(clothing -> clothing.getStatus() == ClothingStatus.DRAFT)
                .count();
        return new DraftCountResponse(count);
    }

    @Transactional(readOnly = true)
    public ClothingResponse get(String sessionId, Long clothingId) {
        return response(requireOwnedClothing(sessionId, clothingId));
    }

    @Transactional
    public ClothingResponse update(String sessionId, Long clothingId, ClothingUpdateRequest request) {
        Long userId = currentUsers.requireUserId(sessionId);
        Clothing clothing = requireOwnedClothing(userId, clothingId);
        Category category = request != null && request.hasCategoryId()
                ? resolveCategory(userId, request.categoryId())
                : clothing.getCategory();
        String name = request != null && request.hasName() ? request.name() : clothing.getName();
        String color = request != null && request.hasColor() ? request.color() : clothing.getColor();
        clothing.updateMetadata(category, name, color);

        if (request != null && request.seasons() != null) {
            replaceSeasons(clothing, request.seasons());
        }
        if (request != null && request.tagIds() != null) {
            replaceTags(userId, clothing, request.tagIds());
        }
        return response(clothing);
    }

    @Transactional
    public ClothingBatchResponse batch(String sessionId, ClothingBatchRequest request) {
        Long userId = currentUsers.requireUserId(sessionId);
        List<Long> clothingIds = request == null || request.clothingIds() == null
                ? List.of()
                : request.clothingIds().stream().distinct().toList();
        List<Clothing> selected = clothingIds.stream()
                .map(clothingId -> requireOwnedClothing(userId, clothingId))
                .toList();
        Category category = request != null && request.hasCategoryId()
                ? resolveCategory(userId, request.categoryId())
                : null;
        List<ClothingTag> tagsToAdd = resolveTags(userId, request == null ? null : request.addTagIds());
        List<Long> tagIdsToRemove = resolveTags(userId, request == null ? null : request.removeTagIds()).stream()
                .map(ClothingTag::getClothingTagId)
                .toList();

        for (Clothing clothing : selected) {
            Category nextCategory = request != null && request.hasCategoryId() ? category : clothing.getCategory();
            String nextColor = request != null && request.hasColor() ? request.color() : clothing.getColor();
            clothing.updateMetadata(nextCategory, clothing.getName(), nextColor);
            if (request != null && request.seasons() != null) {
                replaceSeasons(clothing, request.seasons());
            }
            addTags(clothing, tagsToAdd);
            removeTags(clothing, tagIdsToRemove);
        }
        return new ClothingBatchResponse(selected.size());
    }

    @Transactional
    public void delete(String sessionId, Long clothingId) {
        Clothing clothing = requireOwnedClothing(sessionId, clothingId);
        clothes().delete(clothing);
    }

    @Transactional(readOnly = true)
    public ClothingImage image(String sessionId, Long clothingId) {
        Clothing clothing = requireOwnedClothing(sessionId, clothingId);
        return new ClothingImage(storedFiles.read(clothing.getImagePath()), clothing.getContentType());
    }

    private ClothingResponse response(Clothing clothing) {
        List<ClothingSeason> seasons = clothingSeasons()
                .findByClothingClothingIdOrderByClothingSeasonId(clothing.getClothingId());
        List<ClothingTag> tags = clothingTagLinks()
                .findByClothingClothingIdOrderByClothingTagLinkId(clothing.getClothingId())
                .stream()
                .map(ClothingTagLink::getClothingTag)
                .toList();
        return ClothingResponse.from(clothing, seasons, tags);
    }

    private boolean matchesCategory(Clothing clothing, Long categoryId) {
        return categoryId == null
                || (clothing.getCategory() != null && categoryId.equals(clothing.getCategory().getCategoryId()));
    }

    private boolean matchesStatus(Clothing clothing, String status) {
        if (status == null || status.isBlank()) {
            return true;
        }
        return clothing.getStatus().dbValue().equals(status);
    }

    private boolean matchesColor(Clothing clothing, String color) {
        return color == null || color.isBlank() || color.equals(clothing.getColor());
    }

    private boolean matchesSeason(Clothing clothing, String season) {
        if (season == null || season.isBlank()) {
            return true;
        }
        Season requested = Season.fromApiValue(season);
        return clothingSeasons()
                .findByClothingClothingIdOrderByClothingSeasonId(clothing.getClothingId())
                .stream()
                .anyMatch(clothingSeason -> clothingSeason.getSeason() == requested);
    }

    private boolean matchesTags(Clothing clothing, List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return true;
        }
        List<Long> requestedTagIds = tagIds.stream().distinct().toList();
        List<Long> clothingTagIds = clothingTagLinks()
                .findByClothingClothingIdOrderByClothingTagLinkId(clothing.getClothingId())
                .stream()
                .map(link -> link.getClothingTag().getClothingTagId())
                .toList();
        return clothingTagIds.containsAll(requestedTagIds);
    }

    private Clothing requireOwnedClothing(String sessionId, Long clothingId) {
        return requireOwnedClothing(currentUsers.requireUserId(sessionId), clothingId);
    }

    private Clothing requireOwnedClothing(Long userId, Long clothingId) {
        Clothing clothing = clothes().findById(clothingId)
                .orElseThrow(ClothingService::notFound);
        if (!clothing.getUser().getUserId().equals(userId)) {
            throw notFound();
        }
        return clothing;
    }

    private Category resolveCategory(Long userId, Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        Category category = categories().findById(categoryId).orElseThrow(ClothingService::notFound);
        User owner = category.getUser();
        if (owner != null && !owner.getUserId().equals(userId)) {
            throw notFound();
        }
        return category;
    }

    private void replaceSeasons(Clothing clothing, List<String> requestedSeasons) {
        clothingSeasons().deleteByClothingClothingId(clothing.getClothingId());
        List<ClothingSeason> replacements = requestedSeasons.stream()
                .map(Season::fromApiValue)
                .distinct()
                .map(season -> new ClothingSeason(clothing, season))
                .toList();
        clothingSeasons().saveAll(replacements);
    }

    private void replaceTags(Long userId, Clothing clothing, List<Long> tagIds) {
        List<ClothingTag> resolvedTags = resolveTags(userId, tagIds);
        clothingTagLinks().deleteByClothingClothingId(clothing.getClothingId());
        clothingTagLinks().saveAll(resolvedTags.stream()
                .map(tag -> new ClothingTagLink(clothing, tag))
                .toList());
    }

    private List<ClothingTag> resolveTags(Long userId, List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return List.of();
        }
        List<ClothingTag> resolvedTags = new ArrayList<>();
        for (Long tagId : tagIds.stream().distinct().toList()) {
            ClothingTag tag = clothingTags().findById(tagId).orElseThrow(ClothingService::notFound);
            if (!tag.getUser().getUserId().equals(userId)) {
                throw notFound();
            }
            resolvedTags.add(tag);
        }
        return resolvedTags;
    }

    private void addTags(Clothing clothing, List<ClothingTag> tagsToAdd) {
        if (tagsToAdd.isEmpty()) {
            return;
        }
        List<Long> existingTagIds = clothingTagLinks()
                .findByClothingClothingIdOrderByClothingTagLinkId(clothing.getClothingId())
                .stream()
                .map(link -> link.getClothingTag().getClothingTagId())
                .toList();
        clothingTagLinks().saveAll(tagsToAdd.stream()
                .filter(tag -> !existingTagIds.contains(tag.getClothingTagId()))
                .map(tag -> new ClothingTagLink(clothing, tag))
                .toList());
    }

    private void removeTags(Clothing clothing, List<Long> tagIdsToRemove) {
        if (tagIdsToRemove.isEmpty()) {
            return;
        }
        List<ClothingTag> remainingTags = clothingTagLinks()
                .findByClothingClothingIdOrderByClothingTagLinkId(clothing.getClothingId())
                .stream()
                .map(ClothingTagLink::getClothingTag)
                .filter(tag -> !tagIdsToRemove.contains(tag.getClothingTagId()))
                .toList();
        clothingTagLinks().deleteByClothingClothingId(clothing.getClothingId());
        clothingTagLinks().saveAll(remainingTags.stream()
                .map(tag -> new ClothingTagLink(clothing, tag))
                .toList());
    }

    private static AccessDeniedException notFound() {
        return new AccessDeniedException("RESOURCE_NOT_FOUND", "Resource not found");
    }

    private ClothingRepository clothes() {
        return clothingRepositories.getObject();
    }

    private CategoryRepository categories() {
        return categoryRepositories.getObject();
    }

    private ClothingSeasonRepository clothingSeasons() {
        return clothingSeasonRepositories.getObject();
    }

    private ClothingTagRepository clothingTags() {
        return clothingTagRepositories.getObject();
    }

    private ClothingTagLinkRepository clothingTagLinks() {
        return clothingTagLinkRepositories.getObject();
    }
}
