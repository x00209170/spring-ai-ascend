package com.bank.financial.research.engine;

@FunctionalInterface
public interface PipelineProgress {
    void onAgent(String role, String state, int index, int total); // state = "running" | "done"
    PipelineProgress NOOP = (role, state, index, total) -> { };
}
