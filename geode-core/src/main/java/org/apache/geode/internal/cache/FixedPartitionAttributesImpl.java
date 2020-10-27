/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.apache.geode.internal.cache;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.geode.DataSerializable;
import org.apache.geode.DataSerializer;
import org.apache.geode.cache.FixedPartitionAttributes;
import org.apache.geode.logging.internal.SelectiveLogger;

/**
 * Internal implementation of {@link FixedPartitionAttributes}.
 *
 * @since GemFire 6.6
 */

public class FixedPartitionAttributesImpl extends FixedPartitionAttributes
    implements DataSerializable {
  private static final long serialVersionUID = 7435010874879693776L;

  /**
   * name of the partition
   */
  private String partitionName;

  /**
   * represents primary status
   */
  private boolean isPrimary = false;

  /**
   * number of buckets allowed to create for this partition.
   */
  private int numBuckets = 1;

  private int startingBucketID = KeyInfo.UNKNOWN_BUCKET;

  /**
   * Constructs an instance of <code>FixedPartitionAttributes</code> with default settings.
   */
  public FixedPartitionAttributesImpl() {

  }

  @Override
  public String getPartitionName() {
    return this.partitionName;
  }

  @Override
  public int getNumBuckets() {
    return this.numBuckets;
  }

  @Override
  public boolean isPrimary() {
    return this.isPrimary;
  }

  @Override
  public void fromData(DataInput in) throws IOException, ClassNotFoundException {
    this.partitionName = DataSerializer.readString(in);
    this.isPrimary = in.readBoolean();
    this.numBuckets = in.readInt();
    this.startingBucketID = in.readInt();

  }

  @Override
  public void toData(DataOutput out) throws IOException {
    DataSerializer.writeString(this.partitionName, out);
    out.writeBoolean(this.isPrimary);
    out.writeInt(this.numBuckets);
    out.writeInt(this.startingBucketID);
  }

  public FixedPartitionAttributesImpl setPartitionName(String name) {
    this.partitionName = name;
    return this;
  }

  public FixedPartitionAttributesImpl isPrimary(boolean isPrimary2) {
    this.isPrimary = isPrimary2;
    return this;
  }

  public FixedPartitionAttributesImpl setNumBuckets(int numBuckets) {
    this.numBuckets = numBuckets;
    return this;
  }

  public void setStartingBucketID(int startingBucketID) {
    SelectiveLogger selectiveLogger = new SelectiveLogger();
    selectiveLogger.setPrepend(() -> " This = " + this + " MLH setStartingBucketID ");
    selectiveLogger.log(" 1  startingBucketID = " + startingBucketID).print();
    this.startingBucketID = startingBucketID;
  }

  public int getStartingBucketID() {
    return startingBucketID;
  }

  public int getLastBucketID() {
    return startingBucketID + numBuckets - 1;
  }

  public boolean hasBucket(int bucketId) {
    boolean result = getStartingBucketID() <= bucketId && bucketId <= getLastBucketID();
    SelectiveLogger selectiveLogger = new SelectiveLogger();
    selectiveLogger
        .setPrepend(() -> "Bucket = " + bucketId + " This = " + this + " MLH hasBucket ");
    selectiveLogger.log(" 1 entered getStartingBucketID() (" + getStartingBucketID()
        + ") <= bucketId (" + bucketId + ") && bucketId (" + bucketId + ") <= getLastBucketID() ("
        + getLastBucketID() + ") == result = " + result);
    switch (bucketId) {
      case 1:
      case 2:
      case 3:
      case 5:
      case 6:
      case 7:
        selectiveLogger.print();
    }
    return result;
  }

  public boolean equals(final Object obj) {
    if (obj == null) {
      return false;
    }
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof FixedPartitionAttributesImpl)) {
      return false;
    }
    FixedPartitionAttributesImpl spr = (FixedPartitionAttributesImpl) obj;
    return spr.getPartitionName().equals(this.getPartitionName());
  }

  public int hashCode() {
    return this.getPartitionName().hashCode();
  }

  public String toString() {
    StringBuffer s = new StringBuffer();
    s.append("FixedPartitionAttributes@").append("[partitionName=").append(this.partitionName)
        .append(";isPrimary=").append(this.isPrimary).append(";numBuckets=")
        .append(this.numBuckets);
    s.append(";startingBucketID= ").append(this.startingBucketID);
    s.append("]");
    return s.toString();
  }
}
