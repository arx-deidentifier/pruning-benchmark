/*     
 * Copyright (C) 2015 Fabian Prasser, Raffael Bild, Johanna Eicher, Helmut Spengler, Florian Kohlmayer
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

package org.deidentifier.arx;

import java.io.IOException;
import java.util.HashMap;

import org.deidentifier.arx.algorithm.AbstractAlgorithm;
import org.deidentifier.arx.algorithm.AlgorithmMinimal;
import org.deidentifier.arx.algorithm.IGreedyMetric;
import org.deidentifier.arx.framework.check.NodeChecker;
import org.deidentifier.arx.framework.check.distribution.DistributionAggregateFunction;
import org.deidentifier.arx.framework.data.DataManager;
import org.deidentifier.arx.framework.data.Dictionary;
import org.deidentifier.arx.framework.lattice.SolutionSpace;
import org.deidentifier.arx.metric.Metric;
import org.deidentifier.arx.metric.Metric.AggregateFunction;

/**
 * Creates a benchmarking environment consisting of a solution space, 
 * node checked, data manager etc. Furthermore, initializes all configuration files
 * 
 * @author Fabian Prasser
 */
public class BenchmarkEnvironment {

    /** History size. */
    private static int    CONST_HISTORY_SIZE    = 200;

    /** Snapshot size. */
    private static double CONST_SNAPSHOT_SIZE_1 = 0.2d;

    /** Snapshot size snapshot. */
    private static double CONST_SNAPSHOT_SIZE_2 = 0.8d;

    /**
     * Internal method
     * 
     * @param algorithm
     * @param dataset
     * @param measure
     * @param criterion
     * @param timeLimit
     * @param suppressionLimit
     * @return
     * @throws IOException
     */
    public static double getResult(Data data,
                                   ARXConfiguration config) throws IOException {

        // Create environment
        BenchmarkEnvironment environment = new BenchmarkEnvironment(data, config);

        // Create an algorithm instance
        AbstractAlgorithm implementation = new AlgorithmMinimal(environment.solutions,
                                                                environment.checker);
        config.setMetric(new IGreedyMetric());

        // Execute
        implementation.traverse();

        // Convert results
        int[] optimum = implementation.getGlobalOptimum().getGeneralization();
        return getInformationLoss(data, config, optimum);
    }

    
    /**
     * Returns the information loss for the given transformation
     * @param data
     * @param config
     * @param transformation
     * @return
     * @throws IOException 
     */
    private static double getInformationLoss(Data data,
                                             ARXConfiguration config,
                                             int[] transformation) throws IOException {

        config.setMetric(Metric.createLossMetric(AggregateFunction.GEOMETRIC_MEAN));
        BenchmarkEnvironment environment = new BenchmarkEnvironment(data, config);
        return Double.valueOf(environment.checker.check(environment.solutions.getTransformation(transformation)).informationLoss.toString());
    }

    /** Variable*/
    private final SolutionSpace solutions;

    /** Variable*/
    private final NodeChecker checker;

    /** Variable*/
    private final DataManager manager;

    /**
     * Creates a new instance
     * @param data
     * @param config
     * @throws IOException
     */
    private BenchmarkEnvironment(Data data, ARXConfiguration config) throws IOException {

        // Initialize
        DataHandle handle = data.getHandle();
        handle.getDefinition().materializeHierarchies(handle);
        handle.getRegistry().reset();
        handle.getRegistry().createInputSubset(config);
        DataDefinition definition = handle.getDefinition();

        // Encode
        String[] header = ((DataHandleInput) handle).header;
        int[][] dataArray = ((DataHandleInput) handle).data;
        Dictionary dictionary = ((DataHandleInput) handle).dictionary;
        manager = new DataManager(header,
                                              dataArray,
                                              dictionary,
                                              definition,
                                              config.getCriteria(),
                                              new HashMap<String, DistributionAggregateFunction>());

        // Attach arrays to data handle
        ((DataHandleInput) handle).update(manager.getDataGeneralized().getArray(),
                                          manager.getDataAnalyzed().getArray(),
                                          manager.getDataStatic().getArray());

        // Initialize
        config.initialize(manager);

        // Build or clean the lattice
        solutions = new SolutionSpace(manager.getHierarchiesMinLevels(),
                                                        manager.getHierarchiesMaxLevels());

        // Build a node checker
        checker = new NodeChecker(manager,
                                              config.getMetric(),
                                              config.getInternalConfiguration(),
                                              CONST_HISTORY_SIZE,
                                              CONST_SNAPSHOT_SIZE_1,
                                              CONST_SNAPSHOT_SIZE_2,
                                              solutions);

        // Initialize the metric
        config.getMetric().initialize(manager, definition,
                                      manager.getDataGeneralized(),
                                      manager.getHierarchies(),
                                      config);
    }
}
