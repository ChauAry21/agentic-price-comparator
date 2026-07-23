package com.agenticprice.service;

import com.microsoft.playwright.*;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Service
public class PlaywrightService {

    private static final String CHROMIUM_PATH = findChromium();

    private static String findChromium() {
        String[] candidates = {
                System.getProperty("user.home") + "/.cache/ms-playwright/chromium-1117/chrome-linux/chrome",
                "/run/current-system/sw/bin/chromium",
                "/nix/var/nix/profiles/default/bin/chromium",
                "/usr/bin/chromium",
                "/usr/bin/chromium-browser"
        };
        for (String path : candidates) {
            if (Files.exists(Path.of(path))) {
                log.info("Playwright using Chromium at: {}", path);
                return path;
            }
        }
        log.warn("No Chromium binary found - Playwright scrapers will be disabled");
        return null;
    }

    private final ThreadLocal<Playwright> playwrightThreadLocal = ThreadLocal.withInitial(() -> {
        if (CHROMIUM_PATH == null) throw new IllegalStateException("No Chromium binary available");
        return Playwright.create();
    });

    public String fetchRenderedHtml(String url) {
        if (CHROMIUM_PATH == null) {
            log.warn("Playwright unavailable, skipping: {}", url);
            return "";
        }
        try {
            Playwright playwright = playwrightThreadLocal.get();
            try (Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions()
                            .setExecutablePath(Path.of(CHROMIUM_PATH))
                            .setHeadless(true)
                            .setArgs(java.util.List.of("--no-sandbox", "--disable-dev-shm-usage", "--disable-gpu"))
            )) {
                try (BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                        .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"))) {
                    Page page = context.newPage();
                    page.navigate(url, new Page.NavigateOptions().setTimeout(15000));
                    page.waitForTimeout(3000);
                    return page.content();
                }
            }
        } catch (Exception e) {
            log.error("Playwright failed to fetch {}: {}", url, e.getMessage());
            return "";
        }
    }

    @PreDestroy
    public void close() {
        try {
            playwrightThreadLocal.get().close();
        } catch (Exception e) {
            log.warn("Error closing Playwright: {}", e.getMessage());
        }
    }
}