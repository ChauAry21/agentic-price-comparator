package com.agenticprice.service;


import com.agenticprice.prompt.PriceHawkPrompt;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;

public class OpenAIService {
    private final OpenAIClient client;

    public OpenAIService() {
        this.client = OpenAIOkHttpClient.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
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
