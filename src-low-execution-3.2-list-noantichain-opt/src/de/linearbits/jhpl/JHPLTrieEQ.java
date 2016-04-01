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



/**
 * This class implements a simple trie for integers that is materialized in a backing integer array
 * @author Fabian Prasser
 */
class JHPLTrieEQ extends JHPLTrie{

    /**
     * Constructs a new trie
     * @param lattice
     */
    JHPLTrieEQ(Lattice<?, ?> lattice) {
        super(lattice, false);
    }

    /**
     * Clears all elements from this trie for the given element
     * @param element
     * @param dimension
     * @param offset
     * @return Whether some elements are still referenced by this node
     */
    boolean clear(int[] element, int dimension, int offset) {
        throw new UnsupportedOperationException("");
    }

    /**
     * Queries this trie for the given element
     * 
     * @param element
     * @param dimension
     * @param offset
     */
    boolean contains(int[] element, int dimension, int offset) {
        for (offset = 0; offset < this.size * this.dimensions; offset += dimensions) {
            boolean match = true;
            for (int i=offset; i<offset+dimensions; i++) {
                if (memory.memory[i] != element[i - offset]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Helper for putting an element into this trie
     * @param element
     * @param dimension
     * @param offset
     */
    void put(int[] element, int dimension, int offset) {
        offset = memory.allocate(dimensions);
        for (int i=offset; i<offset+dimensions; i++) {
            memory.memory[i] = element[i - offset];
        }
        size++;
    }

    @Override
    JHPLTrie newInstance() {
        return new JHPLTrieEQ(this.lattice);
    }
}