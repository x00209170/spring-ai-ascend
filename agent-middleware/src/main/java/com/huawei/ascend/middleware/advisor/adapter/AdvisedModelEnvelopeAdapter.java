package com.huawei.ascend.middleware.advisor.adapter;

import com.huawei.ascend.middleware.advisor.spi.AdvisedFinishReason;
import com.huawei.ascend.middleware.advisor.spi.AdvisedMessage;
import com.huawei.ascend.middleware.advisor.spi.AdvisedModelRequest;
import com.huawei.ascend.middleware.advisor.spi.AdvisedModelResponse;
import com.huawei.ascend.middleware.advisor.spi.AdvisedRequest;
import com.huawei.ascend.middleware.advisor.spi.AdvisedResponse;
import com.huawei.ascend.middleware.advisor.spi.AdvisedToolCall;
import com.huawei.ascend.middleware.advisor.spi.AdvisedUsage;
import com.huawei.ascend.middleware.model.spi.Message;
import com.huawei.ascend.middleware.model.spi.ModelFinishReason;
import com.huawei.ascend.middleware.model.spi.ModelInvocation;
import com.huawei.ascend.middleware.model.spi.ModelResponse;
import com.huawei.ascend.middleware.model.spi.ModelUsage;

import java.util.Map;
import java.util.Optional;

/**
 * Lossless adapter between model SPI carriers and advisor-owned carriers.
 */
public final class AdvisedModelEnvelopeAdapter {

    private AdvisedModelEnvelopeAdapter() {
    }

    public static AdvisedRequest toAdvisedRequest(
            ModelInvocation invocation,
            Map<String, Object> advisorContext) {
        AdvisedModelRequest request = new AdvisedModelRequest(
                invocation.modelId(),
                invocation.messages().stream().map(AdvisedModelEnvelopeAdapter::toAdvisedMessage).toList(),
                invocation.tools(),
                invocation.parameters(),
                invocation.hookContext());
        return new AdvisedRequest(invocation.tenantId(), request, advisorContext);
    }

    public static ModelInvocation toModelInvocation(AdvisedRequest request) {
        AdvisedModelRequest modelRequest = request.modelRequest();
        return new ModelInvocation(
                request.tenantId(),
                modelRequest.modelId(),
                modelRequest.messages().stream().map(AdvisedModelEnvelopeAdapter::toModelMessage).toList(),
                modelRequest.tools(),
                modelRequest.parameters(),
                modelRequest.hookContext());
    }

    public static AdvisedResponse toAdvisedResponse(
            String tenantId,
            ModelResponse response,
            Map<String, Object> advisorContext) {
        AdvisedModelResponse modelResponse = new AdvisedModelResponse(
                response.content(),
                response.toolCalls().stream().map(AdvisedModelEnvelopeAdapter::toAdvisedToolCall).toList(),
                AdvisedFinishReason.valueOf(response.finishReason().name()),
                Optional.ofNullable(response.usage()).map(AdvisedModelEnvelopeAdapter::toAdvisedUsage),
                response.metadata());
        return new AdvisedResponse(tenantId, modelResponse, advisorContext);
    }

    public static ModelResponse toModelResponse(AdvisedResponse response) {
        AdvisedModelResponse modelResponse = response.modelResponse();
        return new ModelResponse(
                modelResponse.content(),
                modelResponse.toolCalls().stream().map(AdvisedModelEnvelopeAdapter::toModelToolCall).toList(),
                ModelFinishReason.valueOf(modelResponse.finishReason().name()),
                modelResponse.usage().map(AdvisedModelEnvelopeAdapter::toModelUsage).orElse(null),
                modelResponse.metadata());
    }

    private static AdvisedMessage toAdvisedMessage(Message message) {
        return switch (message) {
            case Message.SystemMessage system -> AdvisedMessage.system(system.content());
            case Message.UserMessage user -> AdvisedMessage.user(user.content());
            case Message.AssistantMessage assistant -> AdvisedMessage.assistant(
                    assistant.content(),
                    assistant.toolCalls().stream().map(AdvisedModelEnvelopeAdapter::toAdvisedToolCall).toList());
            case Message.ToolResultMessage tool -> AdvisedMessage.toolResult(tool.callId(), tool.content());
        };
    }

    private static Message toModelMessage(AdvisedMessage message) {
        return switch (message.role()) {
            case SYSTEM -> new Message.SystemMessage(message.content());
            case USER -> new Message.UserMessage(message.content());
            case ASSISTANT -> new Message.AssistantMessage(
                    message.content(),
                    message.toolCalls().stream().map(AdvisedModelEnvelopeAdapter::toModelToolCall).toList());
            case TOOL -> new Message.ToolResultMessage(message.toolCallId().orElseThrow(), message.content());
        };
    }

    private static AdvisedToolCall toAdvisedToolCall(ModelResponse.ToolCall call) {
        return new AdvisedToolCall(call.callId(), call.skillKey(), call.arguments());
    }

    private static ModelResponse.ToolCall toModelToolCall(AdvisedToolCall call) {
        return new ModelResponse.ToolCall(call.callId(), call.skillKey(), call.arguments());
    }

    private static AdvisedUsage toAdvisedUsage(ModelUsage usage) {
        return new AdvisedUsage(usage.promptTokens(), usage.completionTokens(), usage.totalTokens());
    }

    private static ModelUsage toModelUsage(AdvisedUsage usage) {
        return new ModelUsage(usage.promptTokens(), usage.completionTokens(), usage.totalTokens());
    }
}
