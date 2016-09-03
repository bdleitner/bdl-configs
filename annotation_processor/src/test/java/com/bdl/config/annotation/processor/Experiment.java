package com.bdl.config.annotation.processor;

/**
 * TODO: JavaDoc this class.
 *
 * @author Ben Leitner
 */
public class Experiment {
  public static void main(String[] args) throws Exception {
    com.sun.tools.javac.Main.main(new String[] {"-proc:only",
        "-processor", "com.bdl.config.annotation.processor.ConfigAnnotationProcessor",
        "C:\\projects\\java\\bdl-configs\\annotation_processor\\src\\test\\java\\com\\bdl\\config\\annotation\\processor\\Target.java"});
  }
}
