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

import java.util.Arrays;
import java.util.Iterator;


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

        Iterator<int[]> iter = this.list.iterator();
        while (iter.hasNext()) {
            int[] array = iter.next();
            if (Arrays.equals(array, element)) {
                iter.remove();
                return true;
            }
        }
        return false;
    }

    /**
     * Queries this trie for the given element
     * 
     * @param element
     * @param dimension
     * @param offset
     */
    boolean contains(int[] element, int dimension, int offset) {

        Iterator<int[]> iter = this.list.iterator();
        while (iter.hasNext()) {
            int[] array = iter.next();
            if (Arrays.equals(array, element)) {
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

        this.list.add(element);
    }

    @Override
    JHPLTrie newInstance() {
        return new JHPLTrieEQ(this.lattice);
    }
}