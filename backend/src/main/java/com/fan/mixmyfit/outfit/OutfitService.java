package com.fan.mixmyfit.outfit;

import com.fan.mixmyfit.domain.Clothing;
import com.fan.mixmyfit.domain.ClothingStatus;
import com.fan.mixmyfit.domain.Outfit;
import com.fan.mixmyfit.domain.OutfitItem;
import com.fan.mixmyfit.domain.OutfitItemSize;
import com.fan.mixmyfit.domain.OutfitSeason;
import com.fan.mixmyfit.domain.OutfitSlot;
import com.fan.mixmyfit.domain.OutfitTag;
import com.fan.mixmyfit.domain.OutfitTagLink;
import com.fan.mixmyfit.domain.Season;
import com.fan.mixmyfit.domain.User;
import com.fan.mixmyfit.domain.repository.ClothingRepository;
import com.fan.mixmyfit.domain.repository.OutfitItemRepository;
import com.fan.mixmyfit.domain.repository.OutfitRepository;
import com.fan.mixmyfit.domain.repository.OutfitSeasonRepository;
import com.fan.mixmyfit.domain.repository.OutfitTagLinkRepository;
import com.fan.mixmyfit.domain.repository.OutfitTagRepository;
import com.fan.mixmyfit.security.AccessDeniedException;
import com.fan.mixmyfit.security.CurrentUserResolver;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutfitService {
    private static final String DEFAULT_TITLE = "Untitled Outfit";

    private final CurrentUserResolver currentUsers;
    private final ObjectProvider<OutfitRepository> outfitRepositories;
    private final ObjectProvider<ClothingRepository> clothingRepositories;
    private final ObjectProvider<OutfitItemRepository> outfitItemRepositories;
    private final ObjectProvider<OutfitSeasonRepository> outfitSeasonRepositories;
    private final ObjectProvider<OutfitTagRepository> outfitTagRepositories;
    private final ObjectProvider<OutfitTagLinkRepository> outfitTagLinkRepositories;

    public OutfitService(
            CurrentUserResolver currentUsers,
            ObjectProvider<OutfitRepository> outfitRepositories,
            ObjectProvider<ClothingRepository> clothingRepositories,
            ObjectProvider<OutfitItemRepository> outfitItemRepositories,
            ObjectProvider<OutfitSeasonRepository> outfitSeasonRepositories,
            ObjectProvider<OutfitTagRepository> outfitTagRepositories,
            ObjectProvider<OutfitTagLinkRepository> outfitTagLinkRepositories) {
        this.currentUsers = currentUsers;
        this.outfitRepositories = outfitRepositories;
        this.clothingRepositories = clothingRepositories;
        this.outfitItemRepositories = outfitItemRepositories;
        this.outfitSeasonRepositories = outfitSeasonRepositories;
        this.outfitTagRepositories = outfitTagRepositories;
        this.outfitTagLinkRepositories = outfitTagLinkRepositories;
    }

    @Transactional
    public OutfitCreateResponse create(String sessionId, OutfitCreateRequest request) {
        User user = currentUsers.requireUser(sessionId);
        List<OutfitItemRequest> requestedItems = request == null || request.items() == null
                ? List.of()
                : request.items();
        if (requestedItems.isEmpty()) {
            throw invalid("Outfit must include at least one item");
        }

        List<Clothing> clothing = requestedItems.stream()
                .map(item -> requireReadyOwnedClothing(user.getUserId(), item.clothingId()))
                .toList();
        List<OutfitTag> tags = resolveTags(user.getUserId(), request.tagIds());

        Outfit outfit = outfits().save(new Outfit(user, titleOrDefault(request.title()), request.note()));
        outfitSeasons().saveAll(resolveSeasons(request.seasons()).stream()
                .map(season -> new OutfitSeason(outfit, season))
                .toList());
        outfitTagLinks().saveAll(tags.stream()
                .map(tag -> new OutfitTagLink(outfit, tag))
                .toList());
        outfitItems().saveAll(createItems(outfit, user, requestedItems, clothing));

        return new OutfitCreateResponse(outfit.getOutfitId(), outfit.getTitle());
    }

    private List<OutfitItem> createItems(
            Outfit outfit,
            User user,
            List<OutfitItemRequest> requestedItems,
            List<Clothing> clothing) {
        return java.util.stream.IntStream.range(0, requestedItems.size())
                .mapToObj(index -> createItem(outfit, user, requestedItems.get(index), clothing.get(index)))
                .toList();
    }

    private OutfitItem createItem(Outfit outfit, User user, OutfitItemRequest request, Clothing clothing) {
        if ("main_slot".equals(request.role())) {
            if (request.slot() == null || request.slot().isBlank()) {
                throw invalid("Main slot item must include slot");
            }
            return OutfitItem.mainSlot(outfit, user, clothing.getClothingId(), slot(request.slot()));
        }
        if ("accessory_overlay".equals(request.role())) {
            return OutfitItem.accessory(
                    outfit,
                    user,
                    clothing.getClothingId(),
                    requiredInt(request.positionX(), "Accessory item must include positionX"),
                    requiredInt(request.positionY(), "Accessory item must include positionY"),
                    size(request.size()),
                    requiredInt(request.zIndex(), "Accessory item must include zIndex"));
        }
        throw invalid("Unsupported outfit item role");
    }

    private Clothing requireReadyOwnedClothing(Long userId, Long clothingId) {
        if (clothingId == null) {
            throw invalid("Outfit item must include clothingId");
        }
        Clothing clothing = clothes().findById(clothingId).orElseThrow(OutfitService::notFound);
        if (!clothing.getUser().getUserId().equals(userId)) {
            throw notFound();
        }
        if (clothing.getStatus() != ClothingStatus.READY) {
            throw invalid("Draft clothing cannot be saved into an outfit");
        }
        return clothing;
    }

    private List<OutfitTag> resolveTags(Long userId, List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return List.of();
        }
        return tagIds.stream()
                .distinct()
                .map(tagId -> {
                    OutfitTag tag = outfitTags().findById(tagId).orElseThrow(OutfitService::notFound);
                    if (!tag.getUser().getUserId().equals(userId)) {
                        throw notFound();
                    }
                    return tag;
                })
                .toList();
    }

    private List<Season> resolveSeasons(List<String> requestedSeasons) {
        if (requestedSeasons == null || requestedSeasons.isEmpty()) {
            return List.of();
        }
        return requestedSeasons.stream()
                .map(Season::fromApiValue)
                .distinct()
                .toList();
    }

    private static String titleOrDefault(String title) {
        return title == null || title.isBlank() ? DEFAULT_TITLE : title;
    }

    private static OutfitSlot slot(String value) {
        return switch (normalized(value)) {
            case "top" -> OutfitSlot.TOP;
            case "bottom" -> OutfitSlot.BOTTOM;
            case "shoes" -> OutfitSlot.SHOES;
            case "hat" -> OutfitSlot.HAT;
            default -> throw invalid("Unsupported outfit slot");
        };
    }

    private static OutfitItemSize size(String value) {
        if (value == null || value.isBlank()) {
            throw invalid("Accessory item must include size");
        }
        return switch (normalized(value)) {
            case "small" -> OutfitItemSize.SMALL;
            case "medium" -> OutfitItemSize.MEDIUM;
            case "large" -> OutfitItemSize.LARGE;
            default -> throw invalid("Unsupported accessory size");
        };
    }

    private static String normalized(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private static int requiredInt(Integer value, String message) {
        if (value == null) {
            throw invalid(message);
        }
        return value;
    }

    private static OutfitException invalid(String message) {
        return new OutfitException("INVALID_OUTFIT", message);
    }

    private static AccessDeniedException notFound() {
        return new AccessDeniedException("RESOURCE_NOT_FOUND", "Resource not found");
    }

    private OutfitRepository outfits() {
        return outfitRepositories.getObject();
    }

    private ClothingRepository clothes() {
        return clothingRepositories.getObject();
    }

    private OutfitItemRepository outfitItems() {
        return outfitItemRepositories.getObject();
    }

    private OutfitSeasonRepository outfitSeasons() {
        return outfitSeasonRepositories.getObject();
    }

    private OutfitTagRepository outfitTags() {
        return outfitTagRepositories.getObject();
    }

    private OutfitTagLinkRepository outfitTagLinks() {
        return outfitTagLinkRepositories.getObject();
    }
}
