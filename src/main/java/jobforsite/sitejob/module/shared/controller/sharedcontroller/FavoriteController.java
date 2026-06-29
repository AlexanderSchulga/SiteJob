package jobforsite.sitejob.module.shared.controller.sharedcontroller;

import jobforsite.sitejob.module.product.model.PostText;
import jobforsite.sitejob.module.product.repository.PostRepo;
import jobforsite.sitejob.module.resume.model.Resume;
import jobforsite.sitejob.module.resume.repository.PostResumeRepository;
import jobforsite.sitejob.module.service.model.PostTextJob;
import jobforsite.sitejob.module.service.repository.PostRepoJob;
import jobforsite.sitejob.module.shared.config.globalconfig.RegionsConfig;
import jobforsite.sitejob.module.shared.controller.sharedglobal.BaseController;
import jobforsite.sitejob.module.shared.dto.FavoriteDTO;
import jobforsite.sitejob.module.shared.dto.SliceFavorResponseDTO;
import jobforsite.sitejob.module.shared.model.Favorites;
import jobforsite.sitejob.module.shared.repository.FavorRepo;
import jobforsite.sitejob.module.users.model.User;
import jobforsite.sitejob.module.vacancy.model.Vacancy;
import jobforsite.sitejob.module.vacancy.repository.PostVacancyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Controller
@RequestMapping("page_one/favorites")
public class FavoriteController extends BaseController {

    private final FavorRepo favorRepo;
    private final PostRepo postRepo;
    private final PostRepoJob postRepoJob;
    private final PostVacancyRepository postVacancyRepository;
    private final PostResumeRepository postResumeRepository;

    private final RegionsConfig regionsConfig;


    @GetMapping
    public String favoritesPage(Model model, @AuthenticationPrincipal User currentUser) {
        if (currentUser == null) {
            log.warn("Попытка доступа к избранному без авторизации");
            return "redirect:/login";
        }

        try {
            List<Favorites> allFavorites = favorRepo.findByUser(currentUser);

            List<Favorites> productFavorites = allFavorites.stream()
                    .filter(f -> f.getPost() != null && !f.getPost().isArchived())
                    .collect(Collectors.toList());

            List<Favorites> serviceFavorites = allFavorites.stream()
                    .filter(f -> f.getPostJob() != null && !f.getPostJob().isArchived())
                    .collect(Collectors.toList());

            List<Favorites> vacancyFavorites = allFavorites.stream()
                    .filter(f -> f.getVacancy() != null && !f.getVacancy().isArchived())
                    .collect(Collectors.toList());

            List<Favorites> resumeFavorites = allFavorites.stream()
                    .filter(f -> f.getResume() != null && !f.getResume().isArchived())
                    .collect(Collectors.toList());

            long totalProducts = productFavorites.size();
            long totalServices = serviceFavorites.size();
            long totalVacancies = vacancyFavorites.size();
            long totalResume = resumeFavorites.size();

            log.debug("Загружено избранное: товаров={}, услуг={}, вакансий={}, резюме={}",
                    totalProducts, totalServices, totalVacancies, totalResume);

            List<Favorites> staticProductFavorites = productFavorites.stream().limit(8).collect(Collectors.toList());
            List<Favorites> staticServiceFavorites = serviceFavorites.stream().limit(8).collect(Collectors.toList());
            List<Favorites> staticVacancyFavorites = vacancyFavorites.stream().limit(8).collect(Collectors.toList());
            List<Favorites> staticResumeFavorites = resumeFavorites.stream().limit(8).collect(Collectors.toList());

            model.addAttribute("productFavorites", staticProductFavorites);
            model.addAttribute("serviceFavorites", staticServiceFavorites);
            model.addAttribute("vacancyFavorites", staticVacancyFavorites);
            model.addAttribute("resumeFavorites", staticResumeFavorites);
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("title", "Мои избранные");

            // ВАЖНО: Добавляем regionsConfig в модель
            model.addAttribute("regionsConfig", regionsConfig);

            model.addAttribute("initialProductsBadge", totalProducts);
            model.addAttribute("initialServicesBadge", totalServices);
            model.addAttribute("initialVacanciesBadge", totalVacancies);
            model.addAttribute("initialResumeBadge", totalResume);

            return "page_one/favorites";

        } catch (Exception e) {
            log.error("Ошибка загрузки избранного: {}", e.getClass().getSimpleName(), e);
            model.addAttribute("error", "Ошибка загрузки избранного: " + e.getMessage());
            return "error";
        }
    }

    @PostMapping("/toggle/{type}/{id}")
    @ResponseBody
    public ResponseEntity<?> toggleFavorite(
            @PathVariable String type,
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {

        if (currentUser == null) {
            log.warn("Неавторизованная попытка добавить/удалить из избранного, тип={}, id={}", type, id);
            return ResponseEntity.status(401).body(Map.of("error", "Требуется авторизация"));
        }

        Map<String, Object> response = new HashMap<>();

        try {
            boolean isFavorite;

            if (isUserOwnerOfItem(currentUser, type, id)) {
                log.warn("Пользователь пытается добавить свой собственный пост в избранное, тип={}, id={}", type, id);
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Нельзя добавить свой собственный пост в избранное"
                ));
            }

            switch (type) {
                case "product":
                    isFavorite = toggleProductFavorite(currentUser, id);
                    break;
                case "service":
                    isFavorite = toggleServiceFavorite(currentUser, id);
                    break;
                case "vacancy":
                    isFavorite = toggleVacancyFavorite(currentUser, id);
                    break;
                case "resume":
                    isFavorite = toggleResumeFavorite(currentUser, id);
                    break;
                default:
                    log.warn("Неизвестный тип избранного: {}, id={}", type, id);
                    return ResponseEntity.badRequest().body(Map.of("error", "Неверный тип"));
            }

            log.debug("Toggle избранного: тип={}, id={}, новый статус={}", type, id, isFavorite);
            response.put("isFavorite", isFavorite);
            response.put("message", isFavorite ? "Добавлено в избранное" : "Удалено из избранного");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Ошибка при toggle избранного: тип={}, id={}, ошибка={}", type, id, e.getClass().getSimpleName(), e);
            response.put("error", "Ошибка: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/load-more")
    @ResponseBody
    public ResponseEntity<SliceFavorResponseDTO<FavoriteDTO>> loadMoreFavorites(
            @RequestParam(value = "type", defaultValue = "products") String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size,
            @AuthenticationPrincipal User currentUser) {

        try {
            if (currentUser == null) {
                log.warn("Неавторизованная попытка загрузки избранного через load-more");
                return ResponseEntity.status(401).build();
            }

            log.debug("Загрузка порции избранного: тип={}, страница={}, размер={}", type, page, size);

            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "addedAt"));
            Slice<Favorites> favoritesSlice;

            switch (type) {
                case "products":
                    favoritesSlice = favorRepo.findByUserAndPostNotNull(currentUser, pageable);
                    break;
                case "services":
                    favoritesSlice = favorRepo.findByUserAndPostJobNotNull(currentUser, pageable);
                    break;
                case "vacancies":
                    favoritesSlice = favorRepo.findByUserAndVacancyNotNull(currentUser, pageable);
                    break;
                case "resume":
                    favoritesSlice = favorRepo.findByUserAndResumeNotNull(currentUser, pageable);
                    break;
                default:
                    log.warn("Неизвестный тип для load-more: {}, используем products", type);
                    favoritesSlice = favorRepo.findByUserAndPostNotNull(currentUser, pageable);
                    break;
            }

            List<FavoriteDTO> favoriteDTOs = favoritesSlice.getContent().stream()
                    .map(this::convertToFavoriteDTO)
                    .collect(Collectors.toList());

            SliceFavorResponseDTO<FavoriteDTO> response = new SliceFavorResponseDTO<>();
            response.setStatus("success");
            response.setMessage("Данные успешно загружены");
            response.setFavorites(favoriteDTOs);
            response.setHasMore(favoritesSlice.hasNext());
            response.setCurrentPage(page);
            response.setPageSize(size);

            log.debug("Загружено записей: {}, есть еще: {}", favoriteDTOs.size(), favoritesSlice.hasNext());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Ошибка загрузки избранного (load-more): тип={}, страница={}, ошибка={}", type, page, e.getClass().getSimpleName(), e);
            SliceFavorResponseDTO<FavoriteDTO> response = new SliceFavorResponseDTO<>();
            response.setStatus("error");
            response.setMessage("Ошибка загрузки: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/check/{type}/{id}")
    @ResponseBody
    public ResponseEntity<?> checkFavorite(
            @PathVariable String type,
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {

        if (currentUser == null) {
            return ResponseEntity.ok(Map.of("isFavorite", false));
        }

        boolean isFavorite = favorRepo.existsByUserAndAnyItemId(currentUser, id);
        return ResponseEntity.ok(Map.of("isFavorite", isFavorite));
    }

    @PostMapping("/remove/{favoriteId}")
    public String removeFavorite(@PathVariable Long favoriteId,
                                 @AuthenticationPrincipal User currentUser) {
        if (currentUser != null) {
            Favorites favorite = favorRepo.findById(favoriteId).orElse(null);

            if (favorite != null && favorite.getUser().getId().equals(currentUser.getId())) {
                favorRepo.delete(favorite);
                log.debug("Удалено из избранного: favoriteId={}", favoriteId);
            } else {
                log.warn("Попытка удалить чужое избранное: favoriteId={}", favoriteId);
            }
        }
        return "redirect:/page_one/favorites";
    }

    private FavoriteDTO convertToFavoriteDTO(Favorites favorite) {
        FavoriteDTO dto = new FavoriteDTO();
        dto.setId(favorite.getId());
        dto.setFavoriteId(favorite.getId());

        if (favorite.getPost() != null) {
            dto.setType("product");
            dto.setHashId(favorite.getPost().getHashId());
            dto.setTitle(favorite.getPost().getTitle());
            dto.setPrice(favorite.getPost().getPrice());
            dto.setFirstImagePath(favorite.getPost().getFirstImagePath());
            dto.setPremium(favorite.getPost().isPremium());
            dto.setDonate(favorite.getPost().isDonate());

            if (favorite.getPost().getUser() != null) {
                dto.setCity(favorite.getPost().getUser().getCity());
                dto.setCityDisplayName(regionsConfig.getCityDisplayName(
                        favorite.getPost().getUser().getRegion(),
                        favorite.getPost().getUser().getCity()));
            }

        } else if (favorite.getPostJob() != null) {
            dto.setType("service");
            dto.setHashId(favorite.getPostJob().getHashId());
            dto.setTitle(favorite.getPostJob().getTitle());
            dto.setPrice(favorite.getPostJob().getPrice());
            dto.setFirstImagePath(favorite.getPostJob().getFirstImagePath());
            dto.setPremium(favorite.getPostJob().isPremium());
            dto.setDonate(favorite.getPostJob().isDonate());

            if (favorite.getPostJob().getUser() != null) {
                dto.setCity(favorite.getPostJob().getUser().getCity());
                dto.setCityDisplayName(regionsConfig.getCityDisplayName(
                        favorite.getPostJob().getUser().getRegion(),
                        favorite.getPostJob().getUser().getCity()));
            }

        } else if (favorite.getVacancy() != null) {
            dto.setType("vacancy");
            dto.setHashId(favorite.getVacancy().getHashId());
            dto.setTitle(favorite.getVacancy().getTitle());
            dto.setCash(favorite.getVacancy().getCash());
            dto.setFirstImagePath(favorite.getVacancy().getFirstImagePath());
            dto.setPremium(favorite.getVacancy().isPremium());
            dto.setDonate(favorite.getVacancy().isDonate());

            if (favorite.getVacancy().getUser() != null) {
                dto.setCity(favorite.getVacancy().getUser().getCity());
                dto.setCityDisplayName(regionsConfig.getCityDisplayName(
                        favorite.getVacancy().getUser().getRegion(),
                        favorite.getVacancy().getUser().getCity()));
            }

        } else if (favorite.getResume() != null) {
            dto.setType("resume");
            dto.setHashId(favorite.getResume().getHashId());
            dto.setTitle(favorite.getResume().getTitle());
            dto.setCash(favorite.getResume().getCash());
            dto.setFirstImagePath(favorite.getResume().getFirstImagePath());

            if (favorite.getResume().getUser() != null) {
                dto.setCity(favorite.getResume().getUser().getCity());
                dto.setCityDisplayName(regionsConfig.getCityDisplayName(
                        favorite.getResume().getUser().getRegion(),
                        favorite.getResume().getUser().getCity()));
            }
        }

        return dto;
    }

    private boolean isUserOwnerOfItem(User user, String type, Long itemId) {
        try {
            switch (type) {
                case "product":
                    PostText post = postRepo.findById(itemId).orElse(null);
                    return post != null && post.getUser() != null &&
                            post.getUser().getId().equals(user.getId());

                case "service":
                    PostTextJob postJob = postRepoJob.findById(itemId).orElse(null);
                    return postJob != null && postJob.getUser() != null &&
                            postJob.getUser().getId().equals(user.getId());

                case "vacancy":
                    Vacancy vacancy = postVacancyRepository.findById(itemId).orElse(null);
                    return vacancy != null && vacancy.getUser() != null &&
                            vacancy.getUser().getId().equals(user.getId());

                case "resume":
                    Resume res = postResumeRepository.findById(itemId).orElse(null);
                    return res != null && res.getUser() != null &&
                            res.getUser().getId().equals(user.getId());
                default:
                    return false;
            }
        } catch (Exception e) {
            log.error("Ошибка при проверке владельца: тип={}, id={}, ошибка={}", type, itemId, e.getClass().getSimpleName());
            return false;
        }
    }

    private boolean toggleProductFavorite(User user, Long postId) {
        PostText post = postRepo.findById(postId).orElseThrow(() -> new RuntimeException("Товар не найден"));

        if (post.getUser() != null && post.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Нельзя добавить свой собственный товар в избранное");
        }

        Optional<Favorites> existing = favorRepo.findByUserAndPostId(user, postId);
        if (existing.isPresent()) {
            favorRepo.delete(existing.get());
            return false;
        } else {
            if (post.isBlocked() || post.isArchived() || post.isModeration()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Пост не найден или недоступен");
            }
            Favorites favorite = new Favorites(user, post);
            favorRepo.save(favorite);
            return true;
        }
    }

    private boolean toggleServiceFavorite(User user, Long postJobId) {
        PostTextJob postJob = postRepoJob.findById(postJobId).orElseThrow(() -> new RuntimeException("Услуга не найдена"));

        if (postJob.getUser() != null && postJob.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Нельзя добавить свою собственную услугу в избранное");
        }

        Optional<Favorites> existing = favorRepo.findByUserAndPostJobId(user, postJobId);
        if (existing.isPresent()) {
            favorRepo.delete(existing.get());
            return false;
        } else {
            if (postJob.isBlocked() || postJob.isArchived() || postJob.isModeration()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Пост не найден или недоступен");
            }
            Favorites favorite = new Favorites(user, postJob);
            favorRepo.save(favorite);
            return true;
        }
    }

    private boolean toggleVacancyFavorite(User user, Long vacancyId) {
        Vacancy vacancy = postVacancyRepository.findById(vacancyId).orElseThrow(() -> new RuntimeException("Вакансия не найдена"));

        if (vacancy.getUser() != null && vacancy.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Нельзя добавить свою собственную вакансию в избранное");
        }

        Optional<Favorites> existing = favorRepo.findByUserAndVacancyId(user, vacancyId);
        if (existing.isPresent()) {
            favorRepo.delete(existing.get());
            return false;
        } else {
            if (vacancy.isBlocked() || vacancy.isArchived() || vacancy.isModeration()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Пост не найден или недоступен");
            }
            Favorites favorite = new Favorites(user, vacancy);
            favorRepo.save(favorite);
            return true;
        }
    }

    private boolean toggleResumeFavorite(User user, Long resumeId) {
        Resume res = postResumeRepository.findById(resumeId).orElseThrow(() -> new RuntimeException("Резюме не найдено"));

        if (res.getUser() != null && res.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Нельзя добавить своё собственное резюме в избранное");
        }

        Optional<Favorites> existing = favorRepo.findByUserAndResumeId(user, resumeId);
        if (existing.isPresent()) {
            favorRepo.delete(existing.get());
            return false;
        } else {
            if (res.isBlocked() || res.isArchived() || res.isModeration()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Пост не найден или недоступен");
            }
            Favorites favorite = new Favorites(user, res);
            favorRepo.save(favorite);
            return true;
        }
    }
}