package jobforsite.sitejob.module.shared;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Контроллер для генерации карты сайта (sitemap).
 * <p>
 * Предоставляет эндпоинт {@code /sitemap.xml}, возвращающий XML-документ
 * в формате Sitemap Protocol, содержащий перечень основных URL сайта
 * с указанием частоты обновления и приоритета.
 */
@Controller
public class SitemapController {

    @GetMapping(value = "/sitemap.xml", produces = "application/xml")
    @ResponseBody
    public String sitemap() {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        // 1. Главная
        xml.append("  <url>\n");
        xml.append("    <loc>https://biranaku.ru/</loc>\n");
        xml.append("    <changefreq>daily</changefreq>\n");
        xml.append("    <priority>1.0</priority>\n");
        xml.append("  </url>\n");

        // 2. О нас
        xml.append("  <url>\n");
        xml.append("    <loc>https://biranaku.ru/about</loc>\n");
        xml.append("    <changefreq>monthly</changefreq>\n");
        xml.append("    <priority>0.5</priority>\n");
        xml.append("  </url>\n");

        // 3. Товары (каталог)
        xml.append("  <url>\n");
        xml.append("    <loc>https://biranaku.ru/page_one/viewsone</loc>\n");
        xml.append("    <changefreq>daily</changefreq>\n");
        xml.append("    <priority>0.9</priority>\n");
        xml.append("  </url>\n");

        // 4. Услуги (каталог)
        xml.append("  <url>\n");
        xml.append("    <loc>https://biranaku.ru/page_one/viewsonejob</loc>\n");
        xml.append("    <changefreq>daily</changefreq>\n");
        xml.append("    <priority>0.9</priority>\n");
        xml.append("  </url>\n");

        // 5. Вакансии (каталог)
        xml.append("  <url>\n");
        xml.append("    <loc>https://biranaku.ru/page_one/viewsonevac</loc>\n");
        xml.append("    <changefreq>daily</changefreq>\n");
        xml.append("    <priority>0.9</priority>\n");
        xml.append("  </url>\n");

        // 6. Резюме (каталог)
        xml.append("  <url>\n");
        xml.append("    <loc>https://biranaku.ru/page_one/viewsoneres</loc>\n");
        xml.append("    <changefreq>daily</changefreq>\n");
        xml.append("    <priority>0.9</priority>\n");
        xml.append("  </url>\n");

        // 7. Регистрация
        xml.append("  <url>\n");
        xml.append("    <loc>https://biranaku.ru/register</loc>\n");
        xml.append("    <changefreq>monthly</changefreq>\n");
        xml.append("    <priority>0.6</priority>\n");
        xml.append("  </url>\n");

        // 8. Вход
        xml.append("  <url>\n");
        xml.append("    <loc>https://biranaku.ru/login</loc>\n");
        xml.append("    <changefreq>monthly</changefreq>\n");
        xml.append("    <priority>0.6</priority>\n");
        xml.append("  </url>\n");

        // 9. Поддержка
        xml.append("  <url>\n");
        xml.append("    <loc>https://biranaku.ru/page_one/support_service</loc>\n");
        xml.append("    <changefreq>weekly</changefreq>\n");
        xml.append("    <priority>0.4</priority>\n");
        xml.append("  </url>\n");

        xml.append("</urlset>");
        return xml.toString();
    }
}