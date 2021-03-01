/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2021 WuXi NextCode Inc.
 *  All Rights Reserved.
 *
 *  GORpipe is free software: you can redistribute it and/or modify
 *  it under the terms of the AFFERO GNU General Public License as published by
 *  the Free Software Foundation.
 *
 *  GORpipe is distributed "AS-IS" AND WITHOUT ANY WARRANTY OF ANY KIND,
 *  INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 *  NON-INFRINGEMENT, OR FITNESS FOR A PARTICULAR PURPOSE. See
 *  the AFFERO GNU General Public License for the complete license terms.
 *
 *  You should have received a copy of the AFFERO GNU General Public License
 *  along with GORpipe.  If not, see <http://www.gnu.org/licenses/agpl-3.0.html>
 *
 *  END_COPYRIGHT
 */

package org.gorpipe.util;

import org.gorpipe.exceptions.GorSystemException;

import java.io.*;
import java.util.ArrayList;

/**
 * Pair provides a simple pairing of two objects into one
 *
 * @param <X> The type of the first item in the pair
 * @param <Y> The type of the second item in the pair
 * @version $Id$
 */
public class Pair<X extends Serializable, Y extends Serializable> implements Serializable {
    private X former;
    private Y latter;

    /**
     * Construct a new pair
     */
    public Pair() {}

    /**
     * Construct a new pair from the provided objects
     *
     * @param f First object
     * @param s Second object
     */
    public Pair(X f, Y s) {
        former = f;
        latter = s;
    }

    /**
     * Convenience method for pairing up two objects
     *
     * @param <F>    the type of the first object
     * @param <S>    the type of the second object
     * @param first  the first object
     * @param second the second object
     * @return a Pair object containing {@code first} and {@code second}
     */
    private static <F extends Serializable, S extends Serializable> Pair<F, S> createPair(F first, S second) {
        return new Pair<F, S>(first, second);
    }

    /**
     * Read a file that contains string pairs.
     *
     * @param fileName  The name of the file
     * @param separator The separator to seperate the two strings. This is a Regual Expression
     *                  that is passed to the String.split method
     * @return An array list of the pairs
     * @throws IOException
     */
    public static ArrayList<Pair<String, String>> readFile(String fileName, String separator) throws IOException {
        ArrayList<Pair<String, String>> list = new ArrayList<Pair<String, String>>();
        BufferedReader br = new BufferedReader(new FileReader(new File(fileName)));
        try {
            String line;
            while ((line = br.readLine()) != null) {
                // Allow empty lines and commented lines with #
                if (line.length() > 0 && line.charAt(0) != '#') {
                    String[] tokens = line.split(separator);
                    if (tokens.length != 2) {
                        throw new GorSystemException("Expect 2 tokens to form a pair in " + fileName + " not " + tokens.length, null);
                    }
                    list.add(createPair(tokens[0], tokens[1]));
                }
            }
            return list;
        } finally {
            br.close();
        }
    }

    /**
     * Convenience method for pairing up two {@link Comparable} objects
     *
     * @param <F>    the type of the first object (must implement {@link Comparable}
     * @param <S>    the type of the second object (must implement {@link Comparable}
     * @param first  the first object
     * @param second the second object
     * @return a {@link ComparablePair} containing {@code first} and {@code secode}
     */
    public static <F extends Comparable<F> & Serializable, S extends Comparable<S> & Serializable>
    ComparablePair<F, S> createComparablePair(F first, S second) {
        return new ComparablePair<F, S>(first, second);
    }

    /**
     * @return The former object of the pair
     */
    public X getFormer() {
        return former;
    }

    /**
     * @param former the former to set
     */
    public void setFormer(X former) {
        this.former = former;
    }

    /**
     * @return The latter object of the pair
     */
    public Y getLatter() {
        return latter;
    }

    /**
     * @param latter the latter to set
     */
    public void setLatter(Y latter) {
        this.latter = latter;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Pair<?, ?>) {
            Pair<?, ?> other = (Pair<?, ?>) obj;
            return getFormer().equals(other.getFormer()) &&
                    getLatter().equals(other.getLatter());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return getFormer().hashCode() ^ getLatter().hashCode();
    }

    @Override
    public String toString() {
        return "[" + getFormer() + ", " + getLatter() + "]";
    }

    /**
     * A {@link Comparable} version of the {@link Pair} class
     *
     * @param <X> the type of the first object (must implement {@link Comparable}
     * @param <Y> the type of the second object (must implement {@link Comparable}
     */
    public static class ComparablePair<X extends Comparable<X> & Serializable, Y extends Comparable<Y> & Serializable>
            extends Pair<X, Y> implements Comparable<Pair<X, Y>> {
        @SuppressWarnings("javadoc")
        public ComparablePair(X f, Y s) {
            super(f, s);
        }

        /**
         * Compares the {@code former} objects in the pairs first.
         * If the result is not 0 that is returned, otherwise the result
         * of comparing the {@code latter} objects is returned.
         */
        public int compareTo(Pair<X, Y> other) {
            int result = getFormer().compareTo(other.getFormer());
            return result != 0 ? result : getLatter().compareTo(other.getLatter());
        }
    }
}
