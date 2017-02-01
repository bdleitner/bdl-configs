package com.bdl.config.annotation.processor;

import com.bdl.annotation.processing.model.FieldMetadata;

import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

/**
 * A class to find the initialization string for a given field.
 *
 * @author Ben Leitner
 */
class FieldInitializationGrabber {

  private final Trees trees;
  private final Elements elements;

  FieldInitializationGrabber(Trees trees, Elements elements) {
    this.trees = trees;
    this.elements = elements;
  }

  String findInitializer(FieldMetadata field) {
    TypeElement typeElement = elements.getTypeElement(field.containingClass().toString());
    for (Element element : typeElement.getEnclosedElements()) {
      if (element.getKind() == ElementKind.FIELD && element.getSimpleName().toString().equals(field.name())) {
        VariableTree tree = new FieldScanner().scan(element, trees);
        return tree.getInitializer().toString();
      }
    }
    throw new IllegalArgumentException(String.format("Could not find field element for %s", field));
  }

  private static class FieldScanner extends TreePathScanner<List<VariableTree>, Trees> {
    private List<VariableTree> variableTrees = new ArrayList<>();

    VariableTree scan(Element fieldElement, Trees trees) {
      assert fieldElement.getKind() == ElementKind.FIELD;

      List<VariableTree> variableElement = this.scan(trees.getPath(fieldElement), trees);
      assert variableElement.size() == 1;

      return variableElement.get(0);
    }

    @Override
    public List<VariableTree> scan(TreePath treePath, Trees trees) {
      super.scan(treePath, trees);
      return this.variableTrees;
    }

    @Override
    public List<VariableTree> visitVariable(VariableTree variableTree, Trees trees) {
      this.variableTrees.add(variableTree);
      return super.visitVariable(variableTree, trees);
    }
  }
}
