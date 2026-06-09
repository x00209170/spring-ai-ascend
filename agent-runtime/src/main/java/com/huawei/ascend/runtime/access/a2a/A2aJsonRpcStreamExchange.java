package com.huawei.ascend.runtime.access.a2a;
import com.huawei.ascend.runtime.common.RuntimeIdentity;


public record A2aJsonRpcStreamExchange(
        Object id,
        Object acceptedResponse,
        RuntimeIdentity outputHandle) {
}
