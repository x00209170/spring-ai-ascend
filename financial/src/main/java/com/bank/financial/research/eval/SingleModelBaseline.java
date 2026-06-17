package com.bank.financial.research.eval;

import com.bank.financial.research.data.CompanyData;
import com.bank.financial.research.engine.Bb;
import com.bank.financial.research.model.ReportModel;

/**
 * The "just prompt one big model" baseline: hand the model the same raw data the
 * engine's agents collectively have, and ask it — in a single call, with no
 * blackboard, no deterministic calc, no independent critic, no compliance agent —
 * to produce the whole report including the rating, target price and valuation it
 * computes itself. This is the honest representative of "为什么不直接用大模型",
 * scored against the engine by {@link MethodComparison}.
 *
 * <p>The instruction deliberately does NOT impose the grounding discipline the
 * engine's roles carry (see the {@code baseline} branch in the live model's role
 * prompt): the whole point is to observe what an undisciplined single call does
 * with the numbers.
 */
public final class SingleModelBaseline {

    private SingleModelBaseline() {
    }

    /** Generate a one-shot baseline report (one model call) from the dataset. */
    public static String generate(CompanyData.Dataset ds, ReportModel model) {
        String brief = digest(ds);
        String instruction =
                "你是一名卖方股票分析师。请仅凭下列原始数据,一次性撰写一份简明而完整的个股研究报告(控制在约 800 字以内),"
                + "需包含:投资评级、目标价、DCF 每股内在价值、可比公司估值、盈利预测要点、情景与风险、"
                + "以及必要的合规披露。请自行完成全部估值计算与推导,直接给出结论数字。";
        return model.generate(new ReportModel.ModelTask("baseline", instruction, brief, 1600));
    }

    /** A compact raw-data digest — the inputs, not any computed conclusion. */
    private static String digest(CompanyData.Dataset ds) {
        StringBuilder sb = new StringBuilder();
        if (ds.hasFundamentals()) {
            CompanyData.Fundamentals f = ds.fundamentals();
            sb.append("公司: ").append(f.company()).append(" (").append(f.ticker()).append("),币种 ").append(f.currency()).append('\n');
            sb.append("营收: ").append(Bb.fmt(f.revenue())).append("、EBITDA: ").append(Bb.fmt(f.ebitda()))
                    .append("、EPS: ").append(Bb.fmt(f.eps())).append("、基期自由现金流: ").append(Bb.fmt(f.fcfBase())).append('\n');
            sb.append("净负债: ").append(Bb.fmt(f.netDebt())).append("、少数股东权益: ").append(Bb.fmt(f.minorityInterest()))
                    .append("、稀释股本: ").append(Bb.fmt(f.dilutedShares())).append('\n');
            sb.append("增量利润率: ").append(Bb.fmt(f.incrementalMargin())).append("、税率: ").append(Bb.fmt(f.taxRate())).append('\n');
            if (!f.revenueHistory().isEmpty()) {
                sb.append("历史营收: ").append(f.revenueHistory()).append('\n');
            }
        }
        if (ds.hasMarket()) {
            sb.append("现价: ").append(Bb.fmt(ds.market().price()))
                    .append("、52周区间: ").append(Bb.fmt(ds.market().low52w())).append("~").append(Bb.fmt(ds.market().high52w())).append('\n');
        }
        if (ds.hasConsensus()) {
            CompanyData.Consensus c = ds.consensus();
            sb.append("一致预期 EPS: ").append(Bb.fmt(c.epsMean())).append("、一致目标价: ").append(Bb.fmt(c.priceTargetMean()))
                    .append("、买/持/卖: ").append(c.buys()).append('/').append(c.holds()).append('/').append(c.sells()).append('\n');
        }
        if (ds.hasPeers()) {
            CompanyData.PeerSet p = ds.peers();
            sb.append("可比倍数(中位): EV/EBITDA ").append(Bb.fmt(p.evEbitda()))
                    .append("、EV/Sales ").append(Bb.fmt(p.evSales())).append("、P/E ").append(Bb.fmt(p.priceEarnings())).append('\n');
        }
        for (CompanyData.MacroIndicator m : ds.macro()) {
            sb.append("宏观: ").append(m.name()).append(' ').append(Bb.fmt(m.value())).append(m.unit()).append('\n');
        }
        for (CompanyData.TextItem n : ds.news()) {
            sb.append("资讯: ").append(n.title()).append(" — ").append(n.body()).append('\n');
        }
        return sb.toString();
    }
}
