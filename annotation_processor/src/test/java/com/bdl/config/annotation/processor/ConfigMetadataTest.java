package com.bdl.config.annotation.processor;

import static com.google.common.truth.Truth.assertThat;

import com.google.testing.compile.CompilationRule;

import com.bdl.annotation.processing.model.FieldMetadata;
import com.bdl.annotation.processing.model.TypeMetadata;
import com.bdl.annotation.processing.model.Visibility;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

/**
 * Tests for the ConfigMetadata class.
 *
 * @author Ben Leitner
 */
@RunWith(JUnit4.class)
public class ConfigMetadataTest {

  private static final TypeMetadata CONFIG_CONTAINER_TYPE = TypeMetadata.builder()
      .setPackageName("com.bdl.config.annotation.processor")
      .setName("ConfigContainer")
      .build();

  @Rule public final CompilationRule compilation = new CompilationRule();

  private Elements elements;

  @Before
  public void before() {
    elements = compilation.getElements();
  }


  private Element elementForField(String fieldName) {
    TypeElement container = elements.getTypeElement("com.bdl.config.annotation.processor.ConfigContainer");
    for (Element element : container.getEnclosedElements()) {
      if (element.getKind() == ElementKind.FIELD
          && element.getSimpleName().toString().equals(fieldName)) {
        return element;
      }
    }
    throw new IllegalArgumentException(String.format("No such field: %s", fieldName));
  }

  @Test
  public void testFromRealField_privateString() {
    ConfigMetadata config = ConfigMetadata.from(elements, FieldMetadata.from(elementForField("privateString")));
    assertThat(config).isEqualTo(
        ConfigMetadata.builder()
            .field(FieldMetadata.builder()
                .containingClass(CONFIG_CONTAINER_TYPE)
                .visibility(Visibility.PRIVATE)
                .isStatic(true)
                .isFinal(true)
                .type(TypeUtils.configOf(TypeMetadata.STRING))
                .name("privateString")
                .addAnnotation(Annotations.configMetadata())
                .build())
            .configAnnotation(Annotations.configMetadata())
            .build());
    assertThat(config.name()).isEqualTo("privateString");
  }

  @Test
  public void testFromRealField_packageInteger() {
    ConfigMetadata config = ConfigMetadata.from(elements, FieldMetadata.from(elementForField("packageInteger")));
    assertThat(config).isEqualTo(
        ConfigMetadata.builder()
            .field(FieldMetadata.builder()
                .containingClass(CONFIG_CONTAINER_TYPE)
                .visibility(Visibility.PACKAGE_LOCAL)
                .isStatic(true)
                .isFinal(true)
                .type(TypeUtils.configOf(TypeMetadata.BOXED_INTEGER))
                .name("packageInteger")
                .addAnnotation(Annotations.configMetadata("specified_name"))
                .addAnnotation(Annotations.qualifierMetadata("foo"))
                .build())
            .configAnnotation(Annotations.configMetadata("specified_name"))
            .qualifier(Annotations.qualifierMetadata("foo"))
            .build());
    assertThat(config.name()).isEqualTo("specified_name");
  }

  @Test
  public void testFromRealField_publicBoolean() {
    ConfigMetadata config = ConfigMetadata.from(elements, FieldMetadata.from(elementForField("publicBoolean")));
    assertThat(config).isEqualTo(
        ConfigMetadata.builder()
            .field(FieldMetadata.builder()
                .containingClass(CONFIG_CONTAINER_TYPE)
                .visibility(Visibility.PUBLIC)
                .isStatic(true)
                .isFinal(true)
                .type(TypeUtils.configOf(TypeMetadata.BOXED_BOOLEAN))
                .name("publicBoolean")
                .addAnnotation(Annotations.configMetadata())
                .addAnnotation(Annotations.qualifierMetadata(null))
                .addAnnotation(Annotations.bindingAnnotationMetadata("blah"))
                .build())
            .configAnnotation(Annotations.configMetadata())
            .qualifier(Annotations.qualifierMetadata(null))
            .bindingAnnotation(Annotations.bindingAnnotationMetadata("blah"))
            .build());
    assertThat(config.name()).isEqualTo("publicBoolean");
  }
}
