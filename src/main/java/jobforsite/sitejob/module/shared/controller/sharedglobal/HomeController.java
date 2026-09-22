package jobforsite.sitejob.module.shared.controller.sharedglobal;

import jobforsite.sitejob.module.product.model.PostText;
import jobforsite.sitejob.module.product.repository.PostRepo;
import jobforsite.sitejob.module.service.model.PostTextJob;
import jobforsite.sitejob.module.service.repository.PostRepoJob;
import jobforsite.sitejob.module.shared.dto.PaidCarouselItem;
import jobforsite.sitejob.module.shared.services.BlockedPostsNotificationService;
import jobforsite.sitejob.module.shared.services.cachrateservice.ModerationPostsCacheService;
import jobforsite.sitejob.module.users.model.User;
import jobforsite.sitejob.module.vacancy.model.Vacancy;
import jobforsite.sitejob.module.vacancy.repository.PostVacancyRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Controller
public class HomeController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(HomeController.class);

    private final BlockedPostsNotificationService blockedPostsNotificationService;
    private final PostRepo postRepo;
    private final PostRepoJob postRepoJob;
    private final PostVacancyRepository postVacancyRepository;

    private final ModerationPostsCacheService moderationPostsCacheService;

    @GetMapping({"/", "/index"})
    @Cacheable(value = "index", key = "#authentication?.principal?.id ?: 'anonymous'")
    public String home(Model model, Authentication authentication) {
        log.debug("Home page requested, authenticated: {}", authentication != null && authentication.isAuthenticated());

        model.addAttribute("title", "Biranaku");

        if (authentication != null && authentication.isAuthenticated()) {
            User currentUser = (User) authentication.getPrincipal();
            log.info("Authenticated user accessed home page, role: {}", currentUser.getRole());


            // Для SUPER и ADMIN - проверка наличия объявлений на модерации
            if (currentUser.getRole().equals("ROLE_SUPER") || currentUser.getRole().equals("ROLE_ADMIN")) {
                long moderationCount = moderationPostsCacheService.getTotalModerationCount();
                boolean hasModerationPosts = moderationCount > 0;
                model.addAttribute("hasModerationPosts", hasModerationPosts);
                model.addAttribute("moderationCount", moderationCount);
                log.info("Moderation posts count: {}", moderationCount);
            }

            if (!currentUser.getRole().equals("ROLE_SUPER")) {
                boolean hasBlockedPosts = blockedPostsNotificationService.hasBlockedPosts(currentUser);
                int blockedCount = blockedPostsNotificationService.getBlockedPostsCount(currentUser);
                model.addAttribute("hasBlockedPosts", hasBlockedPosts);
                model.addAttribute("blockedPostsCount", blockedCount);
            }
        } else {
            log.debug("Anonymous user accessed home page");
        }

        //передаем данные для карусели (для анонимов тоже!)
        List<PaidCarouselItem> paidItems = getPaidCarouselItemsFresh();
        model.addAttribute("paidCarouselItems", paidItems);
        log.info("Paid carousel items count: {}", paidItems.size());

        return "index";
    }

    @GetMapping("/about")
    @Cacheable(value = "about", key = "#authentication?.principal?.id ?: 'anonymous'")
    public String aboutPage(Model model) {
        log.info("About page requested");
        model.addAttribute("title", "О нас");
        return "about";
    }

    @GetMapping("/promotion")
    @Cacheable(value = "promotion", key = "#authentication?.principal?.id ?: 'anonymous'")
    public String promotionPage(Model model) {
        log.info("Promotion page requested");
        model.addAttribute("title", "Продвижение, тарифы и оплата");
        return "promotion";
    }

    @GetMapping("/page_one/support_service")
    public String supportPage(Model model) {
        log.info("Support service page requested");
        model.addAttribute("title", "Поддержка");
        return "/page_one/support_service";
    }

    // API для загрузки карусели - ВСЕГДА СВЕЖИЕ ДАННЫЕ
    @GetMapping("/api/paid-carousel")
    @ResponseBody
    public List<PaidCarouselItem> getPaidCarousel() {
        return getPaidCarouselItemsFresh();
    }

    // ПРИВАТНЫЙ МЕТОД - всегда ходит в БД, без кеша
    private List<PaidCarouselItem> getPaidCarouselItemsFresh() {
        log.info("Loading FRESH paid carousel items from database");
        List<PaidCarouselItem> paidItems = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoffDate = now.minusDays(30);

        // 1. Платные товары - используем кастомный запрос
        List<PostText> premiumProducts = postRepo.findPremiumActivePosts(cutoffDate);

        for (PostText product : premiumProducts) {
            PaidCarouselItem item = new PaidCarouselItem();
            item.setType("product");
            item.setHashId(product.getHashId());
            item.setTitle(product.getTitle());
            item.setCity(product.getUser() != null ? product.getUser().getCity() : "Не указан");
            item.setImage(product.getFirstImagePath());
            item.setLink("/postidviews/" + product.getHashId());
            item.setPriority(product.getPremiumPriority());
            item.setPlaceholder(false);
            paidItems.add(item);
        }

        // 2. Платные услуги
        List<PostTextJob> premiumServices = postRepoJob.findAll().stream()
                .filter(s -> s.isPremium()
                        && s.getModeratedAt() != null
                        && s.getModeratedAt().plusDays(30).isAfter(now)
                        && !s.isBlocked())
                .collect(Collectors.toList());

        for (PostTextJob service : premiumServices) {
            PaidCarouselItem item = new PaidCarouselItem();
            item.setType("service");
            item.setHashId(service.getHashId());
            item.setTitle(service.getTitle());
            item.setCity(service.getUser() != null ? service.getUser().getCity() : "Не указан");
            item.setImage(service.getFirstImagePath());
            item.setLink("/postidviewsjob/" + service.getHashId());
            item.setPriority(service.getPremiumPriority());
            item.setPlaceholder(false);
            paidItems.add(item);
        }

        // 3. Платные вакансии
        List<Vacancy> premiumVacancies = postVacancyRepository.findAll().stream()
                .filter(v -> v.isPremium()
                        && v.getModeratedAt() != null
                        && v.getModeratedAt().plusDays(30).isAfter(now)
                        && !v.isBlocked())
                .collect(Collectors.toList());

        for (Vacancy vacancy : premiumVacancies) {
            PaidCarouselItem item = new PaidCarouselItem();
            item.setType("vacancy");
            item.setHashId(vacancy.getHashId());
            item.setTitle(vacancy.getTitle());
            item.setCity(vacancy.getUser() != null ? vacancy.getUser().getCity() : "Не указан");
            item.setImage(vacancy.getFirstImagePath());
            item.setLink("/postidviewsvac/" + vacancy.getHashId());
            item.setPriority(vacancy.getPremiumPriority());
            item.setPlaceholder(false);
            paidItems.add(item);
        }

        // Сортируем по приоритету (чем выше, тем первее)
        paidItems.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));

        // Ограничиваем 15 элементами
        if (paidItems.size() > 15) {
            paidItems = paidItems.subList(0, 15);
        }

        log.info("Found {} fresh paid items", paidItems.size());
        return paidItems;
    }

    @GetMapping(value = "/robots.txt", produces = "text/plain")
    @ResponseBody
    public String robots() {
        return """
           User-agent: *
           Disallow: /admin/
           Disallow: /user/profile/
           Disallow: /page_two/
           Disallow: /api/rating/
           Disallow: /uploads/
           Disallow: /debug-users
           Allow: /

           Sitemap: http://biranaku.ru/sitemap.xml
           """;
    }

}