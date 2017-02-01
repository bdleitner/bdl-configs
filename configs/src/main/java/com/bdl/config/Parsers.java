package com.bdl.config;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * Container class for Parsers that convert Strings into primitive or other simple known types.
 *
 * @author Ben Leitner
 */
class Parsers {

  private Parsers() {
    // container class, no instantiation.
  }

  public static final Function<String, Boolean> BOOLEAN_PARSER =
      new Function<String, Boolean>() {
        @Override
        public Boolean apply(String input) {
          String lower = input.toLowerCase();
          return lower.startsWith("t") || lower.startsWith("y") || lower.startsWith("1");
        }
      };

  public static final Function<String, Integer> INTEGER_PARSER =
      new Function<String, Integer>() {
        @Override
        public Integer apply(String input) {
          return Integer.parseInt(input);
        }
      };

  public static final Function<String, Long> LONG_PARSER =
      new Function<String, Long>() {
        @Override
        public Long apply(String input) {
          return Long.parseLong(input);
        }
      };

  public static final Function<String, Double> DOUBLE_PARSER =
      new Function<String, Double>() {
        @Override
        public Double apply(String input) {
          return Double.parseDouble(input);
        }
      };

  public static final Function<String, String> STRING_PARSER =
      new Function<String, String>() {
        @Override
        public String apply(String input) {
          return input;
        }
      };

  public static final Function<String, Class<?>> CLASS_PARSER =
      new Function<String, Class<?>>() {
        @Override
        public Class<?> apply(String input) {
          try {
            return Class.forName(input);
          } catch (ClassNotFoundException ex) {
            throw new IllegalStateException(
                String.format("Could not load class object for value %s", input), ex);
          }
        }
      };

  /** A parser for enum types */
  private static class EnumParser<T extends Enum> implements Function<String, T> {
    private final Class<T> clazz;

    private EnumParser(Class<T> clazz) {
      this.clazz = clazz;
    }

    @SuppressWarnings({"unchecked", "RedundantCast"}) // Should throw if not a match.
    @Override
    public T apply(String input) {
      return (T) Enum.valueOf(clazz, input.toUpperCase());
    }
  }

  private static final Map<Class<?>, Function<String, ?>> PARSER_MAP =
      ImmutableMap.<Class<?>, Function<String, ?>>builder()
          .put(Boolean.class, BOOLEAN_PARSER)
          .put(Integer.class, INTEGER_PARSER)
          .put(Long.class, LONG_PARSER)
          .put(Double.class, DOUBLE_PARSER)
          .put(String.class, STRING_PARSER)
          .put(Class.class, CLASS_PARSER)
          .build();

  @SuppressWarnings("unchecked") // Types known ok.
  public static <T> Function<String, T> forClass(Class<T> clazz) {
    if (Enum.class.isAssignableFrom(clazz)) {
      return (Function<String, T>) new EnumParser<>((Class<? extends Enum>) clazz);
    }
    return (Function<String, T>) PARSER_MAP.get(clazz);
  }
}
