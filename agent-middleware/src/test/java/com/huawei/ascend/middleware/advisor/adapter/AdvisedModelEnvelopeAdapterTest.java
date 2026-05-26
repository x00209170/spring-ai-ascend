package com.huawei.ascend.middleware.advisor.adapter;

import com.huawei.ascend.middleware.advisor.spi.AdvisedRequest;
import com.huawei.ascend.middleware.advisor.spi.AdvisedResponse;
import com.huawei.ascend.middleware.model.spi.Message;
import com.huawei.ascend.middleware.model.spi.ModelFinishReason;
import com.huawei.ascend.middleware.model.spi.ModelInvocation;
import com.huawei.ascend.middleware.model.spi.ModelResponse;
import com.huawei.ascend.middleware.model.spi.ModelUsage;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AdvisedModelEnvelopeAdapterTest {

    @Test
    void modelInvocationRoundTripsThroughTypedAdvisedRequest() {
        ModelResponse.ToolCall toolCall = new ModelResponse.ToolCall("call", "search", "{\"q\":\"hi\"}");
        ModelInvocation invocation = new ModelInvocation(
                "tenant",
                "model",
                List.of(
                        new Message.SystemMessage("system"),
                        new Message.UserMessage("hello"),
                        new Message.AssistantMessage("", List.of(toolCall)),
                        new Message.ToolResultMessage("call", "{\"ok\":true}")),
                List.of("search"),
                Map.of("temperature", 0.2),
                Map.of("traceId", "trace"));

        AdvisedRequest advised = AdvisedModelEnvelopeAdapter.toAdvisedRequest(invocation, Map.of("policy", "pii"));
        ModelInvocation roundTrip = AdvisedModelEnvelopeAdapter.toModelInvocation(advised);

        assertThat(roundTrip).isEqualTo(invocation);
        assertThat(advised.modelRequest().messages()).hasSize(4);
        assertThat(advised.advisorContext()).containsEntry("policy", "pii");
    }

    @Test
    void modelResponseRoundTripsThroughTypedAdvisedResponse() {
        ModelResponse response = new ModelResponse(
                "answer",
                List.of(new ModelResponse.ToolCall("call", "search", "{}")),
                ModelFinishReason.TOOL_CALLS,
                new ModelUsage(2, 3, 5),
                Map.of("provider", "test"));

        AdvisedResponse advised = AdvisedModelEnvelopeAdapter.toAdvisedResponse(
                "tenant",
                response,
                Map.of("costCenter", "team"));
        ModelResponse roundTrip = AdvisedModelEnvelopeAdapter.toModelResponse(advised);

        assertThat(roundTrip).isEqualTo(response);
        assertThat(advised.tenantId()).isEqualTo("tenant");
        assertThat(advised.advisorContext()).containsEntry("costCenter", "team");
    }
}
