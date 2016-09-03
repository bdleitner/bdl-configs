package com.bdl.config.annotation.processor;

import static com.google.common.truth.Truth.assertWithMessage;

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

/**
 * Tests for the combined functionality of {@link ConfigPackageTree} and {@link DaggerModuleFile}.
 *
 * @author Ben Leitner
 */
@RunWith(JUnit4.class)
public class DaggerModulesTest {

  @Test
  public void testDaggerModuleOutput() throws IOException {
    ConfigPackageTree tree = new ConfigPackageTree();
    tree.addConfig(
        ConfigMetadata.builder()
            .className("Thing1")
            .packageName("com.bdl.config.things")
            .fieldName("flag1")
            .type("java.lang.String")
            .visibility(ConfigMetadata.Visibility.PACKAGE)
            .build());
    tree.addConfig(
        ConfigMetadata.builder()
            .className("Thing2")
            .packageName("com.bdl.config.things")
            .fieldName("flag2")
            .type("java.lang.String")
            .visibility(ConfigMetadata.Visibility.PUBLIC)
            .qualifier(Annotations.qualifier("flag2"))
            .build());
    tree.addConfig(
        ConfigMetadata.builder()
            .className("OtherThingA")
            .packageName("com.bdl.config.others")
            .fieldName("otherFlag1")
            .type("java.lang.String")
            .visibility(ConfigMetadata.Visibility.PUBLIC)
            .build());
    tree.addConfig(
        ConfigMetadata.builder()
            .className("OtherThingA")
            .packageName("com.bdl.config.others")
            .fieldName("otherFlag2")
            .type("java.lang.String")
            .visibility(ConfigMetadata.Visibility.PRIVATE)
            .build());
    tree.addConfig(
        ConfigMetadata.builder()
            .className("OtherThingA")
            .packageName("com.bdl.config.others")
            .fieldName("otherFlag3")
            .type("java.lang.String")
            .visibility(ConfigMetadata.Visibility.PACKAGE)
            .qualifier(Annotations.qualifier(""))
            .build());

    tree.pushPublicAndPrivateConfigsDown();

    DaggerModuleFile rootFile = tree.toModuleFile();

    final Map<String, Writer>  writerMap = Maps.newHashMap();

    rootFile.write(new Function<String, Writer>() {
      @Override
      public Writer apply(String input) {
        StringWriter writer = new StringWriter();
        writerMap.put(input, writer);
        return writer;
      }
    });

    for (Map.Entry<String, Writer> entry : writerMap.entrySet()) {
      URL resource = getClass().getClassLoader().getResource(entry.getKey() + ".txt");
      String file = Resources.toString(resource, Charsets.UTF_8);

      assertWithMessage(String.format("Mismatch in file %s", entry.getKey()))
          .that(entry.getValue().toString())
          .isEqualTo(file);
    }
  }
}
