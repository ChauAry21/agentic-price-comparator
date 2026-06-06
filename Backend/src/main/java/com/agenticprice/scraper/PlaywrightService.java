package com.agenticprice.scraper;

import com.microsoft.playwright.*;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PlaywrightService {

    private final ThreadLocal<Playwright> playwrightThreadLocal = ThreadLocal.withInitial(Playwright::create);
    private final ThreadLocal<Browser> browserThreadLocal = ThreadLocal.withInitial(() ->
            playwrightThreadLocal.get().chromium().launch(new BrowserType.LaunchOptions().setHeadless(true))
    );

    public String fetchRenderedHtml(String url) {
        try {
            Browser browser = browserThreadLocal.get();
            try (BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"))) {
                Page page = context.newPage();
                page.navigate(url, new Page.NavigateOptions().setTimeout(15000));
                page.waitForTimeout(3000);
                return page.content();
            }
        } catch (Exception e) {
            log.error("Playwright failed to fetch {}: {}", url, e.getMessage());
            return "";
        }
    }

    @PreDestroy
    public void close() {
        try {
            browserThreadLocal.get().close();
            playwrightThreadLocal.get().close();
        } catch (Exception e) {
            log.warn("Error closing Playwright: {}", e.getMessage());
        }
    }
}