package com.huawei.ascend.middleware.advisor.spi;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Carrier immutability + tenant scoping contract for the ChatAdvisor SPI
 * (ADR-0132).
 *
 * <p>{@link AdvisedRequest} and {@link AdvisedResponse} both:
 * <ul>
 *   <li>reject null and blank {@code tenantId} (Rule R-C.c);</li>
 *   <li>defensively copy {@code advisorContext} on construction so that
 *       mutating the input map after construction does not affect the
 *       record; and</li>
 *   <li>expose an unmodifiable {@code advisorContext} view that throws
 *       {@code UnsupportedOperationException} on mutation attempts.</li>
 * </ul>
 */
class AdvisorSpiCarrierImmutabilityTest {

    private static Map<String, Object> sampleRequestEnvelope() {
        return Map.of(
                "model", "model",
                "messages", List.of(Map.of("role", "user", "content", "hello")));
    }

    private static AdvisedModelRequest sampleModelRequest() {
        return new AdvisedModelRequest(
                "model",
                List.of(AdvisedMessage.user("hello")),
                List.of("search"),
                Map.of("temperature", 0.2),
                Map.of("traceId", "trace"));
    }

    private static AdvisedModelResponse sampleModelResponse() {
        return new AdvisedModelResponse(
                "answer",
                List.of(new AdvisedToolCall("call", "search", "{}")),
                AdvisedFinishReason.STOP,
                Optional.of(new AdvisedUsage(3, 4, 7)),
                Map.of("provider", "openai"));
    }

    @Test
    void advisedRequestRejectsNullFields() {
        assertThatThrownBy(() -> new AdvisedRequest(null, sampleModelRequest(), Map.of()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new AdvisedRequest("tenant", null, Map.of()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new AdvisedRequest("tenant", sampleModelRequest(), null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void advisedRequestRejectsBlankTenantId() {
        assertThatThrownBy(() -> new AdvisedRequest("", sampleModelRequest(), Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tenantId");
        assertThatThrownBy(() -> new AdvisedRequest("   ", sampleModelRequest(), Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tenantId");
    }

    @Test
    void advisedRequestCopiesPayloadAndAdvisorContextAndIsUnmodifiable() {
        Map<String, Object> advisorContext = new HashMap<>(Map.of("redaction", "pii"));

        AdvisedRequest request = new AdvisedRequest("tenant", sampleModelRequest(), advisorContext);

        advisorContext.put("redaction", "mutated");
        advisorContext.put("added", "after");

        assertThat(request.modelRequest().modelId()).isEqualTo("model");
        assertThat(request.modelRequest().messages()).extracting(AdvisedMessage::content).containsExactly("hello");
        assertThat(request.advisorContext()).containsEntry("redaction", "pii");
        assertThat(request.advisorContext()).doesNotContainKey("added");
        assertThatThrownBy(() -> request.modelRequest().messages().add(AdvisedMessage.user("new")))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> request.advisorContext().put("new", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void advisedResponseRejectsNullFields() {
        assertThatThrownBy(() -> new AdvisedResponse(null, sampleModelResponse(), Map.of()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new AdvisedResponse("tenant", null, Map.of()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new AdvisedResponse("tenant", sampleModelResponse(), null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void advisedResponseRejectsBlankTenantId() {
        assertThatThrownBy(() -> new AdvisedResponse("", sampleModelResponse(), Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tenantId");
        assertThatThrownBy(() -> new AdvisedResponse("\t", sampleModelResponse(), Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tenantId");
    }

    @Test
    void advisedResponseCopiesPayloadAndAdvisorContextAndIsUnmodifiable() {
        Map<String, Object> advisorContext = new HashMap<>(Map.of("cost", 0.12));

        AdvisedResponse response = new AdvisedResponse("tenant", sampleModelResponse(), advisorContext);

        advisorContext.put("cost", 9.99);
        advisorContext.put("added", "after");

        assertThat(response.modelResponse().content()).isEqualTo("answer");
        assertThat(response.modelResponse().toolCalls()).extracting(AdvisedToolCall::skillKey)
                .containsExactly("search");
        assertThat(response.advisorContext()).containsEntry("cost", 0.12);
        assertThat(response.advisorContext()).doesNotContainKey("added");
        assertThatThrownBy(() -> response.modelResponse().toolCalls().add(
                new AdvisedToolCall("new", "search", "{}")))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> response.advisorContext().put("new", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void streamingAdvisorCanWrapStreamingChainWithSamePackageChunks() {
        AdvisedRequest request = new AdvisedRequest("tenant", sampleModelRequest(), Map.of());
        AdvisedResponse response = new AdvisedResponse("tenant", sampleModelResponse(), Map.of());
        StreamingAdvisorChain chain = ignored -> Stream.of(
                new AdvisedStreamChunk.ContentDelta("hel"),
                new AdvisedStreamChunk.ContentDelta("lo"),
                new AdvisedStreamChunk.Complete(response));
        StreamingChatAdvisor advisor = new StreamingChatAdvisor() {
            @Override
            public String advisorName() {
                return "test-streaming-advisor";
            }

            @Override
            public int order() {
                return 0;
            }

            @Override
            public Stream<AdvisedStreamChunk> aroundStream(AdvisedRequest advisedRequest, StreamingAdvisorChain next) {
                return next.proceed(advisedRequest);
            }
        };

        List<AdvisedStreamChunk> chunks = advisor.aroundStream(request, chain).toList();

        assertThat(chunks).hasSize(3);
        assertThat(chunks.get(0)).isInstanceOf(AdvisedStreamChunk.ContentDelta.class);
        assertThat(chunks.get(2)).isInstanceOfSatisfying(AdvisedStreamChunk.Complete.class,
                complete -> assertThat(complete.finalResponse()).isSameAs(response));
    }

    @Test
    void advisedModelRequestRejectsSchemaLessMessagesAndBlankModelId() {
        assertThatThrownBy(() -> new AdvisedModelRequest(
                " ",
                List.of(AdvisedMessage.user("hello")),
                List.of(),
                Map.of(),
                Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("modelId");
        assertThatThrownBy(() -> new AdvisedModelRequest(
                "model",
                List.of(),
                List.of(),
                Map.of(),
                Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("messages");
    }
}
