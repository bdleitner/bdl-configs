package com.bdl.config.annotation.processor;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
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
public class GuiceModulesTest {

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
  public void testGuiceModuleOutput() throws IOException {
    ConfigPackageTree tree = new ConfigPackageTree();
    tree.addConfig(
        DO_NOTHING_MESSAGER,
        ConfigMetadata.builder()
            .className("Thing1")
            .packageName("com.bdl.config.things")
            .fieldName("flag1")
            .type("java.lang.String")
            .visibility(ConfigMetadata.Visibility.PACKAGE)
            .build());
    tree.addConfig(
        DO_NOTHING_MESSAGER,
        ConfigMetadata.builder()
            .className("Thing2")
            .packageName("com.bdl.config.things")
            .fieldName("flag2")
            .type("java.lang.String")
            .visibility(ConfigMetadata.Visibility.PUBLIC)
            .qualifier(Annotations.qualifier("flag2").toString())
            .build());
    tree.addConfig(
        DO_NOTHING_MESSAGER,
        ConfigMetadata.builder()
            .className("OtherThingA")
            .packageName("com.bdl.config.others")
            .fieldName("otherFlag1")
            .type("java.lang.String")
            .visibility(ConfigMetadata.Visibility.PUBLIC)
            .build());
    tree.addConfig(
        DO_NOTHING_MESSAGER,
        ConfigMetadata.builder()
            .className("OtherThingA")
            .packageName("com.bdl.config.others")
            .fieldName("otherFlag2")
            .type("java.lang.String")
            .visibility(ConfigMetadata.Visibility.PRIVATE)
            .build());
    tree.addConfig(
        DO_NOTHING_MESSAGER,
        ConfigMetadata.builder()
            .className("OtherThingA")
            .packageName("com.bdl.config.others")
            .fieldName("otherFlag3")
            .type("java.lang.String")
            .visibility(ConfigMetadata.Visibility.PACKAGE)
            .qualifier(Annotations.qualifier("").toString())
            .build());

    tree.pullPublicAndPrivateConfigsUp();

    final Map<String, Writer> writerMap = Maps.newHashMap();

    GuiceModuleFileWriterVisitor guiceVisitor = new GuiceModuleFileWriterVisitor(
        DO_NOTHING_MESSAGER, new Function<String, Writer>() {
      @Override
      public Writer apply(String input) {
        StringWriter writer = new StringWriter();
        writerMap.put(input, writer);
        return writer;
      }
    });

    tree.visit(guiceVisitor);
    for (Map.Entry<String, Writer> entry : writerMap.entrySet()) {
      URL resource = getClass().getClassLoader().getResource(entry.getKey() + ".txt");
      String file = Resources.toString(resource, Charsets.UTF_8);

      assertThat(entry.getValue().toString())
          .isEqualTo(file);
//      assertWithMessage(String.format("Mismatch in file %s", entry.getKey()))
//          .that(entry.getValue().toString())
//          .isEqualTo(file);
    }
  }
}