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
    private static final int       REPETITIONS    = 10;

    /** The benchmark instance */
    private static final Benchmark BENCHMARK      = new Benchmark(new String[] { "Attributes",
            "Utility",
            "Privacy"                            });

    /** TIME */
    public static final int        TIME_OPTIMAL   = BENCHMARK.addMeasure("TimeOptimal");

    /** TIME */
    public static final int        TIME_HEURISTIC = BENCHMARK.addMeasure("TimeHeuristic");

    /** NODES */
    public static final int        NODES = BENCHMARK.addMeasure("Transformations");

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
        BENCHMARK.addAnalyzer(NODES, new ValueBuffer());
        
        // Repeat for each data set
        for (int i = 3 ; i <= 15; i++) {
            for (BenchmarkPrivacyModel criterion : getCriteria()) {
                for (BenchmarkUtilityMeasure measure : BenchmarkSetup.getUtilityMeasures()) {
                    System.out.println(i + measure.toString() + criterion.toString());
                    BENCHMARK.addRun(i, measure.toString(), criterion.toString());
                    anonymize(BenchmarkSetup.getHighdimensionalData(i), measure, criterion);
                    BENCHMARK.getResults().write(new File("results/results-high-execution-3.2.csv"));
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
        
        config.setHeuristicSearchEnabled(false);
        config.setHeuristicSearchThreshold(Integer.MAX_VALUE);
        config.setHeuristicSearchTimeLimit(Integer.MAX_VALUE);
        
        ARXAnonymizer anonymizer = null;
        long timeOptimal = System.currentTimeMillis();
        if (data.getDefinition().getQuasiIdentifyingAttributes().size() <= 14) {
            
            anonymizer = new ARXAnonymizer();
            
            anonymizer.anonymize(data, config);
            
            timeOptimal = 0;
            for (int i=0; i<REPETITIONS; i++) {
                data.getHandle().release();
                anonymizer.anonymize(data, config);
                timeOptimal += anonymizer.getTime();
            }
            timeOptimal = timeOptimal / REPETITIONS;
        } else {
            timeOptimal = 0;
        }
            
        config.setHeuristicSearchEnabled(true);
        config.setHeuristicSearchThreshold(1);
        config.setHeuristicSearchTimeLimit(Integer.MAX_VALUE);

        anonymizer = new ARXAnonymizer();
        
        data.getHandle().release();
        anonymizer.anonymize(data, config);
        
        long timeHeuristic = 0;
        for (int i=0; i<REPETITIONS; i++) {
            data.getHandle().release();
            anonymizer.anonymize(data, config);
            timeHeuristic += anonymizer.getTime();
        }
        timeHeuristic = timeHeuristic / REPETITIONS;

        long nodes = 1;
        for (String qi : data.getDefinition().getQuasiIdentifyingAttributes()) {
            nodes *= data.getDefinition().getHierarchy(qi)[0].length;
        }
        
        BENCHMARK.addValue(NODES, nodes);
        BENCHMARK.addValue(TIME_OPTIMAL, timeOptimal);
        BENCHMARK.addValue(TIME_HEURISTIC, timeHeuristic);
    }
}
