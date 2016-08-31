package com.bdl.config.annotation.processor;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import javax.lang.model.element.Modifier;
import java.util.Map;
import java.util.Set;

/**
 * A class to hold the found configurable metadata in a tree structure.
 *
 * @author Ben Leitner
 */
class ConfigPackageTree {

  private final Node root;

  ConfigPackageTree() {
    this.root = new Node("");
  }

  void addConfig(ConfigMetadata config) {
    root.addConfig(config);
  }

  void pushPublicAndPrivateConfigsDown() {
    root.pushConfigsDown();
  }

  private static class Node {
    private final String packagePart;
    private final Map<String, Node> children;
    private final Set<ConfigMetadata> configs;

    private Node(String packagePart) {
      this.packagePart = packagePart;
      children = Maps.newHashMap();
      configs = Sets.newHashSet();
    }

    private Node getChild(String packagePart) {
      Node child = children.get(packagePart);
      if (child == null) {
        child = new Node(packagePart);
        children.put(packagePart, child);
      }
      return child;
    }

    private void addConfig(ConfigMetadata config) {
      if (config.fieldModifiers().contains(Modifier.PUBLIC)
          || config.fieldModifiers().contains(Modifier.PRIVATE)) {
        configs.add(config);
        return;
      }
      addConfigToPackage(config.packageName() + ".", config);
    }

    private void addConfigToPackage(String packageName, ConfigMetadata config) {
      if (packageName.isEmpty()) {
        configs.add(config);
        return;
      }
      String[] parts = packageName.split("\\.", 2);
      getChild(parts[0]).addConfigToPackage(parts[1], config);
    }

    private void pushConfigsDown() {
      for (ConfigMetadata config : configs) {
        if (!config.fieldModifiers().contains(Modifier.PRIVATE)
            && !config.fieldModifiers().contains(Modifier.PUBLIC)) {
          // we have a package-private config, no more pushing allowed.
          return;
        }
      }
      // Ok, if we get here, everything is public or private.
      if (children.size() != 1) {
        // No ambiguous place to push, so no more pushing allowed.
        return;
      }
      // Ok, push everything one level and repeat.
      Node node = Iterables.getOnlyElement(children.values());
      node.configs.addAll(this.configs);
      this.configs.clear();
      node.pushConfigsDown();
    }
  }
}
