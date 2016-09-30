package com.bdl.config.annotation.processor;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.Map;

import javax.annotation.processing.Messager;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;

/**
 * Tests for the combined functionality of {@link ConfigPackageTree} and {@link DaggerModuleFileWriterVisitor}.
 *
 * @author Ben Leitner
 */
@RunWith(JUnit4.class)
public class DaggerModulesTest {

  private static final Messager DO_NOTHING_MESSAGER = new Messager() {
    @Override
    public void printMessage(Diagnostic.Kind kind, CharSequence msg) {

    }

    @Override
    public void printMessage(Diagnostic.Kind kind, CharSequence msg, Element e) {

    }

    @Override
    public void printMessage(Diagnostic.Kind kind, CharSequence msg, Element e, AnnotationMirror a) {

    }

    @Override
    public void printMessage(Diagnostic.Kind kind, CharSequence msg, Element e, AnnotationMirror a, AnnotationValue v) {

    }
  };

  @Test
  public void testDaggerModuleOutput() throws IOException {
    ConfigPackageTree tree = new ConfigPackageTree();
    tree.addConfig(
        DO_NOTHING_MESSAGER,
        ConfigMetadata.builder()
            .packageName("com.bdl.config.things")
            .className("Thing1")
            .fieldName("flag1")
            .type("java.lang.String")
            .visibility(ConfigMetadata.Visibility.PACKAGE)
            .build());
    tree.addConfig(
        DO_NOTHING_MESSAGER,
        ConfigMetadata.builder()
            .packageName("com.bdl.config.things")
            .className("Thing2")
            .fieldName("flag2")
            .type("java.lang.String")
            .visibility(ConfigMetadata.Visibility.PUBLIC)
            .qualifier(Annotations.qualifier("flag2").toString())
            .build());
    tree.addConfig(
        DO_NOTHING_MESSAGER,
        ConfigMetadata.builder()
            .packageName("com.bdl.config.others")
            .className("OtherThingA")
            .fieldName("otherFlag1")
            .type("java.lang.String")
            .visibility(ConfigMetadata.Visibility.PUBLIC)
            .build());
    tree.addConfig(
        DO_NOTHING_MESSAGER,
        ConfigMetadata.builder()
            .packageName("com.bdl.config.others")
            .className("OtherThingA")
            .fieldName("otherFlag2")
            .type("java.lang.String")
            .visibility(ConfigMetadata.Visibility.PRIVATE)
            .build());
    tree.addConfig(
        DO_NOTHING_MESSAGER,
        ConfigMetadata.builder()
            .packageName("com.bdl.config.others")
            .className("OtherThingA")
            .fieldName("otherFlag3")
            .type("java.lang.String")
            .visibility(ConfigMetadata.Visibility.PACKAGE)
            .qualifier(Annotations.qualifier("").toString())
            .build());

    tree.pullPublicAndPrivateConfigsUp();

    final Map<String, Writer> writerMap = Maps.newHashMap();

    DaggerModuleFileWriterVisitor daggerVisitor = new DaggerModuleFileWriterVisitor(
        DO_NOTHING_MESSAGER, input -> {
          StringWriter writer = new StringWriter();
          writerMap.put(input + ".txt", writer);
          return writer;
        });

    tree.visit(daggerVisitor);
    for (Map.Entry<String, Writer> entry : writerMap.entrySet()) {
      URL resource = getClass().getClassLoader().getResource(entry.getKey());
      String file = Resources.toString(resource, Charsets.UTF_8);

      assertThat(entry.getValue().toString()).isEqualTo(file);
    }
  }

  /**
   * Tests that a module will be written to include submodules in different subpackages even when no configs
   * are present in the common-prefix package.
   */
  @Test
  public void testWriteModulesAtJoinNodes() throws Exception {
    ConfigPackageTree tree = new ConfigPackageTree();
    tree.addConfig(
        DO_NOTHING_MESSAGER,
        ConfigMetadata.builder()
            .packageName("com.bdl.config.alllocal.sub1")
            .className("Local")
            .fieldName("sub1")
            .type("java.lang.Integer")
            .visibility(ConfigMetadata.Visibility.PACKAGE)
            .build());
    tree.addConfig(
        DO_NOTHING_MESSAGER,
        ConfigMetadata.builder()
            .packageName("com.bdl.config.alllocal.sub2.sub")
            .className("Local")
            .fieldName("sub2")
            .type("java.lang.String")
            .visibility(ConfigMetadata.Visibility.PACKAGE)
            .build());

    tree.pullPublicAndPrivateConfigsUp();

    final Map<String, Writer> writerMap = Maps.newHashMap();

    DaggerModuleFileWriterVisitor daggerVisitor = new DaggerModuleFileWriterVisitor(
        DO_NOTHING_MESSAGER, input -> {
          StringWriter writer = new StringWriter();
          writerMap.put(input + ".txt", writer);
          return writer;
        });

    tree.visit(daggerVisitor);
    assertThat(writerMap.keySet()).containsExactly(
        "com.bdl.config.alllocal.ConfigDaggerModule.txt",
        "com.bdl.config.alllocal.sub1.ConfigDaggerModule.txt",
        "com.bdl.config.alllocal.sub2.sub.ConfigDaggerModule.txt");

    for (Map.Entry<String, Writer> entry : writerMap.entrySet()) {
      URL resource = getClass().getClassLoader().getResource(entry.getKey());
      String file = Resources.toString(resource, Charsets.UTF_8);

      assertThat(entry.getValue().toString()).isEqualTo(file);
    }

  }
}
