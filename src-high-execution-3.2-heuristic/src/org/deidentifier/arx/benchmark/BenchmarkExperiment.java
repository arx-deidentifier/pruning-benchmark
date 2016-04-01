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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.deidentifier.arx.ARXAnonymizer;
import org.deidentifier.arx.ARXConfiguration;
import org.deidentifier.arx.Data;
import org.deidentifier.arx.algorithm.LIGHTNINGAlgorithm;
import org.deidentifier.arx.algorithm.LIGHTNINGAlgorithm.LIGHTNINGAlgorithmListener;
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

    /** The benchmark instance */
    private static final Benchmark BENCHMARK      = new Benchmark(new String[] { "Attributes",
            "Dataset"                            });

    /** Time */
    public static final int        TIME           = BENCHMARK.addMeasure("Time");

    /** Utility */
    public static final int        QUALITY        = BENCHMARK.addMeasure("Quality");

    /** Utility */
    public static final int        TRANSFORMATION = BENCHMARK.addMeasure("Transformation");

    /** Utility */
    public static final int        CHECKS         = BENCHMARK.addMeasure("Checks");

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
        BENCHMARK.addAnalyzer(TIME, new ValueBuffer());
        BENCHMARK.addAnalyzer(QUALITY, new ValueBuffer());
        BENCHMARK.addAnalyzer(TRANSFORMATION, new ValueBuffer());
        BENCHMARK.addAnalyzer(CHECKS, new ValueBuffer());
        
        // Repeat for each data set
        for (BenchmarkDataset dataset : getDatasets()) {
            String[] qis = BenchmarkSetup.getQuasiIdentifyingAttributes(dataset);
            for (int dimensions = 1; dimensions <= qis.length; dimensions++) {
                for (BenchmarkPrivacyModel criterion : getCriteria()) {
                    for (BenchmarkUtilityMeasure measure : BenchmarkSetup.getUtilityMeasures()) {
                        System.out.println(dimensions + "/" + measure.toString() + "/" + criterion.toString());
                        anonymize(BenchmarkSetup.getData(dataset, Arrays.copyOf(qis, dimensions)), measure, criterion, dataset.toString());
                        BENCHMARK.getResults().write(new File("results/results-high-execution-3.2-heuristic.csv"));
                    }
                }
            }
        }

        // Repeat for each data set
        for (int i = 3 ; i <= 15; i++) {
            for (BenchmarkPrivacyModel criterion : getCriteria()) {
                for (BenchmarkUtilityMeasure measure : BenchmarkSetup.getUtilityMeasures()) {
                    System.out.println(i + measure.toString() + criterion.toString());
                    anonymize(BenchmarkSetup.getHighdimensionalData(i), measure, criterion, "SS13ACS");
                    BENCHMARK.getResults().write(new File("results/results-high-execution-3.2-heuristic.csv"));
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
        
        // Prepare
        class Entry {
            public long time;
            public double quality;
            public String transformation;
            public int checks;
            Entry(long time, double utility, String transformation, int checks) {
                this.time = time;
                this.quality = utility;
                this.transformation = transformation;
                this.checks = checks;
            }
        }
        final List<Entry> entries = new ArrayList<>();
        
        // Configure
        ARXConfiguration config = BenchmarkSetup.getConfiguration(measure, criterion, 0.01d);
        config.setHeuristicSearchEnabled(true);
        config.setHeuristicSearchThreshold(1);
        config.setHeuristicSearchTimeLimit(Integer.MAX_VALUE);

        // Anonymize
        LIGHTNINGAlgorithm.setListener(new LIGHTNINGAlgorithmListener() {
            @Override
            public void update(long time, double quality, String transformation, int checks) {
                entries.add(new Entry(time, quality, transformation, checks));
            }
        });
        ARXAnonymizer anonymizer = new ARXAnonymizer();
        anonymizer.anonymize(data, config);
                
        for (Entry entry : entries) {
            BENCHMARK.addRun(data.getDefinition().getQuasiIdentifyingAttributes().size(), dataset);
            BENCHMARK.addValue(TIME, entry.time);
            BENCHMARK.addValue(QUALITY, entry.quality);
            BENCHMARK.addValue(TRANSFORMATION, entry.transformation);
            BENCHMARK.addValue(CHECKS, entry.checks);
        }
    }
}
