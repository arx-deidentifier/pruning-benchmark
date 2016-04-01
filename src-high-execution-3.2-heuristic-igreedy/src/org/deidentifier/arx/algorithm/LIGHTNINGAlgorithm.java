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

import java.util.Comparator;
import java.util.PriorityQueue;

import org.deidentifier.arx.framework.check.NodeChecker;
import org.deidentifier.arx.framework.check.history.History.StorageStrategy;
import org.deidentifier.arx.framework.lattice.SolutionSpace;
import org.deidentifier.arx.framework.lattice.Transformation;

import cern.colt.Arrays;
import cern.colt.list.LongArrayList;
import de.linearbits.jhpl.PredictiveProperty;

/**
 * 
 * @author Fabian Prasser
 * @author Raffael Bild
 * @author Johanna Eicher
 * @author Helmut Spengler
 */
public class LIGHTNINGAlgorithm extends AbstractAlgorithm{
    
    public static interface LIGHTNINGAlgorithmListener {
        public void update(long time, double quality, String transformation, int checks);
    }
    
    public static final void setListener(LIGHTNINGAlgorithmListener listener) {
        LIGHTNINGAlgorithm.listener = listener;
    }
    
    private static LIGHTNINGAlgorithmListener listener;

    /** Property */
    private final PredictiveProperty insufficientUtility;
    /** Property */
    private final PredictiveProperty propertyChecked;
    /** Property */
    private final PredictiveProperty propertyExpanded;
    /** The number indicating how often a depth-first-search will be performed */
    private final int                stepping;
    /** Time limit */
    private final int                timeLimit;
    /** The start time */
    private long                     timeStart;
    
    private int checks = 0;

    /**
    * Constructor
    * @param space
    * @param checker
    * @param timeLimit
    */
    private LIGHTNINGAlgorithm(SolutionSpace space, NodeChecker checker, int timeLimit) {
        super(space, checker);
        this.checker.getHistory().setStorageStrategy(StorageStrategy.ALL);
        int stepping = space.getTop().getLevel();
        this.stepping = stepping > 0 ? stepping : 1;
        this.propertyChecked = space.getPropertyChecked();
        this.propertyExpanded = space.getPropertyExpanded();
        this.insufficientUtility = space.getPropertyInsufficientUtility();
        this.solutionSpace.setAnonymityPropertyPredictable(false);
        this.timeLimit = timeLimit;
        if (timeLimit <= 0) { 
            throw new IllegalArgumentException("Invalid time limit. Must be greater than zero."); 
        }
    }
    
    /**
    * Makes sure that the given Transformation has been checked
    * @param transformation
    */
    private void assureChecked(final Transformation transformation) {
        if (!transformation.hasProperty(propertyChecked)) {
            transformation.setChecked(checker.check(transformation, true));
            checks++;
            long previous = getGlobalOptimum() != null ? getGlobalOptimum().getIdentifier() : -1;
            trackOptimum(transformation);
            long current = getGlobalOptimum() != null ? getGlobalOptimum().getIdentifier() : -1;
            if (current != previous && listener != null) {
                listener.update(System.currentTimeMillis() - timeStart, 
                                1d - Double.valueOf(getGlobalOptimum().getInformationLoss().toString()),
                                Arrays.toString(getGlobalOptimum().getGeneralization()),
                                checks);
            }
            progress((double)(System.currentTimeMillis() - timeStart) / (double)timeLimit);
        }
    }

    @Override
    public void traverse() {
        timeStart = System.currentTimeMillis();
        PriorityQueue<Long> queue = new PriorityQueue<Long>(stepping, new Comparator<Long>() {
            @Override
            public int compare(Long arg0, Long arg1) {
                return solutionSpace.getUtility(arg0).compareTo(solutionSpace.getUtility(arg1));
            }
        });
        Transformation bottom = solutionSpace.getBottom();
        assureChecked(bottom);
        queue.add(bottom.getIdentifier());
        Transformation next;
        Long nextId;
        while ((nextId = queue.poll()) != null) {
            next = solutionSpace.getTransformation(nextId);
            if (!prune(next)) {
                expand(queue, next);
                if (getTime() > timeLimit) {
                    return;
                }
            }
        }
        // Final update
        listener.update(System.currentTimeMillis() - timeStart, 
                        1d - Double.valueOf(getGlobalOptimum().getInformationLoss().toString()),
                        Arrays.toString(getGlobalOptimum().getGeneralization()), checks);
    }
    
    /**
     * Returns the current execution time
     * @return
     */
    private int getTime() {
        return (int)(System.currentTimeMillis() - timeStart);
    }

    /**
    * Returns the successor with minimal information loss, if any, null otherwise.
    * @param queue
    * @param transformation
    * @return
    */
    private Transformation expand(PriorityQueue<Long> queue, Transformation transformation) {
        Transformation result = null;

        LongArrayList list = transformation.getSuccessors();
        for (int i = 0; i < list.size(); i++) {
            long id = list.getQuick(i);
            Transformation successor = solutionSpace.getTransformation(id);
            if (!successor.hasProperty(propertyExpanded) && !successor.hasProperty(insufficientUtility)) {
                assureChecked(successor);
                queue.add(successor.getIdentifier());
                if (result == null || successor.getInformationLoss().compareTo(result.getInformationLoss()) < 0) {
                    result = successor;
                }
            }
            if (getTime() > timeLimit) {
                return null;
            }
        }
        transformation.setProperty(propertyExpanded);
        return result;
    }
    
    /**
    * Returns whether we can prune this Transformation
    * @param transformation
    * @return
    */
    private boolean prune(Transformation transformation) {
        
        // Expanded
        if (transformation.hasProperty(propertyExpanded) || transformation.hasProperty(insufficientUtility)) {
            return true;
        }
        
        // Lower bound
        if (getGlobalOptimum() != null) {
            boolean insufficient = transformation.getLowerBound().compareTo(getGlobalOptimum().getInformationLoss()) >= 0;
            if (insufficient) {
                transformation.setProperty(insufficientUtility);
                return true;
            }
        }
        
        // We cannot prune this node
        return false;
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
        return new LIGHTNINGAlgorithm(solutionSpace, checker, timeLimit);
    }
}
