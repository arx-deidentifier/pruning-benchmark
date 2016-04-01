/*
 * ARX: Powerful Data Anonymization
 * Copyright 2012 - 2015 Florian Kohlmayer, Fabian Prasser
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
 * 
 * @author Fabian Prasser
 */
public class LIGHTNINGAlgorithmTDS extends AbstractAlgorithm{

    /** Property */
    private final PredictiveProperty propertyInsufficientPrivacy;
    /** Property */
    private final PredictiveProperty propertyInsufficientUtility;
    /** Property */
    private final PredictiveProperty propertyExpanded;
    /** Property */
    private final PredictiveProperty propertyChecked;
    /** Time limit */
    private final int                timeLimit;
    /** The start time */
    private long                     timeStart;

    /**
    * Constructor
    * @param space
    * @param checker
    * @param timeLimit
    */
    private LIGHTNINGAlgorithmTDS(SolutionSpace space, NodeChecker checker, int timeLimit) {
        super(space, checker);
        this.checker.getHistory().setStorageStrategy(StorageStrategy.ALL);
        this.propertyExpanded = space.getPropertyExpanded();
        this.propertyChecked = space.getPropertyChecked();
        this.propertyInsufficientUtility = space.getPropertyInsufficientUtility();
        this.propertyInsufficientPrivacy = space.getPropertyNotKAnonymous();
        this.solutionSpace.setAnonymityPropertyPredictable(false);
        this.timeLimit = timeLimit;
        if (timeLimit <= 0) { 
            throw new IllegalArgumentException("Invalid time limit. Must be greater than zero."); 
        }
        
        // TODO: ADD BEST-FIRST
        // TODO: ADD BEST-FIRST
        // TODO: ADD BEST-FIRST
        // TODO: ADD BEST-FIRST
    }
    
    /**
    * Check the given transformation
    * @param transformation
    */
    private void check(final Transformation transformation) {
        
        // Check, if already checked
        if (!transformation.hasProperty(propertyChecked)) {
            
            // Check
            transformation.setChecked(checker.check(transformation, true));
            
            // Track
            progress((double)(System.currentTimeMillis() - timeStart) / (double)timeLimit);
            trackOptimum(transformation);
            
            // Insufficient utility
            if (getGlobalOptimum() != null && transformation.getLowerBound() != null) {
                if (transformation.getLowerBound().compareTo(getGlobalOptimum().getInformationLoss()) >= 0) {
                    transformation.setProperty(propertyInsufficientUtility);
                }
            }
        }
    }
    
    @Override
    public void traverse() {
        timeStart = System.currentTimeMillis();
        tds(solutionSpace.getTop());    
    }
    
    /**
     * TDS
     * @param transformation
     */
    private void tds(Transformation transformation) {
        
        // Exclude
        if (!canPrune(transformation) && !canExclude(transformation)) {
            check(transformation);
        }
        
        // Prune
        if (canPrune(transformation)) {
            return;
        }
        
        // Check time limit
        if (getTime() > timeLimit) {
            return;
        }
        
        // Check all predecessors
        LongArrayList list = transformation.getPredecessors();
        for (int i = 0; i < list.size(); i++) {
            
            // Recursive call
            tds(solutionSpace.getTransformation(list.getQuick(i)));
            
            // Check time limit
            if (getTime() > timeLimit) {
                return;
            }
        }
        
        // Mark as expanded
        transformation.setProperty(propertyExpanded);
    }
    
    /**
     * Returns the current execution time
     * @return
     */
    private int getTime() {
        return (int)(System.currentTimeMillis() - timeStart);
    }

    /**
     * Returns whether we can prune this transformation and its predecessors
     * @param transformation
     * @return
     */
     private boolean canPrune(Transformation transformation) {
         return transformation.hasProperty(propertyInsufficientPrivacy) ||
                transformation.hasProperty(propertyExpanded);
     }
         
    /**
     * Returns whether we can exclude this transformation
     * @param transformation
     * @return
     */
     private boolean canExclude(Transformation transformation) {
         return transformation.hasProperty(propertyInsufficientUtility);
     }

    /**
     * Creates a new instance
     * @param solutionSpace
     * @param checker
     * @param timeLimit
     * @return
     */
    public static AbstractAlgorithm create(SolutionSpace solutionSpace,
                                           NodeChecker checker,
                                           int timeLimit) {
        return new LIGHTNINGAlgorithmTDS(solutionSpace, checker, timeLimit);
    }
}
