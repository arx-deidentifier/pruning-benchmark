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

package org.deidentifier.arx.algorithm;

import org.deidentifier.arx.framework.check.NodeChecker;
import org.deidentifier.arx.framework.check.history.History.StorageStrategy;
import org.deidentifier.arx.framework.lattice.SolutionSpace;
import org.deidentifier.arx.framework.lattice.Transformation;

import cern.colt.list.LongArrayList;
import de.linearbits.jhpl.PredictiveProperty;

/**
 * A base class for algorithms that find a minimally anonymous solution. This class is used
 * as a basis for implementing IGreedy and DataFly, both algorithms are implemented as instances
 * of this class using different models for measuring information loss.
 * 
 * @author Fabian Prasser
 * @author Raffael Bild
 * @author Johanna Eicher
 * @author Helmut Spengler
 */
public class AlgorithmMinimal extends AbstractAlgorithm{
   
    /** Property */
    private final PredictiveProperty propertyChecked;
    
    /**
    * Constructor
    * @param space
    * @param checker
    */
    public AlgorithmMinimal(SolutionSpace space, NodeChecker checker) {
        super(space, checker);
        this.propertyChecked = space.getPropertyChecked();
        this.checker.getHistory().setStorageStrategy(StorageStrategy.ALL);
        this.solutionSpace.setAnonymityPropertyPredictable(false);
    }
    
    /**
    * Makes sure that the given Transformation has been checked
    * @param transformation
    */
    private void assureChecked(final Transformation transformation) {
        if (!transformation.hasProperty(propertyChecked)) {
            transformation.setChecked(checker.check(transformation, true));
            trackOptimum(transformation);
        }
    }

    /**
    * Performs a depth first search without backtracking
    * @param transformation
    */
    private void dfs(Transformation transformation) {
        assureChecked(transformation);
        if (getGlobalOptimum() != null) {
            return;
        }
        Transformation next = expand(transformation);
        if (getGlobalOptimum() != null) {
            return;
        }
        if (next != null) {
            dfs(next);
        }
    }
    
    /**
    * Returns the successor with minimal information loss, if any, null otherwise.
    * @param transformation
    * @return
    */
    private Transformation expand(Transformation transformation) {
        
        // Result
        Transformation result = null;

        // Find
        LongArrayList list = transformation.getSuccessors();
        for (int i=0; i<list.size(); i++) {
            long id = list.getQuick(i);
        
            Transformation successor = solutionSpace.getTransformation(id);
            if (!successor.hasProperty(propertyChecked)) {
                assureChecked(successor);
                if (result == null || successor.getInformationLoss().compareTo(result.getInformationLoss()) < 0) {
                    result = successor;
                }
            }
            if (getGlobalOptimum() != null) {
                return result;
            }
        }
        return result;
    }
    
    @Override
    public void traverse() {
        Transformation bottom = solutionSpace.getBottom();
        dfs(bottom);
    }
}
