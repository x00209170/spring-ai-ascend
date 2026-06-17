package com.bank.financial.research.eval;

import java.util.List;

/**
 * A side-by-side quality scorecard: the multi-agent engine vs a single-model
 * one-shot baseline, on the dimensions a bank is actually accountable for. The
 * point is not prose fluency (both can be fluent) but verifiability,
 * reproducibility, auditability and process control — the properties that decide
 * whether a sell-side report is defensible.
 *
 * <p>Rows come in two kinds. {@link Kind#GUARANTEE} rows are architectural facts
 * true by construction (the engine has the mechanism; a single prompt does not).
 * {@link Kind#MEASURED} rows are computed from this run's two actual outputs, so
 * the difference is demonstrated, not asserted.
 */
public record MethodScorecard(List<Row> rows) {

    public MethodScorecard {
        rows = List.copyOf(rows);
    }

    public enum Kind { GUARANTEE, MEASURED }

    /**
     * One comparison line.
     *
     * @param dimension   what is being compared (Chinese label)
     * @param engine      the engine's value / verdict (display string)
     * @param single      the single-model value / verdict (display string)
     * @param enginePass  whether the engine meets the bar on this dimension
     * @param singlePass  whether the single model meets it
     * @param why         one-line explanation of why this matters / what was measured
     */
    public record Row(Kind kind, String dimension, String engine, String single,
            boolean enginePass, boolean singlePass, String why) {
    }
}
