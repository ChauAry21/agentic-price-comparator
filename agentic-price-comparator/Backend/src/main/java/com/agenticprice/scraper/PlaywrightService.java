package com.agenticprice.scraper;

import com.microsoft.playwright.*;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class PlaywrightService {

    private final List<Playwright> allPlaywrights = Collections.synchronizedList(new ArrayList<>());
    private final List<Browser> allBrowsers = Collections.synchronizedList(new ArrayList<>());

    private final ThreadLocal<Playwright> playwrightThreadLocal = ThreadLocal.withInitial(() -> {
        Playwright pw = Playwright.create();
        allPlaywrights.add(pw);
        return pw;
    });

    private final ThreadLocal<Browser> browserThreadLocal = ThreadLocal.withInitial(() -> {
        Browser browser = playwrightThreadLocal.get()
                .chromium()
                .launch(new BrowserType.LaunchOptions().setHeadless(true));
        allBrowsers.add(browser);
        return browser;
    });

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
        for (Browser b : allBrowsers) {
            try { b.close(); } catch (Exception e) { log.warn("Error closing browser: {}", e.getMessage()); }
        }
        for (Playwright pw : allPlaywrights) {
            try { pw.close(); } catch (Exception e) { log.warn("Error closing playwright: {}", e.getMessage()); }
        }
        allBrowsers.clear();
        allPlaywrights.clear();
    }
}
