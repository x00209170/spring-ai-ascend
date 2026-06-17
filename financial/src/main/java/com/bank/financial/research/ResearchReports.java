package com.bank.financial.research;

import com.bank.financial.kit.ModelConnection;
import com.bank.financial.research.data.DataIngestionService;
import com.bank.financial.research.data.FreshnessPolicy;
import com.bank.financial.research.data.ResearchDataSource;
import com.bank.financial.research.data.http.HttpResearchDataSource;
import com.bank.financial.research.data.stub.StubBondDataSource;
import com.bank.financial.research.data.stub.StubFundDataSource;
import com.bank.financial.research.data.stub.StubResearchDataSource;
import com.bank.financial.research.data.stub.StubThematicDataSource;
import com.bank.financial.research.bond.BondReportEngine;
import com.bank.financial.research.engine.ResearchReportEngine;
import com.bank.financial.research.fund.FundReportEngine;
import com.bank.financial.research.thematic.ThematicReportEngine;
import com.bank.financial.research.model.OpenJiuwenReportModel;
import com.bank.financial.research.model.ReportModel;
import com.bank.financial.research.model.RetryReportModel;
import com.bank.financial.research.model.ScriptedReportModel;
import com.bank.financial.research.model.TimeoutReportModel;
import com.huawei.ascend.a2a.memory.obs.CompositeMemoryObserver;
import com.huawei.ascend.a2a.memory.obs.MemoryObserver;
import com.huawei.ascend.a2a.memory.obs.MicrometerMemoryObserver;
import com.huawei.ascend.a2a.memory.obs.Slf4jMemoryObserver;
import java.time.Duration;

/**
 * Wiring factory for the research-report engine. Two presets:
 * <ul>
 *   <li>{@link #offline(long)} — stub data + scripted model: deterministic, no
 *       network, no API key. Used by tests and the {@code --mock} playground.</li>
 *   <li>{@link #fromEnv(long)} — production wiring driven by env vars: an HTTP
 *       data gateway ({@code RESEARCH_DATA_BASE_URL}) when set (else the stub),
 *       and a real LLM ({@code RESEARCH_REPORT_LIVE_MODEL=true}, via BANK_LLM_*)
 *       when enabled (else the scripted model).</li>
 * </ul>
 */
public final class ResearchReports {

    private ResearchReports() {
    }

    /** Fully offline, deterministic engine (stub data + scripted model). */
    public static ResearchReportEngine offline(long asOfEpochMs) {
        ResearchDataSource source = new StubResearchDataSource(asOfEpochMs);
        DataIngestionService ingestion = new DataIngestionService(source, FreshnessPolicy.days(90));
        return new ResearchReportEngine(
                ingestion, source.name(), new ScriptedReportModel(), null, MemoryObserver.NOOP, null);
    }

    /** Production wiring from environment variables (falls back to offline pieces). */
    public static ResearchReportEngine fromEnv(long asOfEpochMs) {
        ResearchDataSource source = dataSourceFromEnv(asOfEpochMs);
        DataIngestionService ingestion = new DataIngestionService(source, FreshnessPolicy.days(envInt("RESEARCH_FRESHNESS_DAYS", 90)));
        // Live model = retry(timeout(llm)): hard per-call timeout so a stuck LLM can't
        // hang a run, wrapped in bounded backoff retry for transient connect/rate errors.
        ReportModel model = liveModel() ? liveModel(ModelConnection.forTier("smart")) : new ScriptedReportModel();
        // One instrumentation surface: routine ops via Slf4j (DEBUG), metrics via Micrometer.
        MemoryObserver observer = CompositeMemoryObserver.of(
                new Slf4jMemoryObserver(false), new MicrometerMemoryObserver());
        return new ResearchReportEngine(ingestion, source.name(), model, null, observer, null);
    }

    private static ResearchDataSource dataSourceFromEnv(long asOfEpochMs) {
        String base = System.getenv("RESEARCH_DATA_BASE_URL");
        if (base != null && !base.isBlank()) {
            return new HttpResearchDataSource(
                    base, Duration.ofSeconds(3), Duration.ofSeconds(envInt("RESEARCH_DATA_TIMEOUT_S", 8)),
                    System.getenv("RESEARCH_DATA_TOKEN"), asOfEpochMs);
        }
        return new StubResearchDataSource(asOfEpochMs);
    }

    // ── Thematic / sector-strategy engine ─────────────────────────────────────

    /** Fully offline thematic engine (scenario stub + scripted model). */
    public static ThematicReportEngine thematicOffline(long asOfEpochMs) {
        return new ThematicReportEngine(
                new StubThematicDataSource(asOfEpochMs), new ScriptedReportModel(), null, MemoryObserver.NOOP, null);
    }

    /** Thematic engine with the env-driven live model (data still from the scenario stub). */
    public static ThematicReportEngine thematicFromEnv(long asOfEpochMs) {
        ReportModel model = liveModel() ? liveModel(ModelConnection.forTier("smart")) : new ScriptedReportModel();
        return new ThematicReportEngine(
                new StubThematicDataSource(asOfEpochMs), model, null,
                CompositeMemoryObserver.of(new Slf4jMemoryObserver(false), new MicrometerMemoryObserver()), null);
    }

    /** Production live model: retry(timeout(openJiuwen)) — bounded latency + transient-failure backoff. */
    private static ReportModel liveModel(ModelConnection conn) {
        return new RetryReportModel(
                new TimeoutReportModel(new OpenJiuwenReportModel(conn),
                        Duration.ofSeconds(envInt("RESEARCH_MODEL_TIMEOUT_S", 60))),
                envInt("RESEARCH_MODEL_RETRIES", 3),
                envInt("RESEARCH_MODEL_BACKOFF_MS", 1500));
    }

    // ── Fund / FOF engine ─────────────────────────────────────────────────────

    /** Fully offline fund engine (synthetic NAV stub + scripted model). */
    public static FundReportEngine fundOffline(long asOfEpochMs) {
        return new FundReportEngine(
                new StubFundDataSource(asOfEpochMs), new ScriptedReportModel(), null, MemoryObserver.NOOP, null);
    }

    // ── Bond / fixed-income engine ────────────────────────────────────────────

    /** Fully offline bond engine (synthetic credit-issue stub + scripted model). */
    public static BondReportEngine bondOffline(long asOfEpochMs) {
        return new BondReportEngine(
                new StubBondDataSource(asOfEpochMs), new ScriptedReportModel(), null, MemoryObserver.NOOP, null);
    }

    static boolean liveModel() {
        return "true".equalsIgnoreCase(System.getenv("RESEARCH_REPORT_LIVE_MODEL"));
    }

    // ── Web playground model selection ────────────────────────────────────────

    /**
     * The model the web playground should use: the wrapped live model (retry +
     * timeout around the real LLM) when {@code live} is requested AND a real
     * endpoint is configured, otherwise the deterministic scripted model. Lets the
     * page offer a "脚本 / glm" toggle while still running with no key.
     */
    public static ReportModel webModel(boolean live) {
        return (live && glmConfigured()) ? liveModel(ModelConnection.forTier("smart")) : new ScriptedReportModel();
    }

    /**
     * Model for the single-model comparison baseline. Unlike the engine — which makes
     * many small, individually-bounded per-section calls — the baseline asks for the
     * whole report in one shot, so it needs a much longer per-call timeout and a larger
     * token budget. (That the monolithic call is slow and hard to bound is itself part
     * of the contrast.) Returns the scripted model when no live endpoint is configured.
     */
    public static ReportModel baselineModel(boolean live) {
        if (live && glmConfigured()) {
            // maxTokens must leave room for a reasoning model's ~500 reasoning tokens AND
            // the content (too small → empty content), so keep it at the engine's 4096;
            // the prose stays one-page via the prompt, so wall-time matches a section and
            // returns within the runtime's request timeout. (The engine decomposes into many
            // such bounded calls precisely because one mega-call is impractical — part of
            // what the comparison shows.)
            return new RetryReportModel(
                    new TimeoutReportModel(
                            new OpenJiuwenReportModel(ModelConnection.forTier("smart"),
                                    envInt("RESEARCH_BASELINE_MAX_TOKENS", 4096), 0.4),
                            Duration.ofSeconds(envInt("RESEARCH_BASELINE_TIMEOUT_S", 120))),
                    envInt("RESEARCH_MODEL_RETRIES", 2),
                    envInt("RESEARCH_MODEL_BACKOFF_MS", 1500));
        }
        return new ScriptedReportModel();
    }

    /** True when a real LLM endpoint is wired (BANK_LLM_* set to something non-placeholder). */
    public static boolean glmConfigured() {
        String base = System.getenv("BANK_LLM_API_BASE");
        String key = System.getenv("BANK_LLM_API_KEY");
        return base != null && !base.isBlank() && !base.contains("localhost")
                && key != null && !key.isBlank() && !"sk-local-placeholder".equals(key);
    }

    private static int envInt(String key, int def) {
        try {
            String v = System.getenv(key);
            return v == null || v.isBlank() ? def : Integer.parseInt(v);
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
