package com.agenticprice.service;

import com.agenticprice.prompt.PriceHawkPrompt;
import com.agenticprice.scraper.PriceResult;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OpenAIService {

    private final OpenAIClient client;
    private final boolean enabled;

    public OpenAIService(@Value("${OPENAI_API_KEY:}") String apiKey) {
        this.enabled = apiKey != null && !apiKey.isBlank();
        if (this.enabled) {
            this.client = OpenAIOkHttpClient.builder().apiKey(apiKey).build();
        } else {
            this.client = null;
            log.warn("OPENAI_API_KEY not set — AI features (query parsing, ranking, normalization) are disabled.");
        }
    }

    public String extractPrice(String rawHtml) {
        if (!enabled) return "";
        try {
            ChatCompletion response = client.chat().completions().create(
                    ChatCompletionCreateParams.builder()
                            .model(ChatModel.GPT_4O_MINI)
                            .addUserMessage(PriceHawkPrompt.EXTRACT_PRICE.with(rawHtml))
                            .build());
            return response.choices().isEmpty() ? "" : response.choices().get(0).message().content().orElse("");
        } catch (Exception e) {
            log.warn("OpenAI extractPrice failed: {}", e.getMessage());
            return "";
        }
    }

    public String extractProductUrl(String rawHtml) {
        if (!enabled) return "URL_NOT_FOUND";
        try {
            ChatCompletion response = client.chat().completions().create(
                    ChatCompletionCreateParams.builder()
                            .model(ChatModel.GPT_4O_MINI)
                            .addUserMessage(PriceHawkPrompt.EXTRACT_PRODUCT_URL.with(rawHtml))
                            .build());
            return response.choices().isEmpty() ? "URL_NOT_FOUND"
                    : response.choices().get(0).message().content().orElse("URL_NOT_FOUND");
        } catch (Exception e) {
            log.warn("OpenAI extractProductUrl failed: {}", e.getMessage());
            return "URL_NOT_FOUND";
        }
    }

    public String normalizeProductName(String productName) {
        if (!enabled) return productName;
        try {
            ChatCompletion response = client.chat().completions().create(
                    ChatCompletionCreateParams.builder()
                            .model(ChatModel.GPT_4O_MINI)
                            .addUserMessage(PriceHawkPrompt.NORMALIZE_PRODUCT_NAME.with(productName))
                            .build());
            return response.choices().isEmpty() ? productName
                    : response.choices().get(0).message().content().orElse(productName);
        } catch (Exception e) {
            log.warn("OpenAI normalizeProductName failed: {}", e.getMessage());
            return productName;
        }
    }

    public String parseQuery(String userQuery) {
        if (!enabled) return userQuery;
        try {
            ChatCompletion response = client.chat().completions().create(
                    ChatCompletionCreateParams.builder()
                            .model(ChatModel.GPT_4O_MINI)
                            .addUserMessage(PriceHawkPrompt.PARSE_QUERY.with(userQuery))
                            .build());
            return response.choices().isEmpty() ? userQuery
                    : response.choices().get(0).message().content().orElse(userQuery);
        } catch (Exception e) {
            log.warn("OpenAI parseQuery failed: {}", e.getMessage());
            return userQuery;
        }
    }

    public String rankProducts(List<PriceResult> products, String userQuery) {
        if (!enabled) return "";
        try {
            String input = "Query: " + userQuery + "\nProducts: " + products.toString();
            ChatCompletion response = client.chat().completions().create(
                    ChatCompletionCreateParams.builder()
                            .model(ChatModel.GPT_4O_MINI)
                            .addUserMessage(PriceHawkPrompt.RANK_PRODUCTS.with(input))
                            .build());
            return response.choices().isEmpty() ? "" : response.choices().get(0).message().content().orElse("");
        } catch (Exception e) {
            log.warn("OpenAI rankProducts failed: {}", e.getMessage());
            return "";
        }
    }
}
