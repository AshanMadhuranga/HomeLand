package com.landhub.land;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@Transactional
public class LandService {

    private static final long MAX_IMAGE_SIZE = 5L * 1024L * 1024L;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final LandRepository landRepository;
    private final Path uploadDirectory = Paths.get("uploads", "lands").toAbsolutePath().normalize();

    public LandService(LandRepository landRepository) {
        this.landRepository = landRepository;
    }

    @Transactional(readOnly = true)
    public List<Land> findPublicLands(String location,
                                      String type,
                                      String availability,
                                      BigDecimal minPrice,
                                      BigDecimal maxPrice,
                                      BigDecimal minSize,
                                      BigDecimal maxSize,
                                      String sortBy) {
        Stream<Land> lands = landRepository.findByStatusNotOrderByCreatedAtDesc(LandStatus.INACTIVE).stream();

        if (hasText(location)) {
            String locationSearch = location.trim().toLowerCase(Locale.ROOT);
            lands = lands.filter(land -> safeLower(land.getLocation()).contains(locationSearch)
                    || safeLower(land.getDistrict()).contains(locationSearch)
                    || safeLower(land.getAddress()).contains(locationSearch));
        }

        LandType landType = parseLandType(type);
        if (landType != null) {
            lands = lands.filter(land -> land.getLandType() == landType);
        }

        LandStatus landStatus = parseLandStatus(availability);
        if (landStatus != null && landStatus != LandStatus.INACTIVE) {
            lands = lands.filter(land -> land.getStatus() == landStatus);
        }

        if (minPrice != null) {
            lands = lands.filter(land -> land.getPrice() != null && land.getPrice().compareTo(minPrice) >= 0);
        }

        if (maxPrice != null) {
            lands = lands.filter(land -> land.getPrice() != null && land.getPrice().compareTo(maxPrice) <= 0);
        }

        if (minSize != null) {
            lands = lands.filter(land -> land.getSize() != null && land.getSize().compareTo(minSize) >= 0);
        }

        if (maxSize != null) {
            lands = lands.filter(land -> land.getSize() != null && land.getSize().compareTo(maxSize) <= 0);
        }

        return lands.sorted(resolvePublicSort(sortBy)).toList();
    }

    @Transactional(readOnly = true)
    public Optional<Land> findPublicLandById(Long id) {
        return landRepository.findById(id)
                .filter(land -> land.getStatus() != LandStatus.INACTIVE);
    }

    @Transactional(readOnly = true)
    public List<Land> findAllForAdmin() {
        return landRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Optional<Land> findById(Long id) {
        return landRepository.findById(id);
    }

    public Land create(Land land, MultipartFile[] images) {
        if (land.getStatus() == null) {
            land.setStatus(LandStatus.PENDING);
        }

        Land saved = landRepository.save(land);
        storeImages(saved, images);
        return saved;
    }

    public Land update(Long id, Land formLand, MultipartFile[] images) {
        Land existing = landRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Land listing was not found."));

        existing.setTitle(formLand.getTitle());
        existing.setDescription(formLand.getDescription());
        existing.setLocation(formLand.getLocation());
        existing.setAddress(formLand.getAddress());
        existing.setDistrict(formLand.getDistrict());
        existing.setPrice(formLand.getPrice());
        existing.setSize(formLand.getSize());
        existing.setSizeUnit(formLand.getSizeUnit());
        existing.setLandType(formLand.getLandType());
        existing.setStatus(formLand.getStatus() == null ? LandStatus.PENDING : formLand.getStatus());
        existing.setRoadAccess(formLand.isRoadAccess());
        existing.setWaterAvailable(formLand.isWaterAvailable());
        existing.setElectricityAvailable(formLand.isElectricityAvailable());

        Land saved = landRepository.save(existing);
        storeImages(saved, images);
        return saved;
    }

    public void deactivate(Long id) {
        Land land = landRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Land listing was not found."));
        land.setStatus(LandStatus.INACTIVE);
        landRepository.save(land);
    }

    private void storeImages(Land land, MultipartFile[] files) {
        if (files == null || files.length == 0) {
            return;
        }

        int nextOrder = land.getImages().size() + 1;
        boolean hasCoverImage = land.getImages().stream().anyMatch(LandImage::isCoverImage);

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }

            validateImage(file);
            String extension = getExtension(file.getOriginalFilename());
            String fileName = UUID.randomUUID() + "." + extension;
            Path target = uploadDirectory.resolve(fileName).normalize();

            if (!target.startsWith(uploadDirectory)) {
                throw new IllegalArgumentException("Invalid image path.");
            }

            try {
                Files.createDirectories(uploadDirectory);
                Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException exception) {
                throw new IllegalStateException("Unable to save uploaded image.", exception);
            }

            LandImage image = new LandImage();
            image.setImageUrl("/uploads/lands/" + fileName);
            image.setDisplayOrder(nextOrder++);
            image.setCoverImage(!hasCoverImage);
            hasCoverImage = true;
            land.addImage(image);
        }

        landRepository.save(land);
    }

    private void validateImage(MultipartFile file) {
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException("Each image must be 5MB or smaller.");
        }

        String extension = getExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Only JPG, JPEG, PNG, and WEBP images are allowed.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Uploaded images must be valid JPG, PNG, or WEBP files.");
        }
    }

    private Comparator<Land> resolvePublicSort(String sortBy) {
        if ("price-low".equals(sortBy)) {
            return Comparator.comparing(Land::getPrice, Comparator.nullsLast(BigDecimal::compareTo));
        }

        if ("price-high".equals(sortBy)) {
            return Comparator.comparing(Land::getPrice, Comparator.nullsLast(BigDecimal::compareTo)).reversed();
        }

        if ("largest".equals(sortBy)) {
            return Comparator.comparing(Land::getSize, Comparator.nullsLast(BigDecimal::compareTo)).reversed();
        }

        return Comparator.comparing(Land::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed();
    }

    private LandType parseLandType(String value) {
        if (!hasText(value)) {
            return null;
        }

        try {
            return LandType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private LandStatus parseLandStatus(String value) {
        if (!hasText(value)) {
            return null;
        }

        try {
            return LandStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private String getExtension(String filename) {
        if (!hasText(filename) || !filename.contains(".")) {
            return "";
        }

        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
