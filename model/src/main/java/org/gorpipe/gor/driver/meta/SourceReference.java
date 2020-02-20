/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2019 WuXi NextCode Inc.
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

package org.gorpipe.gor.driver.meta;

import org.gorpipe.model.genome.files.gor.DefaultChromoLookup;
import org.gorpipe.model.genome.files.gor.GenomicIterator;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.gorpipe.exceptions.GorSystemException;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

/**
 * Source reference - url and its context.
 * <p>
 * Created by gisli on 29/02/16.
 */
public class SourceReference {

    public final String url;
    public final String securityContext;
    public final String commonRoot; // TODO: This should be removed?
    @JsonIgnore
    GenomicIterator.ChromoLookup lookup;
    public final String chrSubset;
    @JsonIgnore
    public final int[] columns;

    // TODO: evaluate whether the securityContext, lookup and columns should actually be a part of this class.
    // - should the context come in at request time?
    // - should the chromo lookup be retrieved from somewhere instead of pushed into this object?
    // - common root and security context are not used in all driver types, shouldn't this rather be a hash map?
    // - should the context hash map be stored as a part of this class or should it enter the chain at some other point?

    /**
     *
     * @param url                       url for the source.
     * @param securityContext
     * @param commonRoot
     * @param lookup
     * @param chrSubset
     * @param columns
     */
    public SourceReference(String url, String securityContext, String commonRoot, GenomicIterator.ChromoLookup lookup, String chrSubset, int[] columns) {
        this.url = url;
        // Pick up default security context here - it's not propagated from GorOptions if this is a sub query.
        if (securityContext == null) {
            this.securityContext = System.getProperty("gor.security.context");
        } else {
            this.securityContext = securityContext;
        }
        this.commonRoot = commonRoot;
        this.lookup = lookup != null ? lookup : new DefaultChromoLookup();
        this.chrSubset = chrSubset;
        this.columns = columns;
    }

    /**
     * @param url url for the source.
     */
    public SourceReference(String url) {
        this(url, null, null, null, null, null);
    }

    /**
     * @param url                   url for the source.
     * @param parentSourceReference parent source reference to copy unupplied context from.
     */
    public SourceReference(String url, SourceReference parentSourceReference) {
        this(url, parentSourceReference.getSecurityContext(), parentSourceReference.getCommonRoot(),
                parentSourceReference.getLookup(), parentSourceReference.getChrSubset(), parentSourceReference.getColumns());
    }

    @JsonCreator
    public SourceReference(@JsonProperty("url") String url, @JsonProperty("securityContext") String securityContext,
                           @JsonProperty("commonRoot") String commonRoot, @JsonProperty("chrSubset") String chrSubset) {
        this(url, securityContext, commonRoot, null, chrSubset, null);
    }

    public String getUrl() {
        return url;
    }

    public String getSecurityContext() {
        return securityContext;
    }

    public String getCommonRoot() {
        return commonRoot;
    }

    public GenomicIterator.ChromoLookup getLookup() {
        return lookup;
    }

    public void setLookup(GenomicIterator.ChromoLookup lookup) {
        this.lookup = lookup;
    }

    public String getChrSubset() {
        return chrSubset;
    }

    public int[] getColumns() {
        return columns;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SourceReference)) return false;

        SourceReference that = (SourceReference) o;

        return Objects.equals(url, that.url)
                && Objects.equals(securityContext, that.securityContext)
                && Objects.equals(commonRoot, that.commonRoot)
                && Objects.equals(lookup, that.lookup)
                && Objects.equals(chrSubset, that.chrSubset)
                && Arrays.equals(columns, that.columns);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, securityContext, commonRoot, lookup, chrSubset, Arrays.hashCode(columns));
    }

    @Override
    public String toString() {
        return "SourceContext{" +
                "url='" + url + '\'' +
                ", securityContext='" + securityContext + '\'' +
                ", commonRoot='" + commonRoot + '\'' +
                ", lookup=" + lookup +
                ", chrSubset='" + chrSubset + '\'' +
                ", columns=" + Arrays.toString(columns) +
                '}';
    }

    public String toJson() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new GorSystemException("Unable to convert SourceReference to JSON. Content:\n" + this.toString(), e);
        }
    }

    public static SourceReference fromJson(String json) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(json, SourceReference.class);
        } catch (IOException e) {
            throw new GorSystemException("Unable to convert JSON to SourceReference. Content:\n" + json, e);
        }
    }

    /**
     * Builder for the SourceReference, use builder copy constructor to allow copying fields from parent SourceReference.
     */
    public static class Builder {
        private String url;
        private String securityContext;
        private String commonRoot;
        private GenomicIterator.ChromoLookup lookup;
        private String chrSubset;
        private int[] columns;

        public Builder(String url) {
            this.url = url;
        }

        public Builder(String url, SourceReference parentSourceReference) {
            this.url = url;
            // Don't copy objects, we want to share the instance with the parent.
            this.securityContext = parentSourceReference.securityContext;
            this.commonRoot = parentSourceReference.commonRoot;
            this.lookup = parentSourceReference.lookup;
            this.chrSubset = parentSourceReference.chrSubset;
            this.columns = parentSourceReference.columns;
        }

        public SourceReference build() {
            return new SourceReference(url, securityContext, commonRoot, lookup, chrSubset, columns);
        }

        public Builder securityContext(String securityContext) {
            this.securityContext = securityContext;
            return this;
        }

        public Builder commonRoot(String commonRoot) {
            this.commonRoot = commonRoot;
            return this;
        }

        public Builder lookup(GenomicIterator.ChromoLookup lookup) {
            this.lookup = lookup;
            return this;
        }

        public Builder chrSubset(String chrSubset) {
            this.chrSubset = chrSubset;
            return this;
        }

        public Builder columns(int[] columns) {
            this.columns = columns;
            return this;
        }
    }
}
