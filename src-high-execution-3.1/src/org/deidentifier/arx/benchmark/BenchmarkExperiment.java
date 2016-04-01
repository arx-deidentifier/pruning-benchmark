/*
 * Benchmark of risk-based anonymization in ARX 3.0.0
 * Copyright 2015 - Fabian Prasser
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.deidentifier.arx.benchmark;

import java.io.File;
import java.io.IOException;

import org.deidentifier.arx.ARXAnonymizer;
import org.deidentifier.arx.ARXConfiguration;
import org.deidentifier.arx.Data;
import org.deidentifier.arx.benchmark.BenchmarkSetup;
import org.deidentifier.arx.benchmark.BenchmarkSetup.BenchmarkDataset;
import org.deidentifier.arx.benchmark.BenchmarkSetup.BenchmarkPrivacyModel;
import org.deidentifier.arx.benchmark.BenchmarkSetup.BenchmarkUtilityMeasure;

import de.linearbits.subframe.Benchmark;
import de.linearbits.subframe.analyzer.ValueBuffer;

/**
 * Main benchmark class.
 * 
 * @author Fabian Prasser
 */
public class BenchmarkExperiment {

    /** Repetitions */
    private static final int       REPETITIONS = 10;

    /** The benchmark instance */
    private static final Benchmark BENCHMARK   = new Benchmark(new String[] { "Attributes", "Utility", "Privacy" });

    /** TIME */
    public static final int        TIME_OPTIMAL        = BENCHMARK.addMeasure("TimeOptimal");

    /** TIME */
    public static final int        TIME_HEURISTIC        = BENCHMARK.addMeasure("TimeHeuristic");

    /**
     * Returns all criteria relevant for this benchmark
     * @return
     */
    public static BenchmarkPrivacyModel[] getCriteria() {
        return new BenchmarkPrivacyModel[] {
                BenchmarkPrivacyModel.K_ANONYMITY,
        };
    }
    
    /**
     * Main entry point
     * 
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {

        // Init
        BENCHMARK.addAnalyzer(TIME_OPTIMAL, new ValueBuffer());
        BENCHMARK.addAnalyzer(TIME_HEURISTIC, new ValueBuffer());
        
        // Repeat for each data set
        for (int i = 3 ; i <= 12; i++) {
            for (BenchmarkPrivacyModel criterion : getCriteria()) {
                for (BenchmarkUtilityMeasure measure : BenchmarkSetup.getUtilityMeasures()) {
                    System.out.println(i + measure.toString() + criterion.toString());
                    BENCHMARK.addRun(i, measure.toString(), criterion.toString());
                    anonymize(BenchmarkSetup.getHighdimensionalData(i), measure, criterion);
                    BENCHMARK.getResults().write(new File("results/results-high-execution-3.1.csv"));
                }
            }
        }
    }
    
    /**
     * Performs the experiments
     * 
     * @param dataset
     * @param measure 
     * @throws IOException
     */
    private static void anonymize(Data data, BenchmarkUtilityMeasure measure, BenchmarkPrivacyModel criterion) throws IOException {
        
        // Get memory baseline
        ARXConfiguration config = BenchmarkSetup.getConfiguration(measure, criterion, 0.01d);
        ARXAnonymizer anonymizer = new ARXAnonymizer();
        anonymizer.setMaxTransformations(Integer.MAX_VALUE);
        
        data.getHandle().release();
        anonymizer.anonymize(data, config);
        
        long time = 0;
        for (int i=0; i<REPETITIONS; i++) {
            data.getHandle().release();
            anonymizer.anonymize(data, config);
            time += anonymizer.getTime();
        }
        time = time / REPETITIONS;
        BENCHMARK.addValue(TIME_OPTIMAL, time);
        BENCHMARK.addValue(TIME_HEURISTIC, 0);
    }
}
