package com.bank.financial.research.eval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bank.financial.research.ResearchReports;
import com.bank.financial.research.engine.ReportRequest;
import com.bank.financial.research.engine.ResearchReport;
import com.bank.financial.research.eval.MethodScorecard.Kind;
import com.bank.financial.research.eval.MethodScorecard.Row;
import java.util.OptionalDouble;
import org.junit.jupiter.api.Test;

/** The method-comparison harness: engine wins the accountability dimensions, deterministically. */
class MethodComparisonTest {

    private static final long AS_OF = 1_750_000_000_000L;

    private ResearchReport engineReport() {
        return ResearchReports.offline(AS_OF).generate(ReportRequest.equity("DEMO", "t-test", AS_OF));
    }

    @Test
    void architecturalGuaranteesFavourEngine() {
        MethodScorecard card = MethodComparison.compare(engineReport(), "随便一段没有数字的研报草稿。");
        long guarantees = card.rows().stream().filter(r -> r.kind() == Kind.GUARANTEE).count();
        assertTrue(guarantees >= 7, "expected the full set of architectural guarantees");
        // Every guarantee row: the engine meets the bar, the single model does not.
        card.rows().stream().filter(r -> r.kind() == Kind.GUARANTEE).forEach(r -> {
            assertTrue(r.enginePass(), "engine should pass guarantee: " + r.dimension());
            assertFalse(r.singlePass(), "single model should fail guarantee: " + r.dimension());
        });
    }

    @Test
    void engineClearsTheNumericConsistencyGate() {
        MethodScorecard card = MethodComparison.compare(engineReport(), "草稿");
        Row gate = card.rows().stream().filter(r -> r.dimension().contains("数值一致性门")).findFirst().orElseThrow();
        assertTrue(gate.enginePass(), "engine clears the consistency gate (0 drift)");
        assertFalse(gate.singlePass(), "single model has no such gate");
    }

    @Test
    void disclosureCompletenessIsMeasured() {
        // The engine report carries the full disclosure set; a bare baseline carries none.
        MethodScorecard card = MethodComparison.compare(engineReport(), "目标价 88.8 元,建议买入。");
        Row disc = card.rows().stream().filter(r -> r.dimension().contains("合规披露")).findFirst().orElseThrow();
        assertTrue(disc.enginePass(), "engine has all required disclosures: " + disc.engine());
        assertFalse(disc.singlePass(), "bare baseline is missing disclosures: " + disc.single());
    }

    @Test
    void parsesTheSingleModelStatedTarget() {
        assertEquals(88.8, MethodComparison.statedTarget("我们给予目标价 88.8 元。").getAsDouble(), 1e-9);
        assertEquals(OptionalDouble.empty(), MethodComparison.statedTarget("没有给出任何目标价数字。"));
    }
}
