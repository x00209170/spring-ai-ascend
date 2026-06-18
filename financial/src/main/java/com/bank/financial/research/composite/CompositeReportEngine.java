package com.bank.financial.research.composite;

import com.bank.financial.research.bond.BondReport;
import com.bank.financial.research.bond.BondReportEngine;
import com.bank.financial.research.data.MacroData;
import com.bank.financial.research.data.eastmoney.EastMoneyFundDataSource;
import com.bank.financial.research.data.eastmoney.EastMoneyMacroDataSource;
import com.bank.financial.research.data.stub.StubBondDataSource;
import com.bank.financial.research.data.stub.StubFundDataSource;
import com.bank.financial.research.data.stub.StubMacroDataSource;
import com.bank.financial.research.data.stub.StubThematicDataSource;
import com.bank.financial.research.engine.PipelineProgress;
import com.bank.financial.research.engine.ReportBudget;
import com.bank.financial.research.engine.ReportMetadata;
import com.bank.financial.research.engine.ReportRequest;
import com.bank.financial.research.engine.ReportSection;
import com.bank.financial.research.fund.FundReport;
import com.bank.financial.research.fund.FundReportEngine;
import com.bank.financial.research.macro.MacroReport;
import com.bank.financial.research.macro.MacroReportEngine;
import com.bank.financial.research.model.ReportModel;
import com.bank.financial.research.thematic.ThematicReport;
import com.bank.financial.research.thematic.ThematicReportEngine;
import com.huawei.ascend.a2a.memory.experience.ExperienceStore;
import com.huawei.ascend.a2a.memory.obs.MemoryObserver;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.LongSupplier;

/**
 * Composes one report from a single subject (fund / bond / none) plus any number
 * of analysis lenses (macro / industry-sector / global). Each module is produced
 * by its existing engine over the same chosen model, so every module keeps its own
 * deterministic grounding + consistency gate; the composer merges their sections
 * and disclosures into one document. This is the "标的 + 多视角组合一篇" shape.
 */
public final class CompositeReportEngine {

    private final ReportModel model;
    private final ExperienceStore experienceStore;
    private final MemoryObserver observer;
    private final LongSupplier clock;
    private final long asOf;
    private final boolean real;
    private final ReportBudget budget = new ReportBudget(1, 8000, 30, 4 * 60 * 1000L);

    public CompositeReportEngine(ReportModel model, ExperienceStore experienceStore,
            MemoryObserver observer, LongSupplier clock, long asOf, boolean real) {
        this.model = model;
        this.experienceStore = experienceStore;
        this.observer = observer == null ? MemoryObserver.NOOP : observer;
        this.clock = clock == null ? System::currentTimeMillis : clock;
        this.asOf = asOf;
        this.real = real;
    }

    /** The ordered modules (key + display label) a given selection will produce — for the UI roster. */
    public static List<Map<String, String>> modulesFor(String subject, Set<String> lenses) {
        List<Map<String, String>> mods = new ArrayList<>();
        if ("fund".equals(subject)) {
            mods.add(Map.of("role", "fund", "label", "基金分析"));
        } else if ("bond".equals(subject)) {
            mods.add(Map.of("role", "bond", "label", "债券分析"));
        }
        if (lenses.contains("macro")) {
            mods.add(Map.of("role", "macro", "label", "宏观与政策"));
        }
        if (lenses.contains("industry") || lenses.contains("sector")) {
            mods.add(Map.of("role", "sector", "label", "行业与板块策略"));
        }
        if (lenses.contains("global")) {
            mods.add(Map.of("role", "global", "label", "全球影响(定性)"));
        }
        return mods;
    }

    public CompositeReport generate(String subject, String code, Set<String> lenses, PipelineProgress progress) {
        if (progress == null) {
            progress = PipelineProgress.NOOP;
        }
        List<Map<String, String>> roster = modulesFor(subject, lenses);
        int total = roster.size();

        // Forward each module engine's blackboard writes + interaction edges to the UI
        // (so the shared-memory / interactions panels fill live), but let the composite own
        // the chip transitions at the module level (onAgent swallowed here).
        final PipelineProgress webProgress = progress;
        PipelineProgress inner = new PipelineProgress() {
            @Override
            public void onAgent(String role, String state, int index, int total) {
                // module-level chips are emitted by the composite; ignore inner agent transitions
            }

            @Override
            public void onAgentDone(String role, Map<String, String> wrote) {
                try {
                    webProgress.onAgentDone(role, wrote);
                } catch (RuntimeException ignored) {
                    // progress reporting must never affect the run
                }
            }

            @Override
            public void onInteractions(List<Map<String, String>> edges) {
                try {
                    webProgress.onInteractions(edges);
                } catch (RuntimeException ignored) {
                    // progress reporting must never affect the run
                }
            }
        };

        List<CompositeReport.Module> modules = new ArrayList<>();
        Set<String> notes = new LinkedHashSet<>();
        int modelCalls = 0;
        List<String> degradations = new ArrayList<>();
        String dataSources = real ? "东方财富(免费真实)+ 情景库" : "离线桩 / 快照";
        int idx = 0;

        for (Map<String, String> mod : roster) {
            idx++;
            String role = mod.get("role");
            emit(progress, role, "running", idx, total);
            try {
                switch (role) {
                    case "fund" -> {
                        FundReport r = fund(code, inner);
                        modules.add(new CompositeReport.Module("fund", "基金分析", r.sections()));
                        notes.addAll(r.metadata().complianceNotes());
                        modelCalls += r.metadata().modelCalls();
                        degradations.addAll(r.metadata().degradations());
                    }
                    case "bond" -> {
                        BondReport r = new BondReportEngine(new StubBondDataSource(asOf), model,
                                experienceStore, observer, clock)
                                .generate(new ReportRequest(blank(code, "DEMOBOND"), "BOND", "web", "zh-CN", asOf, budget), inner);
                        modules.add(new CompositeReport.Module("bond", "债券分析", r.sections()));
                        notes.addAll(r.metadata().complianceNotes());
                        modelCalls += r.metadata().modelCalls();
                        degradations.addAll(r.metadata().degradations());
                    }
                    case "macro" -> {
                        MacroReport r = new MacroReportEngine(
                                real ? new EastMoneyMacroDataSource(asOf) : new StubMacroDataSource(asOf),
                                model, experienceStore, observer, clock, Set.<MacroData.Domain>of())
                                .generate(new ReportRequest("中国", "MACRO", "web", "zh-CN", asOf, budget), inner);
                        modules.add(new CompositeReport.Module("macro", "宏观与政策", r.sections()));
                        notes.addAll(r.metadata().complianceNotes());
                        modelCalls += r.metadata().modelCalls();
                        degradations.addAll(r.metadata().degradations());
                    }
                    case "sector" -> {
                        String theme = "fund".equals(subject) || "bond".equals(subject) ? "中国 TMT" : blank(code, "中国 TMT");
                        ThematicReport r = new ThematicReportEngine(new StubThematicDataSource(asOf), model,
                                experienceStore, observer, clock)
                                .generate(new ReportRequest(theme, "INDUSTRY", "web", "zh-CN", asOf, budget), inner);
                        modules.add(new CompositeReport.Module("sector", "行业与板块策略", r.sections()));
                        notes.addAll(r.metadata().complianceNotes());
                        modelCalls += r.metadata().modelCalls();
                        degradations.addAll(r.metadata().degradations());
                    }
                    case "global" -> {
                        GlobalResult g = global();
                        modules.add(new CompositeReport.Module("global", "全球影响(定性)",
                                List.of(new ReportSection("global", "全球影响(定性)", g.body(), 0))));
                        modelCalls += g.calls();
                        if (g.degraded() != null) {
                            degradations.add(g.degraded());
                        }
                    }
                    default -> {
                    }
                }
            } catch (RuntimeException e) {
                degradations.add(role + ": " + e.getMessage());
                modules.add(new CompositeReport.Module(role, mod.get("label"),
                        List.of(new ReportSection(role, mod.get("label"), "(本模块生成失败,已跳过:" + e.getMessage() + ")", 0))));
            }
            emit(progress, role, "done", idx, total);
        }

        notes.add("组合说明:本报告由「标的主体 + 所选分析视角」组合而成,各模块数字均由对应确定性模型计算;"
                + "「全球影响」为定性分析,暂未接入海外实时数据源。须经持牌监督分析师(SA)复核签发。");

        String title = subjectTitle(subject, code) + " — 组合研究报告";
        String subtitle = "视角:" + roster.stream().map(m -> m.get("label")).reduce((a, b) -> a + " · " + b).orElse("(无)");
        ReportMetadata metadata = new ReportMetadata(
                model.name(), dataSources, modelCalls, 0, "COMPOSITE",
                List.of(), new ArrayList<>(notes), List.of(), degradations, clock.getAsLong());
        return new CompositeReport(title, subtitle, modules, new ArrayList<>(notes), metadata);
    }

    private FundReport fund(String code, PipelineProgress inner) {
        String c = blank(code, "110011");
        if (real) {
            try {
                return new FundReportEngine(new EastMoneyFundDataSource(asOf), model, experienceStore, observer, clock)
                        .generate(new ReportRequest(c, "FUND", "web", "zh-CN", asOf, budget), inner);
            } catch (RuntimeException e) {
                // fall through to stub
            }
        }
        return new FundReportEngine(new StubFundDataSource(asOf), model, experienceStore, observer, clock)
                .generate(new ReportRequest(real ? "DEMOFUND" : c, "FUND", "web", "zh-CN", asOf, budget), inner);
    }

    private record GlobalResult(String body, int calls, String degraded) {
    }

    private GlobalResult global() {
        String brief = "(本节为定性分析,暂未接入海外实时数据源)";
        String instruction = "就全球宏观与外部事件(美联储 FOMC、美国通胀/就业、地缘形势等)对中国资产及所选标的的"
                + "传导影响,做定性分析(约 400 字):分点说明方向与不确定性,明确标注为非实时数据的定性判断。";
        try {
            String body = model.generate(new ReportModel.ModelTask("writer", instruction, brief, 700));
            return new GlobalResult(body, 1, null);
        } catch (RuntimeException e) {
            return new GlobalResult("(全球影响章节生成失败,降级)\n" + brief, 0, "global: " + e.getMessage());
        }
    }

    private static String subjectTitle(String subject, String code) {
        return switch (subject) {
            case "fund" -> "基金 " + blank(code, "110011");
            case "bond" -> "债券 " + blank(code, "DEMOBOND");
            default -> "宏观与策略";
        };
    }

    private static String blank(String v, String def) {
        return v == null || v.isBlank() ? def : v;
    }

    private void emit(PipelineProgress progress, String role, String state, int index, int total) {
        try {
            progress.onAgent(role, state, index, total);
        } catch (RuntimeException ignored) {
            // progress reporting must never affect the run
        }
    }
}
