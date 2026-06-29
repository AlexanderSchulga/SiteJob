package jobforsite.sitejob.module.auth.controller;

import jobforsite.sitejob.module.shared.model.LicenseUser;
import jobforsite.sitejob.module.auth.repository.LicenseUserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;


/**
 * Контроллер для отображения страниц лицензионного соглашения и политики конфиденциальности
 *
 * GET /confiden_agr/agreement - пользовательское соглашение
 * GET /confiden_agr/confidential - политика конфиденциальности
 */
@RequiredArgsConstructor
@Controller
public class LicenseController {

    private final LicenseUserRepo licenseUserRepo;

    /**
     * Страница пользовательского соглашения
     */
    @GetMapping("/confiden_agr/agreement")
    public String viewsAgr(Model model) {
        Iterable<LicenseUser> agr = licenseUserRepo.findAll();
        model.addAttribute("agr", agr);
        return "confiden_agr/agreement";
    }

    /**
     * Страница политики конфиденциальности
     */
    @GetMapping("/confiden_agr/confidential")
    public String viewsConfidential(Model model) {
        Iterable<LicenseUser> conf = licenseUserRepo.findAll();
        model.addAttribute("conf", conf);
        return "confiden_agr/confidential";
    }

}