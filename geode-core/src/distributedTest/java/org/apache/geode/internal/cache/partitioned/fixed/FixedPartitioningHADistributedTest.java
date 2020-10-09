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
package org.apache.geode.internal.cache.partitioned.fixed;

import static java.lang.System.lineSeparator;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.geode.cache.FixedPartitionAttributes.createFixedPartition;
import static org.apache.geode.cache.RegionShortcut.PARTITION;
import static org.apache.geode.distributed.ConfigurationProperties.MEMBER_TIMEOUT;
import static org.apache.geode.distributed.ConfigurationProperties.SERIALIZABLE_OBJECT_FILTER;
import static org.apache.geode.test.dunit.VM.getController;
import static org.apache.geode.test.dunit.VM.getVM;
import static org.apache.geode.test.dunit.VM.getVMId;
import static org.apache.geode.test.dunit.rules.DistributedRule.getDistributedSystemProperties;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import org.apache.geode.cache.EntryOperation;
import org.apache.geode.cache.FixedPartitionAttributes;
import org.apache.geode.cache.FixedPartitionResolver;
import org.apache.geode.cache.PartitionAttributesFactory;
import org.apache.geode.cache.Region;
import org.apache.geode.distributed.ServerLauncher;
import org.apache.geode.internal.cache.BucketRegion;
import org.apache.geode.internal.cache.PartitionedRegion;
import org.apache.geode.internal.cache.PartitionedRegionDataStore;
import org.apache.geode.internal.cache.partitioned.RegionAdvisor;
import org.apache.geode.test.dunit.rules.DistributedExecutorServiceRule;
import org.apache.geode.test.dunit.rules.DistributedMap;
import org.apache.geode.test.dunit.rules.DistributedReference;
import org.apache.geode.test.dunit.rules.DistributedRule;
import org.apache.geode.test.junit.categories.PartitioningTest;
import org.apache.geode.test.junit.rules.RandomRule;
import org.apache.geode.test.junit.rules.serializable.SerializableTemporaryFolder;

@Category(PartitioningTest.class)
@SuppressWarnings("serial")
public class FixedPartitioningHADistributedTest implements Serializable {

  private final String REGION_NAME = "theRegion";

  private static int PARTITIONS = 0;
  private static int BUCKETS_PER_PARTITION = 0;
  private static int COUNT = 0;

  private final int THREADS = 1;

  private final int LOCAL_MAX_MEMORY = 10;
  private final int REDUNDANT_COPIES = 1;
  private static int TOTAL_NUM_BUCKETS = 0;
  private int partitions;
  private int buckets;

  private FixedPartitionAttributes primary(int whichPartition) {
    return createFixedPartition("Partition-" + whichPartition, true, BUCKETS_PER_PARTITION);
  }

  private FixedPartitionAttributes secondary(int whichPartition) {
    return createFixedPartition("Partition-" + whichPartition, false, BUCKETS_PER_PARTITION);
  }

  public static Map<Integer, Partition> BUCKET_TO_PARTITION;


  private final AtomicInteger DONE = new AtomicInteger();

  @Rule
  public DistributedRule distributedRule = new DistributedRule();
  @Rule
  public DistributedReference<ServerLauncher> serverLauncher = new DistributedReference<>();
  @Rule
  public DistributedReference<AtomicBoolean> doPuts = new DistributedReference<>();
  @Rule
  public DistributedMap<Integer, List<FixedPartitionAttributes>> fpaMap = new DistributedMap<>();
  @Rule
  public DistributedExecutorServiceRule executorServiceRule = new DistributedExecutorServiceRule();
  @Rule
  public SerializableTemporaryFolder temporaryFolder = new SerializableTemporaryFolder();
  @Rule
  public RandomRule randomRule = new RandomRule();

  @Before
  public void setUp() {
    partitions = randomRule.nextInt(3, 10);
    buckets = randomRule.nextInt(3, 20);

    for (int whichVM = -1; whichVM < partitions; whichVM++) {
      getVM(whichVM).invoke(() -> {
        PARTITIONS = partitions;
        BUCKETS_PER_PARTITION = buckets;
        System.out
            .println("MLH PARTITIONS = " + partitions + " BUCKETS_PER_PARTITION = " + buckets);
        COUNT = partitions * buckets;
        TOTAL_NUM_BUCKETS = COUNT;
        BUCKET_TO_PARTITION = initialize(new HashMap<>());
      });
    }

    fpaMap.put(getController().getId(), emptyList());

    int secondary = 1;
    for (int primary = 0; primary < PARTITIONS; primary++) {
      assertThat(secondary).isNotEqualTo(primary);

      fpaMap.put(primary, asList(primary(primary), secondary(secondary)));

      secondary++;
      if (secondary == PARTITIONS) {
        secondary = 0;
      }
    }

    for (int whichVM = 0; whichVM < PARTITIONS; whichVM++) {
      final int vmId = whichVM;
      getVM(vmId).invoke(() -> startServer(vmId, "datastore" + vmId, LOCAL_MAX_MEMORY));
    }

    getController().invoke(() -> startServer(getController().getId(), "accessor1"));

    for (int vmId = -1; vmId < PARTITIONS; vmId++) {
      getVM(vmId).invoke(() -> {
        doPuts.set(new AtomicBoolean(true));
        DONE.set(0);
      });
    }
  }

  @Test
  public void recoversAfterBouncingOneDatastore() {
    getController().invoke(() -> {

      Region<Object, Object> region = serverLauncher.get().getCache().getRegion(REGION_NAME);
      for (int i = 0; i < COUNT; i++) {
        Partition partition = BUCKET_TO_PARTITION.get(i);
        region.put(partition, "value-" + i);
        // 0, "value-0"
        // 18, "value-18"
      }

      validateBucketsAreFullyRedundant();
    });

    for (int whichVM = 0; whichVM < PARTITIONS; whichVM++) {
      final int vmId = whichVM;
      getVM(vmId).invoke(() -> dumpBucketMetadata(vmId, "BEFORE"));
    }

    for (int vmId = -1; vmId < PARTITIONS; vmId++) {
      getVM(vmId).invoke(() -> {
        Region<Object, Object> region = serverLauncher.get().getCache().getRegion(REGION_NAME);
        for (int i = 0; i < THREADS; i++) {
          executorServiceRule.submit(() -> {
            try {
              while (doPuts.get().get()) {
                int bucketId = randomRule.nextInt(1, COUNT);
                region.put(BUCKET_TO_PARTITION.get(bucketId), "value-" + (100 + bucketId));
              }
            } finally {
              DONE.incrementAndGet();
            }
          });
        }
      });
    }

    getVM(serversToBounce())
        .bounceForcibly()
        .invoke(() -> {
          System.out.println("KIRK: about to bounce " + getVMId());

          doPuts.set(new AtomicBoolean(true));

          startServer(getVMId(), "datastore" + getVMId(), 10);

          Region<Object, Object> region = serverLauncher.get().getCache().getRegion(REGION_NAME);
          for (int i = 0; i < THREADS; i++) {
            executorServiceRule.submit(() -> {
              try {
                while (doPuts.get().get()) {
                  int bucketId = randomRule.nextInt(1, COUNT);
                  region.put(BUCKET_TO_PARTITION.get(bucketId), "value-" + (1000 + bucketId));
                }
              } finally {
                DONE.incrementAndGet();
              }
            });
          }
        });

    for (int vmId = -1; vmId < PARTITIONS; vmId++) {
      getVM(vmId).invoke(() -> doPuts.get().set(false));
    }

    for (int whichVM = 0; whichVM < PARTITIONS; whichVM++) {
      final int vmId = whichVM;
      getVM(vmId).invoke(() -> dumpBucketMetadata(vmId, "AFTER"));
    }

    getController().invoke(this::validateBucketsHavePrimary);
  }

  private void startServer(int vm, String name) {
    startServer(vm, name, 0);
  }

  private void startServer(int vm, String name, int localMaxMemory) {

    PARTITIONS = partitions;
    BUCKETS_PER_PARTITION = buckets;
    System.out.println("MLH PARTITIONS = " + partitions + " BUCKETS_PER_PARTITION = " + buckets);
    COUNT = partitions * buckets;
    TOTAL_NUM_BUCKETS = COUNT;
    BUCKET_TO_PARTITION = initialize(new HashMap<>());

    serverLauncher.set(new ServerLauncher.Builder()
        .set(getDistributedSystemProperties())
        .set(MEMBER_TIMEOUT, "2000")
        .set(SERIALIZABLE_OBJECT_FILTER, "org.apache.geode.**")
        .setDisableDefaultServer(true)
        .setMemberName(name)
        .setWorkingDirectory(folder(name).getAbsolutePath())
        .build())
        .get()
        .start();

    PartitionAttributesFactory<Partition, Object> partitionAttributesFactory =
        new PartitionAttributesFactory<Partition, Object>()
            .setLocalMaxMemory(localMaxMemory)
            .setPartitionResolver(new PartitionResolver())
            .setRedundantCopies(REDUNDANT_COPIES)
            .setTotalNumBuckets(TOTAL_NUM_BUCKETS);

    List<FixedPartitionAttributes> fpaList = fpaMap.get(vm);

    if (isNotEmpty(fpaList)) {
      fpaList.forEach(partitionAttributesFactory::addFixedPartitionAttributes);
    }

    serverLauncher.get().getCache()
        .createRegionFactory(PARTITION)
        .setPartitionAttributes(partitionAttributesFactory.create())
        .create(REGION_NAME);
  }

  private void dumpBucketMetadata(int vmId, String when) {
    Region<Object, Object> region = serverLauncher.get().getCache().getRegion(REGION_NAME);
    PartitionedRegion partitionedRegion = (PartitionedRegion) region;

    PartitionedRegionDataStore dataStore = partitionedRegion.getDataStore();
    Set<Entry<Integer, BucketRegion>> localBuckets = dataStore.getAllLocalBuckets();

    StringBuilder sb = new StringBuilder();
    sb
        .append("KIRK:")
        .append(when)
        .append(": server")
        .append(vmId)
        .append(" contains ")
        .append(localBuckets.size())
        .append(" bucket ids: [");
    boolean linefeed = false;
    for (Entry<Integer, BucketRegion> localBucket : localBuckets) {
      if (linefeed) {
        sb.append(", ");
      }
      sb
          .append(lineSeparator())
          .append(localBucket.getKey())
          .append("=")
          .append(localBucket.getValue());
      linefeed = true;
    }
    sb
        .append(lineSeparator())
        .append("]");
    System.out.println(sb);
  }

  private int serversToBounce() {
    return randomRule.nextInt(0, PARTITIONS - 1);
  }

  private void validateBucketsAreFullyRedundant() {
    Region<Object, Object> region = serverLauncher.get().getCache().getRegion(REGION_NAME);
    PartitionedRegion partitionedRegion = (PartitionedRegion) region;
    RegionAdvisor regionAdvisor = partitionedRegion.getRegionAdvisor();

    for (int i = 0; i < TOTAL_NUM_BUCKETS; i++) {
      assertThat(partitionedRegion.isFixedPartitionedRegion()).isTrue();
      assertThat(regionAdvisor.getBucketRedundancy(i))
          .as("Bucket ID = " + i + " Redundancy = " + (regionAdvisor.getBucketRedundancy(i)))
          .isEqualTo(REDUNDANT_COPIES);
    }
  }

  private void validateBucketsHavePrimary() {
    Region<Object, Object> region = serverLauncher.get().getCache().getRegion(REGION_NAME);
    PartitionedRegion partitionedRegion = (PartitionedRegion) region;
    RegionAdvisor regionAdvisor = partitionedRegion.getRegionAdvisor();

    for (int bucketId = 0; bucketId < TOTAL_NUM_BUCKETS; bucketId++) {
      assertThat(regionAdvisor.getPrimaryMemberForBucket(bucketId)).isNotNull();
    }
  }

  private File folder(String name) {
    File folder = new File(temporaryFolder.getRoot(), name);
    if (!folder.exists()) {
      assertThat(folder.mkdirs()).isTrue();
    }
    return folder;
  }

  private Map<Integer, Partition> initialize(Map<Integer, Partition> map) {
    for (int whichPartition = 0; whichPartition < COUNT; whichPartition++) {
      map.put(whichPartition, new Partition(whichPartition));
    }
    return map;
  }

  public static class Partition implements Serializable {

    private final int partition;
    private final String partitionName;

    private Partition(int partition) {
      this.partition = partition;
      int dunno = partition / BUCKETS_PER_PARTITION;
      assertThat(dunno >= 0 && dunno < PARTITIONS);
      partitionName = "Partition-" + dunno;
    }

    String getPartitionName() {
      return partitionName;
    }

    @Override
    public String toString() {
      return partitionName;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof Partition) {
        return partition == ((Partition) obj).partition;
      }
      return false;
    }

    @Override
    public int hashCode() {
      return partition;
    }
  }

  public static class PartitionResolver
      implements FixedPartitionResolver<Partition, Object>, Serializable {

    @Override
    public String getName() {
      return getClass().getName();
    }

    @Override
    public String getPartitionName(EntryOperation<Partition, Object> opDetails,
        Set<String> targetPartitions) {
      return opDetails.getKey().getPartitionName();
    }

    @Override
    public Partition getRoutingObject(EntryOperation<Partition, Object> opDetails) {
      return opDetails.getKey();
    }
  }
}
