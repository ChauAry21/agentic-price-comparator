package com.agenticprice.service;


import com.agenticprice.prompt.PriceHawkPrompt;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class OpenAIService {
    private final OpenAIClient client;

    public OpenAIService(@Value("${OPENAI_API_KEY}") String apiKey) {
        this.client = OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .build();
    }

    public String extractPrice(String rawHtml) {
        ChatCompletion response = client.chat().completions().create(
                ChatCompletionCreateParams.builder()
                        .model(ChatModel.GPT_4O_MINI)
                        .addUserMessage(PriceHawkPrompt.EXTRACT_PRICE.with(rawHtml))
                        .build()
        );

        return response.choices().get(0).message().content().orElse("");
    }

    public String extractProductUrl(String rawHtml) {
        ChatCompletion response = client.chat().completions().create(
                ChatCompletionCreateParams.builder()
                        .model(ChatModel.GPT_4O_MINI)
                        .addUserMessage(PriceHawkPrompt.EXTRACT_PRODUCT_URL.with(rawHtml))
                        .build()
        );
        return response.choices().get(0).message().content().orElse("URL_NOT_FOUND");
    }

    public String normalizeProductName(String productName) {
        ChatCompletion response = client.chat().completions().create(
                ChatCompletionCreateParams.builder()
                        .model(ChatModel.GPT_4O_MINI)
                        .addUserMessage(PriceHawkPrompt.NORMALIZE_PRODUCT_NAME.with(productName))
                        .build()
        );
        return response.choices().get(0).message().content().orElse(productName);
    }

    public String parseQuery(String userQuery) {
        ChatCompletion response = client.chat().completions().create(
                ChatCompletionCreateParams.builder()
                        .model(ChatModel.GPT_4O_MINI)
                        .addUserMessage(PriceHawkPrompt.PARSE_QUERY.with(userQuery))
                        .build()
        );

        return  response.choices().get(0).message().content().orElse("");
    }
}
