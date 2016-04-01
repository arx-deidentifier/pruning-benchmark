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
import java.util.Map;
import java.util.Set;

import org.deidentifier.arx.ARXAnonymizer;
import org.deidentifier.arx.ARXConfiguration;
import org.deidentifier.arx.Data;
import org.deidentifier.arx.benchmark.BenchmarkSetup.BenchmarkPrivacyModel;
import org.deidentifier.arx.benchmark.BenchmarkSetup.BenchmarkUtilityMeasure;

import de.linearbits.jhpl.Lattice;
import de.linearbits.subframe.Benchmark;
import de.linearbits.subframe.analyzer.ValueBuffer;

/**
 * Main benchmark class.
 * 
 * @author Fabian Prasser
 */
public class BenchmarkExperiment {
        
    /** The benchmark instance */
    private static final Benchmark BENCHMARK = new Benchmark(new String[] { "Attributes", "Utility", "Privacy", "Nodes", "Property" });
    
    private static final int PUTPROPERTY  = BENCHMARK.addMeasure("PutProperty");
    private static final int HASPROTPERTY = BENCHMARK.addMeasure("HasProperty");
    private static final int ANTICHAIN    = BENCHMARK.addMeasure("Antichain");
    private static final int HITRATE      = BENCHMARK.addMeasure("Hitrate");
    
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
        BENCHMARK.addAnalyzer(PUTPROPERTY, new ValueBuffer());
        BENCHMARK.addAnalyzer(HASPROTPERTY, new ValueBuffer());
        BENCHMARK.addAnalyzer(ANTICHAIN, new ValueBuffer());
        BENCHMARK.addAnalyzer(HITRATE, new ValueBuffer());
        
        // Repeat for each data set
        for (int i = 3; i <= 15; i++) {
            for (BenchmarkPrivacyModel criterion : getCriteria()) {
                for (BenchmarkUtilityMeasure measure : BenchmarkSetup.getUtilityMeasures()) {
                    
                        System.out.println(i + measure.toString() + criterion.toString());
                        long nodes = anonymize(BenchmarkSetup.getHighdimensionalData(i), measure, criterion);
                        
                        Map<String, Integer> hitrate = ARXAnonymizer.hitrate;
                        Map<String, Integer> maxAntichain = ARXAnonymizer.maxAntichain;
                        Map<String, Integer> numHasProperty = ARXAnonymizer.numHasProperty;
                        Map<String, Integer> numPutProperty = ARXAnonymizer.numPutProperty;
                        
                        // Collect all properties
                        Set<String> properties = new HashSet<String>();
                        properties.addAll(hitrate.keySet());
                        properties.addAll(maxAntichain.keySet());
                        properties.addAll(numHasProperty.keySet());
                        properties.addAll(numPutProperty.keySet());
                        
                        // Sort properties
                        String[] propertiesArray = properties.toArray(new String[0]);
                        Arrays.sort(propertiesArray);
                        
                        // Add values
                        for (String property : propertiesArray) {
                            BENCHMARK.addRun(i, measure.toString(), criterion.toString(), nodes, property);
                            BENCHMARK.addValue(HITRATE, hitrate.get(property) == null || numHasProperty.get(property) == null ? 0d : (double) hitrate.get(property) / (double) numHasProperty.get(property));
                            BENCHMARK.addValue(ANTICHAIN, maxAntichain.get(property) == null || numPutProperty.get(property) == null ? 0d : (double) maxAntichain.get(property) / (double) numPutProperty.get(property));
                            BENCHMARK.addValue(HASPROTPERTY, numHasProperty.get(property) == null ? 0 : numHasProperty.get(property));
                            BENCHMARK.addValue(PUTPROPERTY, numPutProperty.get(property) == null ? 0 : numPutProperty.get(property));
                        }
                        
                        BENCHMARK.getResults().write(new File("results/results-high-jhpl-numbers-3.2.csv"));
                        
                        Lattice.hitrate.clear();
                        Lattice.maxAntichain.clear();
                        Lattice.numHasProperty.clear();
                        Lattice.numPutProperty.clear();
                    
                }
            }
        }
    }
    
    /**
     * Performs the experiments
     * 
     * @param dataset
     * @param measure
     * @param config2
     * @throws IOException
     */
    private static long anonymize(Data data, BenchmarkUtilityMeasure measure, BenchmarkPrivacyModel criterion) throws IOException {
        
        // Get memory baseline
        ARXConfiguration config = BenchmarkSetup.getConfiguration(measure, criterion, 0.01d);
        
        config.setHeuristicSearchEnabled(true);
        config.setHeuristicSearchThreshold(1);
        config.setHeuristicSearchTimeLimit(Integer.MAX_VALUE);
        
        ARXAnonymizer anonymizer = new ARXAnonymizer();
        anonymizer.anonymize(data, config);
        
        long nodes = 1;
        for (String qi : data.getDefinition().getQuasiIdentifyingAttributes()) {
            nodes *= data.getDefinition().getHierarchy(qi)[0].length;
        }
        return nodes;
        
    }
}
