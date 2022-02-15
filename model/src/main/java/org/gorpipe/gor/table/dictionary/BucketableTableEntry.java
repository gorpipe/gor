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

package org.gorpipe.gor.table.dictionary;

import com.google.common.base.Strings;
import org.gorpipe.gor.table.util.GenomicRange;
import org.gorpipe.gor.table.util.PathUtils;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * BaseTable entry with bucket information.
 * <p>
 * Created by gisli on 22/08/16.
 */
public abstract class BucketableTableEntry extends TableEntry {

    // Use strings for performance reasons (using Path or URI takes twice as long to parse).
    protected String bucketLogical;
    protected Boolean isDeleted;

    protected BucketableTableEntry(BucketableTableEntry entry) {
        super(entry);
        this.bucketLogical = entry.bucketLogical;
        this.isDeleted = entry.isDeleted;
    }

    protected BucketableTableEntry(String contentLogical, URI rootUri,String alias, String[] tags, GenomicRange range, String bucket, boolean isDeleted) {
        super(contentLogical, rootUri, alias, tags, range);
        this.bucketLogical = bucket;
        this.isDeleted = isDeleted;
    }

    /**
     * Get unique key for the entry.
     * NOTE: If they fields used to generate the key are changed then the entries must be deleted and reinserted.
     */
    @Override
    public String getKey() {
        if (key == null) {
            String newKey = super.getKey();
            // We keep deleted entries around for the the bucket (to know what to exclude).
            // So for each deleted entry we need to add the bucket to the key.
            if (isDeleted()) {
                newKey += getBucket();
            }
            key = newKey;
        }
        return key;
    }
    
    /**
     * Get the bucket (relative path)
     *
     * @return  the bucket.
     */
    public String getBucket() {
        return this.bucketLogical;
    }

    /**
     * Get the buckets real path.
     *
     * @return the buckets real path.
     */
    public String getBucketReal() {
        return getBucket() != null ? PathUtils.resolve(getRootUri(), getBucket()).toString() : null;
    }

    /**
     * Get bucket as path.
     *
     * @return bucket as path.
     */
    public Path getBucketPath() {
        return getBucket() != null ? Paths.get(getBucket()) : null;
    }
    
    /**
     * Set the bucket logical path.
     *
     * @param bucket bucket file, normalized and relative to table or absolute.
     */
    protected void setBucket(String bucket) {
        this.bucketLogical = bucket;
        this.key = null;
    }

    public boolean hasBucket() {
        return !Strings.isNullOrEmpty(getBucket());
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        this.isDeleted = deleted;
        this.key = null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BucketableTableEntry)) return false;
        if (!super.equals(o)) return false;
        BucketableTableEntry that = (BucketableTableEntry) o;
        return Objects.equals(bucketLogical, that.bucketLogical) &&
                Objects.equals(isDeleted, that.isDeleted);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), bucketLogical, isDeleted);
    }

    public abstract static class Builder<B extends Builder> extends TableEntry.Builder<B> {
        protected String bucketLogical = null;
        protected boolean isDeleted = false;

        public Builder(String contentLogical, URI rootUri) {
            super(contentLogical, rootUri);
        }

        public B bucket(String bucketLogical) {
            this.bucketLogical = bucketLogical;
            return self();
        }

        public B deleted() {
            this.isDeleted = true;
            return self();
        }

    }

}
