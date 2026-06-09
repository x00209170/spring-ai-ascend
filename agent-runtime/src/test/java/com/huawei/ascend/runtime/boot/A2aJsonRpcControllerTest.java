package com.huawei.ascend.runtime.boot;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Flow;
import org.a2aproject.sdk.grpc.StreamResponse;
import org.a2aproject.sdk.grpc.utils.JSONRPCUtils;
import org.a2aproject.sdk.jsonrpc.common.wrappers.A2ARequest;
import org.a2aproject.sdk.server.ServerCallContext;
import org.a2aproject.sdk.server.requesthandlers.RequestHandler;
import org.a2aproject.sdk.spec.A2AError;
import org.a2aproject.sdk.spec.CancelTaskParams;
import org.a2aproject.sdk.spec.EventKind;
import org.a2aproject.sdk.spec.ListTaskPushNotificationConfigsParams;
import org.a2aproject.sdk.spec.ListTaskPushNotificationConfigsResult;
import org.a2aproject.sdk.spec.ListTasksParams;
import org.a2aproject.sdk.spec.Message;
import org.a2aproject.sdk.spec.MessageSendParams;
import org.a2aproject.sdk.spec.Part;
import org.a2aproject.sdk.spec.StreamingEventKind;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TaskIdParams;
import org.a2aproject.sdk.spec.TaskPushNotificationConfig;
import org.a2aproject.sdk.spec.TaskQueryParams;
import org.a2aproject.sdk.spec.TextPart;
import org.junit.jupiter.api.Test;
import org.springframework.http.codec.ServerSentEvent;
import reactor.test.StepVerifier;

class A2aJsonRpcControllerTest {

    @Test
    void streamingResponseDataIsJsonRpcEventReadableByA2aSdkClient() throws Exception {
        A2aJsonRpcController controller = new A2aJsonRpcController(new SingleEventRequestHandler());
        String body = """
                {"jsonrpc":"2.0","id":"request-1","method":"SendStreamingMessage","params":{"message":{"role":"ROLE_USER","parts":[{"text":"ping"}],"messageId":"message-1"}}}
                """;
        A2ARequest<?> request = JSONRPCUtils.parseRequestBody(body, null);

        StepVerifier.create(controller.handleStream(request))
                .assertNext(event -> assertThat(sdkParsedPayload(event)).isEqualTo(StreamResponse.PayloadCase.MESSAGE))
                .verifyComplete();
    }

    private static StreamResponse.PayloadCase sdkParsedPayload(ServerSentEvent<String> event) {
        assertThat(event.event()).isEqualTo("jsonrpc");
        assertThat(event.data()).isNotBlank();
        try {
            return JSONRPCUtils.parseResponseEvent(event.data()).getPayloadCase();
        } catch (Exception e) {
            throw new AssertionError("SSE data is not parseable by the A2A SDK client", e);
        }
    }

    private static final class SingleEventRequestHandler implements RequestHandler {
        @Override
        public Flow.Publisher<StreamingEventKind> onMessageSendStream(
                MessageSendParams params, ServerCallContext context) {
            Message message = Message.builder()
                    .role(Message.Role.ROLE_AGENT)
                    .messageId("message-2")
                    .parts(List.<Part<?>>of(new TextPart("pong")))
                    .metadata(Map.of("runStatus", "completed"))
                    .build();
            return subscriber -> {
                subscriber.onSubscribe(new Flow.Subscription() {
                    @Override
                    public void request(long n) {
                    }

                    @Override
                    public void cancel() {
                    }
                });
                subscriber.onNext(message);
                subscriber.onComplete();
            };
        }

        @Override
        public Flow.Publisher<StreamingEventKind> onSubscribeToTask(TaskIdParams params, ServerCallContext context) {
            return onMessageSendStream(null, context);
        }

        @Override
        public Task onGetTask(TaskQueryParams params, ServerCallContext context) throws A2AError {
            throw unsupported();
        }

        @Override
        public org.a2aproject.sdk.jsonrpc.common.wrappers.ListTasksResult onListTasks(
                ListTasksParams params, ServerCallContext context) throws A2AError {
            throw unsupported();
        }

        @Override
        public Task onCancelTask(CancelTaskParams params, ServerCallContext context) throws A2AError {
            throw unsupported();
        }

        @Override
        public EventKind onMessageSend(MessageSendParams params, ServerCallContext context) throws A2AError {
            throw unsupported();
        }

        @Override
        public TaskPushNotificationConfig onCreateTaskPushNotificationConfig(
                TaskPushNotificationConfig params, ServerCallContext context) throws A2AError {
            throw unsupported();
        }

        @Override
        public TaskPushNotificationConfig onGetTaskPushNotificationConfig(
                org.a2aproject.sdk.spec.GetTaskPushNotificationConfigParams params,
                ServerCallContext context) throws A2AError {
            throw unsupported();
        }

        @Override
        public ListTaskPushNotificationConfigsResult onListTaskPushNotificationConfigs(
                ListTaskPushNotificationConfigsParams params, ServerCallContext context) throws A2AError {
            throw unsupported();
        }

        @Override
        public void onDeleteTaskPushNotificationConfig(
                org.a2aproject.sdk.spec.DeleteTaskPushNotificationConfigParams params,
                ServerCallContext context) throws A2AError {
            throw unsupported();
        }

        @Override
        public void validateRequestedTask(String requestedTaskId) throws A2AError {
        }

        private static UnsupportedOperationException unsupported() {
            return new UnsupportedOperationException("not used by this test");
        }
    }
}
