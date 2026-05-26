package com.huawei.ascend.middleware.advisor.spi;

/**
 * Around-call interceptor over model invocation.
 *
 * <p>Authority: ADR-0132. Runtime binding follows sequence
 * {@code advisor-model-hook-order/v1}: customers bind advisors at agent
 * definition time; the platform translates the model invocation to this
 * package's typed carriers, fires {@code BEFORE_LLM}, runs the advisor
 * chain around the model call, translates the final response, then fires
 * {@code AFTER_LLM}. Customers never import {@code HookDispatcher}.
 *
 * <p>Implementations:
 * <ul>
 *   <li>MUST return a stable, non-blank {@link #advisorName()} for
 *       registry lookup, hook attribution, and audit.</li>
 *   <li>MUST be thread-safe; the runtime invokes from virtual
 *       threads.</li>
 *   <li>MUST call {@link AdvisorChain#next(AdvisedRequest)} exactly
 *       once unless they short-circuit by returning a synthetic
 *       {@link AdvisedResponse} (PII-filter rejection, cache hit,
 *       policy denial).</li>
 * </ul>
 *
 * <p>SPI purity per Rule R-D: imports only {@code java.*} +
 * same-module middleware SPI siblings.
 */
public interface ChatAdvisor {

    /** Stable advisor identifier (e.g. {@code "pii-redaction-advisor"}); non-blank. */
    String advisorName();

    /**
     * Ordinal sort key — advisors with smaller {@code order()} run first
     * on the inbound (around-request) leg and last on the outbound
     * (around-response) leg. Convention mirrors Spring's
     * {@code Ordered.LOWEST_PRECEDENCE = Integer.MAX_VALUE}.
     */
    int order();

    /**
     * Intercept a model call. Implementations MUST call
     * {@link AdvisorChain#next(AdvisedRequest)} exactly once, unless
     * they short-circuit the call by returning a synthetic
     * {@link AdvisedResponse} (e.g. PII-filter rejection or cache hit).
     *
     * @param request the advised request envelope; never null.
     * @param chain   the advisor chain; never null.
     * @return the advised response; never null.
     */
    AdvisedResponse aroundCall(AdvisedRequest request, AdvisorChain chain);
}
