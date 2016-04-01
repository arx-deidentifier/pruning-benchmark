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

import de.linearbits.subframe.Benchmark;
import de.linearbits.subframe.analyzer.ValueBuffer;

/**
 * Main benchmark class.
 * 
 * @author Fabian Prasser
 */
public class BenchmarkExperiment {


    /** The benchmark instance */
    private static final Benchmark BENCHMARK   = new Benchmark(new String[] { "Dataset", "Utility", "Privacy" });

    /** QIS */
    public static final int SPACE = BENCHMARK.addMeasure("Space");

    /** SEARCH_SPACE */
    public static final int NODES = BENCHMARK.addMeasure("Nodes");


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
        BENCHMARK.addAnalyzer(SPACE, new ValueBuffer());
        BENCHMARK.addAnalyzer(NODES, new ValueBuffer());
        
        // Repeat for each data set
        for (BenchmarkDataset data : BenchmarkSetup.getDatasets()) {
            for (BenchmarkPrivacyModel criterion : getCriteria()) {
                for (BenchmarkUtilityMeasure measure : BenchmarkSetup.getUtilityMeasures()) {
                    System.out.println(data.toString() + measure.toString() + criterion.toString());
                    BENCHMARK.addRun(data.toString(), measure.toString(), criterion.toString());
                    anonymize(data, measure, criterion);
                    BENCHMARK.getResults().write(new File("results/results-low-memory-3.2.csv"));
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
    private static void anonymize(BenchmarkDataset dataset, BenchmarkUtilityMeasure measure, BenchmarkPrivacyModel criterion) throws IOException {
        
        // Get memory baseline
        Data data = BenchmarkSetup.getData(dataset, criterion);
        ARXConfiguration config = BenchmarkSetup.getConfiguration(measure, criterion, 0.01d);
        ARXAnonymizer anonymizer = new ARXAnonymizer();
        anonymizer.setMeasureMemory(true);
        anonymizer.anonymize(data, config);
        long memory = anonymizer.getMemory();
        
        int nodes = 1;
        for (String qi : data.getDefinition().getQuasiIdentifyingAttributes()) {
            nodes *= data.getDefinition().getHierarchy(qi)[0].length;
        }
        BENCHMARK.addValue(SPACE, memory);
        BENCHMARK.addValue(NODES, nodes);
    }
}
