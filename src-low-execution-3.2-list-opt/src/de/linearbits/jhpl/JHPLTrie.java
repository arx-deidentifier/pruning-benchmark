/* ******************************************************************************
 * Copyright (c) 2015 Fabian Prasser.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 * Fabian Prasser - initial API and implementation
 * ****************************************************************************
 */
package de.linearbits.jhpl;

import java.util.Iterator;

import de.linearbits.jhpl.JHPLIterator.LongIterator;

/**
 * This class implements a simple trie for integers that is materialized in a backing integer array
 * @author Fabian Prasser
 */
abstract class JHPLTrie {

    /** Constant*/
    protected static final double COMPACTION_THRESHOLD = 0.2d;

    /** The buffer */
    protected final JHPLBuffer memory = new JHPLBuffer();
    /** The number of dimensions */
    protected final int           dimensions;
    /** The height of each dimension */
    protected final int[]         heights;
    /** The Lattice */
    protected final Lattice<?, ?> lattice;
    /** The number of levels */
    protected final int           levels;
    /** The number of used memory units */
    protected int                 used;
    
    protected int size = 0;

    /**
     * Constructs a new trie
     * @param lattice
     * @param withLevel
     */
    JHPLTrie(Lattice<?, ?> lattice, boolean withLevel) {
        
        // Initialize. Root node will be at offset 0
        this.dimensions = lattice.nodes().getDimensions();
        this.heights = lattice.nodes().getHeights();
        this.used = heights[0] + (withLevel ? 1 : 0);
        this.lattice = lattice;
        int sum = 0;
        for (int i = 0; i < this.heights.length; i++) {
            sum += this.heights[i] - 1;
        }
        this.levels = sum + 1;
    }
    
    abstract boolean clear(int[] element, int dimension, int offset);

    abstract JHPLTrie newInstance();
    
    /**
     * Queries this trie for the given element
     * 
     * @param element
     * @param dimension
     * @param offset
     */
    abstract boolean contains(int[] element, int dimension, int offset);
        
    /**
     * Helper for putting an element into this trie
     * @param element
     * @param dimension
     * @param offset
     */
    abstract void put(int[] element, int dimension, int offset);

    /**
     * Clears all above/below this element
     * @param element
     */
    void clear(int[] element) {
        this.clear(element, 0, 0);
        
        // Compaction
        double utilization = (double)used / (double)memory.memory.length;
        if (utilization < COMPACTION_THRESHOLD) {
            compactify();
        }
    }

    /**
     * Compaction method on the trie
     */
    void compactify() {
        
        Iterator<int[]> iterator = iterator();
        JHPLTrie other = newInstance();
        int[] element = iterator.next();
        int size = 0;
        while (element != null) {
            other.put(element, 0, 0);
            element = iterator.next();
            size++;
        }
        this.size = size;
        this.used = size * dimensions;
        this.memory.replace(other.memory);
    }
    /**
     * Queries this trie for the given element
     * @param node
     * @return
     */
    boolean contains(int[] node) {
        return contains(node, 0, 0);
    }
    
    /**
     * Returns the memory consumption in bytes
     * @return
     */
    long getByteSize() {
        return this.memory.memory.length * 4;
    }
    
    /**
     * Returns the number of levels
     * @return
     */
    int getLevels() {
        return this.levels;
    }


    /**
     * Returns an iterator over all elements in the trie. Note: hasNext() is not implemented. Simply iterate until
     * <code>null</code> is returned.
     * @return
     */
    Iterator<int[]> iterator() {
        return new Iterator<int[]>() {
            int[] element = new int[dimensions];
            int offset = 0;
            @Override
            public boolean hasNext() {
                throw new UnsupportedOperationException();
            }
            @Override
            public int[] next() {
                
                while (offset < size * dimensions && memory.memory[offset]==-1) {
                    offset += dimensions;
                }
                
                if (offset < size * dimensions) {
                    for (int i=offset; i<offset+dimensions; i++) {
                        element[i - offset] = memory.memory[i];
                    }
                    offset += dimensions;
                    return element;
                } else {
                    return null;
                }
            }
            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
    
    /**
     * Returns an iterator over all elements in the trie. Note: hasNext() is not implemented. Simply iterate until
     * <code>null</code> is returned.
     * @return
     */
    LongIterator iteratorLong(final long[] multiplier) {

        // Return
        return new LongIterator() {
            
            int offset = 0;

            @Override public boolean hasNext() { throw new UnsupportedOperationException(); }

            @Override
            public long next() {

                while (offset < size * dimensions && memory.memory[offset]==-1) {
                    offset += dimensions;
                }
                
                if (offset < size * dimensions) {
                    long result = 0;
                    for (int i=offset; i<offset+dimensions; i++) {
                        result += memory.memory[i] * multiplier[i - offset];
                    }
                    offset += dimensions;
                    return result;
                } else {
                    return -1;
                }
            }
        };
    }

    /**
     * Puts an element into this trie
     * @param element
     */
    void put(int[] element) {
        put(element, 0, 0);
    }

    /**
     * To string method
     * @param prefix
     * @return
     */
    String toString(String prefix1, String prefix2) {
        return "";
    }
}