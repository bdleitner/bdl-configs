package com.bdl.config.annotation.processor;

import static com.bdl.annotation.processing.model.TypeMetadata.simpleTypeParam;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import com.bdl.annotation.processing.model.AnnotationMetadata;
import com.bdl.annotation.processing.model.FieldMetadata;
import com.bdl.annotation.processing.model.Modifiers;
import com.bdl.annotation.processing.model.TypeMetadata;
import com.bdl.annotation.processing.model.ValueMetadata;
import com.bdl.annotation.processing.model.Visibility;
import com.bdl.config.Config;
import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.Map;
import javax.annotation.Nullable;
import javax.annotation.processing.Messager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for the combined functionality of {@link ConfigPackageTree} and {@link
 * DaggerModuleFileWriterVisitor}.
 *
 * TODO: Add a test for if a module is named "Module."  Appears to block the dagger.Module import.
 * @author Ben Leitner
 */
@RunWith(JUnit4.class)
public class DaggerModulesTest {

  private static final TypeMetadata CONFIG_TYPE = TypeMetadata.from(Config.class);
  private static final Messager DO_NOTHING_MESSAGER = new Auto_DaggerMessager_Impl();

  @Test
  public void testDaggerModuleOutput() throws IOException {
    ConfigPackageTree tree = new ConfigPackageTree();
    tree.addConfig(
        DO_NOTHING_MESSAGER,
        ConfigMetadata.builder()
            .field(
                FieldMetadata.builder()
                    .containingClass(
                        TypeMetadata.builder()
                            .setPackageName("com.bdl.config.things")
                            .setName("Thing1")
                            .addParam(simpleTypeParam("T"))
                            .build())
                    .name("flag1")
                    .type(configOf(TypeMetadata.STRING))
                    .modifiers(Modifiers.visibility(Visibility.PACKAGE_LOCAL))
                    .build())
            .configAnnotation(
                AnnotationMetadata.builder()
                    .setType(CONFIG_TYPE)
                    .putValue("name", ValueMetadata.create("alternate_name"))
                    .build())
            .hasDefault(true)
            .build());
    tree.addConfig(
        DO_NOTHING_MESSAGER,
        ConfigMetadata.builder()
            .field(
                FieldMetadata.builder()
                    .containingClass(
                        TypeMetadata.builder()
                            .setPackageName("com.bdl.config.things")
                            .setName("Thing2")
                            .build())
                    .name("flag2")
                    .type(configOf(TypeMetadata.STRING))
                    .modifiers(Modifiers.visibility(Visibility.PUBLIC))
                    .build())
            .configAnnotation(AnnotationMetadata.builder().setType(CONFIG_TYPE).build())
            .qualifier(
                AnnotationMetadata.builder()
                    .setType(TypeMetadata.from(DaggerAnnotations.DummyQualifier.class))
                    .putValue("value", ValueMetadata.create("flag2"))
                    .build())
            .hasDefault(false)
            .build());
    tree.addConfig(
        DO_NOTHING_MESSAGER,
        ConfigMetadata.builder()
            .field(
                FieldMetadata.builder()
                    .containingClass(
                        TypeMetadata.builder()
                            .setPackageName("com.bdl.config.others")
                            .setName("OtherThingA")
                            .build())
                    .name("otherFlag1")
                    .type(configOf(TypeMetadata.STRING))
                    .modifiers(Modifiers.visibility(Visibility.PUBLIC))
                    .build())
            .configAnnotation(AnnotationMetadata.builder().setType(CONFIG_TYPE).build())
            .hasDefault(true)
            .build());
    tree.addConfig(
        DO_NOTHING_MESSAGER,
        ConfigMetadata.builder()
            .field(
                FieldMetadata.builder()
                    .containingClass(
                        TypeMetadata.builder()
                            .setPackageName("com.bdl.config.others")
                            .setName("OtherThingA")
                            .build())
                    .name("otherFlag2")
                    .type(configOf(TypeMetadata.STRING))
                    .modifiers(Modifiers.visibility(Visibility.PRIVATE))
                    .build())
            .configAnnotation(AnnotationMetadata.builder().setType(CONFIG_TYPE).build())
            .hasDefault(true)
            .build());
    tree.addConfig(
        DO_NOTHING_MESSAGER,
        ConfigMetadata.builder()
            .field(
                FieldMetadata.builder()
                    .containingClass(
                        TypeMetadata.builder()
                            .setPackageName("com.bdl.config.others")
                            .setName("OtherThingA")
                            .build())
                    .name("otherFlag3")
                    .type(configOf(TypeMetadata.STRING))
                    .modifiers(Modifiers.visibility(Visibility.PACKAGE_LOCAL))
                    .build())
            .configAnnotation(AnnotationMetadata.builder().setType(CONFIG_TYPE).build())
            .qualifier(
                AnnotationMetadata.builder()
                    .setType(TypeMetadata.from(DaggerAnnotations.DummyQualifier.class))
                    .build())
            .hasDefault(true)
            .build());

    tree.pullPublicAndPrivateConfigsUp();

    final Map<String, Writer> writerMap = Maps.newHashMap();

    DaggerModuleFileWriterVisitor daggerVisitor =
        new DaggerModuleFileWriterVisitor(
            DO_NOTHING_MESSAGER,
            new Function<String, Writer>() {
              @Override
              public Writer apply(@Nullable String input) {
                StringWriter writer = new StringWriter();
                writerMap.put(input + ".txt", writer);
                return writer;
              }
            });

    tree.visit(daggerVisitor);
    for (Map.Entry<String, Writer> entry : writerMap.entrySet()) {
      URL resource =
          Preconditions.checkNotNull(
              getClass().getClassLoader().getResource(entry.getKey()),
              "Unable to locate resource: %s",
              entry.getKey());
      String file = Resources.toString(resource, Charsets.UTF_8);
      assertWithMessage(String.format("file: %s", entry.getKey()))
          .that(normalize(entry.getValue().toString()))
          .isEqualTo(normalize(file));
    }
  }

  /**
   * Tests that a module will be written to include submodules in different subpackages even when no
   * configs are present in the common-prefix package.
   */
  @Test
  public void testWriteModulesAtJoinNodes() throws Exception {
    ConfigPackageTree tree = new ConfigPackageTree();
    tree.addConfig(
        DO_NOTHING_MESSAGER,
        ConfigMetadata.builder()
            .field(
                FieldMetadata.builder()
                    .containingClass(
                        TypeMetadata.builder()
                            .setPackageName("com.bdl.config.alllocal.sub1")
                            .setName("Local")
                            .build())
                    .name("sub1")
                    .type(configOf(TypeMetadata.BOXED_INTEGER))
                    .modifiers(Modifiers.visibility(Visibility.PACKAGE_LOCAL))
                    .build())
            .configAnnotation(AnnotationMetadata.builder().setType(CONFIG_TYPE).build())
            .hasDefault(true)
            .build());
    tree.addConfig(
        DO_NOTHING_MESSAGER,
        ConfigMetadata.builder()
            .field(
                FieldMetadata.builder()
                    .containingClass(
                        TypeMetadata.builder()
                            .setPackageName("com.bdl.config.alllocal.sub2.sub")
                            .setName("Local")
                            .build())
                    .name("sub2")
                    .type(configOf(TypeMetadata.STRING))
                    .modifiers(Modifiers.visibility(Visibility.PACKAGE_LOCAL))
                    .build())
            .configAnnotation(AnnotationMetadata.builder().setType(CONFIG_TYPE).build())
            .hasDefault(true)
            .build());

    tree.pullPublicAndPrivateConfigsUp();

    final Map<String, Writer> writerMap = Maps.newHashMap();

    DaggerModuleFileWriterVisitor daggerVisitor =
        new DaggerModuleFileWriterVisitor(
            DO_NOTHING_MESSAGER,
            new Function<String, Writer>() {
              @Override
              public Writer apply(@Nullable String input) {
                StringWriter writer = new StringWriter();
                writerMap.put(input + ".txt", writer);
                return writer;
              }
            });

    tree.visit(daggerVisitor);
    assertThat(writerMap.keySet())
        .containsExactly(
            "com.bdl.config.alllocal.ConfigDaggerModule.txt",
            "com.bdl.config.alllocal.sub1.ConfigDaggerModule.txt",
            "com.bdl.config.alllocal.sub2.sub.ConfigDaggerModule.txt");

    for (Map.Entry<String, Writer> entry : writerMap.entrySet()) {
      URL resource =
          Preconditions.checkNotNull(
              getClass().getClassLoader().getResource(entry.getKey()),
              "Unable to locate resource: %s",
              entry.getKey());
      String file = Resources.toString(resource, Charsets.UTF_8);

      assertWithMessage(String.format("file: %s", entry.getKey()))
          .that(normalize(entry.getValue().toString()))
          .isEqualTo(normalize(file));
    }
  }

  private static TypeMetadata configOf(TypeMetadata type) {
    return TypeMetadata.builder()
        .setPackageName("com.bdl.config")
        .setName("Configurable")
        .addParam(type)
        .build();
  }

  private static String normalize(String input) {
    return input.replace("\r\n", "\n");
  }
}
