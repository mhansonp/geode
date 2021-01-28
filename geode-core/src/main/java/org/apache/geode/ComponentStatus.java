package org.apache.geode;

public interface ComponentStatus {
  String name();

  String getStatusString();

  void print();
}
