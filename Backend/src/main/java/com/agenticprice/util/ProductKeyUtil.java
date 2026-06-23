package com.agenticprice.util;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProductKeyUtil {

    public static String extractProductKey(String url) {
        if (url == null) return "";
        try {
            URI uri = new URI(url);
            String path = uri.getPath();
            Matcher m = Pattern.compile("/dp/([A-Z0-9]{10})").matcher(path);
            if (m.find()) return m.group(1);
            String[] parts = path.split("/");
            String last = parts[parts.length - 1];
            return last.isEmpty() ? parts[parts.length - 2] : last;
        } catch (Exception e) {
            return url.split("\\?")[0];
        }
    }
    
}
