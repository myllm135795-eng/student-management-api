package com.example.studentmanagement.service;

import com.example.studentmanagement.dto.ChatRequest;
import com.example.studentmanagement.dto.ChatResponse;
import com.openai.client.OpenAIClient;
import com.openai.models.ChatCompletion;
import com.openai.models.ChatCompletionCreateParams;
import com.openai.models.ChatCompletionMessage;
import com.openai.models.ChatCompletionRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
public class ChatbotService {

    @Autowired
    private OpenAIClient openAIClient;

    @Autowired
    private StudentApiClientService studentApiClient;

    private final Map<String, List<ChatCompletionMessage>> conversationHistory = new HashMap<>();
    private static final String SYSTEM_PROMPT = "You are a helpful student management assistant. " +
            "You can help students inquire about their information, courses, and enrollment status. " +
            "You have access to a student management system API. When users ask about students, " +
            "use the information available to provide accurate responses. Always be professional and helpful.";

    public ChatResponse chat(ChatRequest request) {
        ChatResponse response = new ChatResponse();
        response.setUserMessage(request.getMessage());
        response.setTimestamp(LocalDateTime.now());
        
        String conversationId = request.getConversationId() != null ? 
                request.getConversationId() : UUID.randomUUID().toString();
        response.setConversationId(conversationId);

        try {
            // Get or create conversation history
            List<ChatCompletionMessage> messages = conversationHistory.getOrDefault(
                    conversationId, new ArrayList<>()
            );

            // Add system message if this is a new conversation
            if (messages.isEmpty()) {
                messages.add(ChatCompletionMessage.builder()
                    .role(ChatCompletionRole.SYSTEM)
                    .content(SYSTEM_PROMPT)
                    .build());
            }

            // Add user message
            messages.add(ChatCompletionMessage.builder()
                .role(ChatCompletionRole.USER)
                .content(request.getMessage())
                .build());

            // Call OpenAI API
            ChatCompletion completion = openAIClient.chat().completions().create(
                ChatCompletionCreateParams.builder()
                    .model("gpt-3.5-turbo")
                    .messages(messages)
                    .maxTokens(500)
                    .temperature(0.7)
                    .build()
            );

            String botResponse = completion.choices().get(0).message().content().orElse("");
            response.setBotResponse(botResponse);
            response.setSuccess(true);

            // Add assistant response to history
            messages.add(ChatCompletionMessage.builder()
                .role(ChatCompletionRole.ASSISTANT)
                .content(botResponse)
                .build());
            conversationHistory.put(conversationId, messages);

            log.info("Chat processed successfully for conversation: {}", conversationId);

        } catch (Exception e) {
            log.error("Error processing chat message", e);
            response.setSuccess(false);
            response.setError(e.getMessage());
        }

        return response;
    }

    public ChatResponse chatWithContext(ChatRequest request) {
        ChatResponse response = new ChatResponse();
        response.setUserMessage(request.getMessage());
        response.setTimestamp(LocalDateTime.now());
        
        String conversationId = request.getConversationId() != null ? 
                request.getConversationId() : UUID.randomUUID().toString();
        response.setConversationId(conversationId);

        try {
            // Fetch student context from API
            String studentContext = studentApiClient.getStudentContext();
            
            String enrichedSystemPrompt = SYSTEM_PROMPT + "\n\nCurrent Student Database:\n" + studentContext;

            // Get or create conversation history
            List<ChatCompletionMessage> messages = conversationHistory.getOrDefault(
                    conversationId, new ArrayList<>()
            );

            if (messages.isEmpty()) {
                messages.add(ChatCompletionMessage.builder()
                    .role(ChatCompletionRole.SYSTEM)
                    .content(enrichedSystemPrompt)
                    .build());
            }

            messages.add(ChatCompletionMessage.builder()
                .role(ChatCompletionRole.USER)
                .content(request.getMessage())
                .build());

            ChatCompletion completion = openAIClient.chat().completions().create(
                ChatCompletionCreateParams.builder()
                    .model("gpt-3.5-turbo")
                    .messages(messages)
                    .maxTokens(500)
                    .temperature(0.7)
                    .build()
            );

            String botResponse = completion.choices().get(0).message().content().orElse("");
            response.setBotResponse(botResponse);
            response.setSuccess(true);

            messages.add(ChatCompletionMessage.builder()
                .role(ChatCompletionRole.ASSISTANT)
                .content(botResponse)
                .build());
            conversationHistory.put(conversationId, messages);

            log.info("Context-aware chat processed for conversation: {}", conversationId);

        } catch (Exception e) {
            log.error("Error processing context-aware chat", e);
            response.setSuccess(false);
            response.setError(e.getMessage());
        }

        return response;
    }

    public void clearConversation(String conversationId) {
        conversationHistory.remove(conversationId);
        log.info("Conversation cleared: {}", conversationId);
    }

    public boolean conversationExists(String conversationId) {
        return conversationHistory.containsKey(conversationId);
    }
}
