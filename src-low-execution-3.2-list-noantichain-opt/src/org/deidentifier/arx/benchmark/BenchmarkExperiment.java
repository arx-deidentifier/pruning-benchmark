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
import org.deidentifier.arx.benchmark.BenchmarkSetup.BenchmarkDataset;
import org.deidentifier.arx.benchmark.BenchmarkSetup.BenchmarkPrivacyModel;
import org.deidentifier.arx.benchmark.BenchmarkSetup.BenchmarkUtilityMeasure;
import org.deidentifier.arx.framework.check.NodeChecker;

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
    private static final Benchmark BENCHMARK   = new Benchmark(new String[] { "Dataset", "Utility", "Privacy" });

    /** TIME */
    public static final int        TIME_OPTIMAL        = BENCHMARK.addMeasure("TimeOptimal");

    /** TIME */
    public static final int        TIME_HEURISTIC        = BENCHMARK.addMeasure("TimeHeuristic");

    /** TIME */
    public static final int        TIME_CHECK        = BENCHMARK.addMeasure("TimeCheck");

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
        BENCHMARK.addAnalyzer(TIME_CHECK, new ValueBuffer());
        
        // Repeat for each data set
        for (BenchmarkDataset data : BenchmarkSetup.getDatasets()) {
            for (BenchmarkPrivacyModel criterion : getCriteria()) {
                for (BenchmarkUtilityMeasure measure : BenchmarkSetup.getUtilityMeasures()) {
                    System.out.println(data.toString() + measure.toString() + criterion.toString());
                    BENCHMARK.addRun(data.toString(), measure.toString(), criterion.toString());
                    anonymize(BenchmarkSetup.getData(data, criterion), measure, criterion);
                    BENCHMARK.getResults().write(new File("results/results-low-execution-3.2-list-noantichain-opt.csv"));
                }
            }
        }
        
        // Repeat for each data set
        for (int qi : new int[]{10}) {
            for (BenchmarkPrivacyModel criterion : getCriteria()) {
                for (BenchmarkUtilityMeasure measure : BenchmarkSetup.getUtilityMeasures()) {
                    System.out.println("SACS" + measure.toString() + criterion.toString());
                    BENCHMARK.addRun("SACS", measure.toString(), criterion.toString());
                    anonymize(BenchmarkSetup.getHighdimensionalData(qi), measure, criterion);
                    BENCHMARK.getResults().write(new File("results/results-low-execution-3.2-list-noantichain-opt.csv"));
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
        
        ARXAnonymizer anonymizer = new ARXAnonymizer();
        anonymizer.anonymize(data, config);
        
        NodeChecker.TIME = 0;
        long timeOptimal = 0;
        for (int i=0; i<REPETITIONS; i++) {
            data.getHandle().release();
            anonymizer.anonymize(data, config);
            timeOptimal += anonymizer.getTime();
        }
        timeOptimal = timeOptimal / REPETITIONS;
        long timeCheck = NodeChecker.TIME / REPETITIONS;
        
        BENCHMARK.addValue(TIME_OPTIMAL, timeOptimal);
        BENCHMARK.addValue(TIME_HEURISTIC, 0);
        BENCHMARK.addValue(TIME_CHECK, timeCheck);
    }
}
