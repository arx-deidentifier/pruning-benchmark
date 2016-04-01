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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.deidentifier.arx.ARXAnonymizer;
import org.deidentifier.arx.ARXConfiguration;
import org.deidentifier.arx.BenchmarkEnvironment;
import org.deidentifier.arx.BenchmarkSetup;
import org.deidentifier.arx.BenchmarkSetup.BenchmarkDataset;
import org.deidentifier.arx.BenchmarkSetup.BenchmarkPrivacyModel;
import org.deidentifier.arx.BenchmarkSetup.BenchmarkUtilityMeasure;
import org.deidentifier.arx.Data;
import org.deidentifier.arx.algorithm.LIGHTNINGAlgorithm;
import org.deidentifier.arx.algorithm.LIGHTNINGAlgorithm.LIGHTNINGAlgorithmListener;

import de.linearbits.subframe.Benchmark;
import de.linearbits.subframe.analyzer.ValueBuffer;

/**
 * Main benchmark class.
 * 
 * @author Fabian Prasser
 */
public class BenchmarkExperiment {

    /** The benchmark instance */
    private static final Benchmark BENCHMARK   = new Benchmark(new String[] { "Attributes", "Dataset"});

    /** REPETITIONS */
    private static final int       REPETITIONS = 5;

    /** LIMIT */
    public static final int        LIMIT       = BENCHMARK.addMeasure("Limit");

    /** IMPROVEMENT */
    public static final int        IMPROVEMENT = BENCHMARK.addMeasure("Improvement");

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
     * Returns all datasets relevant for this benchmark
     * 
     * @return
     */
    private static BenchmarkDataset[] getDatasets() {
        return new BenchmarkDataset[] { 
                BenchmarkDataset.ADULT,
                BenchmarkDataset.CUP,
                BenchmarkDataset.FARS,
                BenchmarkDataset.ATUS,
                BenchmarkDataset.IHIS, };
    }
    
    /**
     * Main entry point
     * 
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {

        // Init
        BENCHMARK.addAnalyzer(LIMIT, new ValueBuffer());
        BENCHMARK.addAnalyzer(IMPROVEMENT, new ValueBuffer());
        
        // Repeat for each data set
        for (BenchmarkDataset dataset : getDatasets()) {
            String[] qis = BenchmarkSetup.getQuasiIdentifyingAttributes(dataset);
            for (int dimensions = 3; dimensions <= qis.length; dimensions++) {
                for (BenchmarkPrivacyModel criterion : getCriteria()) {
                    for (BenchmarkUtilityMeasure measure : BenchmarkSetup.getUtilityMeasures()) {
                        System.out.println(dimensions + "/" + measure.toString() + "/" + criterion.toString());
                        anonymize(BenchmarkSetup.getData(dataset, Arrays.copyOf(qis, dimensions)), measure, criterion, dataset.toString());
                        BENCHMARK.getResults().write(new File("results/results-high-execution-3.2-heuristic-igreedy.csv"));
                    }
                }
            }
        }

        // Repeat for each data set
        for (int i = 3 ; i <= 15; i++) {
            for (BenchmarkPrivacyModel criterion : getCriteria()) {
                for (BenchmarkUtilityMeasure measure : BenchmarkSetup.getUtilityMeasures()) {
                    System.out.println(i + "/" + measure.toString() + criterion.toString());
                    anonymize(BenchmarkSetup.getHighdimensionalData(i), measure, criterion, "SS13ACS");
                    BENCHMARK.getResults().write(new File("results/results-high-execution-3.2-heuristic-igreedy.csv"));
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
    private static void anonymize(Data data, BenchmarkUtilityMeasure measure, BenchmarkPrivacyModel criterion, String dataset) throws IOException {
        
        BENCHMARK.addRun(data.getDefinition().getQuasiIdentifyingAttributes().size(), dataset);
        
        double igreedy = 0;
        double limit = Double.MAX_VALUE;
        for (int i=0; i<REPETITIONS; i++) {
            long time = System.currentTimeMillis();
            igreedy = 1d - BenchmarkEnvironment.getResult(data, BenchmarkSetup.getConfiguration(measure,
                                                                                                criterion,
                                                                                                0.01d));
            time = System.currentTimeMillis() - time;
            limit = time != 0 ? Math.min(limit, time) : limit;
        }

        // Configure
        double lightning = 0;
        for (int i=0; i<REPETITIONS; i++) {
            ARXConfiguration config = BenchmarkSetup.getConfiguration(measure, criterion, 0.01d);
            config.setHeuristicSearchEnabled(true);
            config.setHeuristicSearchThreshold(1);
            config.setHeuristicSearchTimeLimit((int)limit);
            
            final Set<Double> set = new HashSet<Double>();
            LIGHTNINGAlgorithm.setListener(new LIGHTNINGAlgorithmListener() {
                @Override
                public void update(long time, double quality, String transformation, int checks) {
                    set.clear();
                    set.add(quality);
                }
            });
            ARXAnonymizer anonymizer = new ARXAnonymizer();
            anonymizer.anonymize(data, config);
            if (!set.isEmpty()) {
                lightning = Math.max(lightning, set.iterator().next());
            }
        }
        
        BENCHMARK.addValue(LIMIT, (int)limit);
        BENCHMARK.addValue(IMPROVEMENT, lightning != 0d ? (lightning - igreedy) / igreedy : "--");
    }
}
