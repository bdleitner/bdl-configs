package com.bdl.config.annotation.processor;

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

  /** Interface for classes that visit the nodes and act on the configs.*/
  interface Visitor<T> {
    /** Called upon visiting a node. */
    Set<T> visit(Set<T> childOutputs, String packageName, Set<ConfigMetadata> configs);
  }

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

  <T> void visit(Visitor<T> visitor) {
    root.visit(visitor);
  }

  private static class Node {
    private final String packageName;
    private final Map<String, Node> children;
    private final Set<ConfigMetadata> configs;

    private Node(String packageName) {
      this.packageName = packageName;
      children = Maps.newHashMap();
      configs = Sets.newHashSet();
    }

    private Node getChild(String packagePart) {
      Node child = children.get(packagePart);
      if (child == null) {
        String childPackage = PackageNameUtil.append(packageName, packagePart);
        child = new Node(childPackage);
        children.put(packagePart, child);
      }
      return child;
    }

    private void addConfigToPackage(Messager messager, String packageNameTail, ConfigMetadata config) {
      if (packageNameTail.isEmpty()) {
        messager.printMessage(Diagnostic.Kind.NOTE,
            String.format("Adding %s config %s to package %s.",
                config.visibility(), config.fullyQualifiedPathName(), this.packageName));
        configs.add(config);
        return;
      }
      String[] parts = packageNameTail.split("\\.", 2);
      getChild(parts[0]).addConfigToPackage(messager, parts[1], config);
    }

    /**
     * Moves any public or private configs from {@code this} node to the given node.
     *
     * @return {@code true} if there are no more configs in this package or any subpackage, indicating that
     * this node can be removed from its parent.
     */
    private boolean moveNonPackageConfigsTo(Node node) {
      boolean canRemoveMe = true;
      Set<String> childrenToRemove = Sets.newHashSet();
      for (Map.Entry<String, Node> entry : children.entrySet()) {
        if (entry.getValue().moveNonPackageConfigsTo(node)) {
          childrenToRemove.add(entry.getKey());
        } else {
          canRemoveMe = false;
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

      return canRemoveMe && configs.isEmpty();
    }

    private <T> Set<T> visit(Visitor<T> visitor) {
      Set<T> childOutputs = Sets.newHashSet();
      for (Map.Entry<String, Node> entry : children.entrySet()) {
        childOutputs.addAll(entry.getValue().visit(visitor));
      }
      return visitor.visit(childOutputs, packageName, configs);
    }
  }
}
