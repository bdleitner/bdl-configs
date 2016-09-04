package com.bdl.config.annotation.processor;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.Messager;
import javax.tools.Diagnostic;

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

  void addConfig(Messager messager, ConfigMetadata config) {
    root.addConfigToPackage(messager, config.packageName() + ".", config);
  }

  void pullPublicAndPrivateConfigsUp() {
    Node node = getCommonNode();
    node.moveNonPackageConfigsTo(node);
  }

  private Node getCommonNode() {
    Node common = root;
    while (common.children.size() == 1) {
      common = Iterables.getOnlyElement(common.children.values());
    }
    return common;
  }

  DaggerModuleFile toModuleFile() {
    return root.toModuleFile("");
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

    private void addConfigToPackage(Messager messager, String packageName, ConfigMetadata config) {
      if (packageName.isEmpty()) {
        messager.printMessage(Diagnostic.Kind.NOTE,
            String.format("Adding %s config %s to package %s.",
                config.visibility(), config.fullyQualifiedPathName(), packagePart));
        configs.add(config);
        return;
      }
      String[] parts = packageName.split("\\.", 2);
      getChild(parts[0]).addConfigToPackage(messager, parts[1], config);
    }

    /**
     * Moves any public or private configs from {@code this} node to the given node.
     *
     * @return {@code true} if there are no more configs after the move, indicating that
     * this node can be removed from its parent.
     */
    private boolean moveNonPackageConfigsTo(Node node) {
      Set<String> childrenToRemove = Sets.newHashSet();
      for (Map.Entry<String, Node> entry : children.entrySet()) {
        if (entry.getValue().moveNonPackageConfigsTo(node)) {
          childrenToRemove.add(entry.getKey());
        }
      }
      for (String key : childrenToRemove) {
        children.remove(key);
      }
      if (this == node) {
        return false;
      }
      for (Iterator<ConfigMetadata> it = configs.iterator(); it.hasNext();) {
        ConfigMetadata config = it.next();
        if (config.visibility() == ConfigMetadata.Visibility.PUBLIC
            || config.visibility() == ConfigMetadata.Visibility.PRIVATE) {
          node.configs.add(config);
          it.remove();
        }
      }

      return configs.isEmpty();
    }

    DaggerModuleFile toModuleFile(String prefix) {
      String thisPackage = prefix + packagePart;
      ImmutableSet.Builder<DaggerModuleFile> files = ImmutableSet.builder();
      for (Map.Entry<String, Node> entry : children.entrySet()) {
        files.add(entry.getValue().toModuleFile(thisPackage.isEmpty() ? "" : thisPackage + "."));
      }
      return new DaggerModuleFile(
          String.format("%s.ConfigDaggerModule", thisPackage),
          configs,
          files.build());
    }
  }
}
