package org.deidentifier.arx.benchmark;

import java.io.IOException;
import java.text.ParseException;

public class BenchmarkAnalysisAll {

    public static void main(String[] args) throws IOException, ParseException {
        BenchmarkAnalysisExecutionHighHeuristic.main(args);
        BenchmarkAnalysisExecutionHighOptimal.main(args);
        BenchmarkAnalysisExecutionLow.main(args);
        BenchmarkAnalysisMemoryHigh.main(args);
        BenchmarkAnalysisMemoryLow.main(args);
    }
}
