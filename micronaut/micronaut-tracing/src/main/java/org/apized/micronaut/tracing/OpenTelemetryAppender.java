package org.apized.micronaut.tracing;

import ch.qos.logback.classic.spi.ILoggingEvent;

public class OpenTelemetryAppender extends io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender {
  private boolean started = false;

  @Override
  public void start() {
    if (!inGraalImageBuildtimeCode()) {
      super.start();
      this.started = true;
    }
  }

  @Override
  public void doAppend(ILoggingEvent eventObject) {
    if (!inGraalImageBuildtimeCode()) {
      if (!started) start();
      super.doAppend(eventObject);
    }
  }

  //THE BELOW CODE CAN BE SUBSTITUTED BY ImageInfo.inImageBuildtimeCode() if you have it on your classpath

  private static final String PROPERTY_IMAGE_CODE_VALUE_BUILDTIME = "buildtime";
  private static final String PROPERTY_IMAGE_CODE_KEY = "org.graalvm.nativeimage.imagecode";
  /**
   * Returns true if (at the time of the call) code is executing in the context of Graal native image building
   * (e.g. in a static initializer of class that will be contained in the image).
   * Copy of graal code in org.graalvm.nativeimage.ImageInfo.inImageBuildtimeCode().
   * https://github.com/oracle/graal/blob/master/sdk/src/org.graalvm.nativeimage/src/org/graalvm/nativeimage/ImageInfo.java
   */
  private static boolean inGraalImageBuildtimeCode() {
    return PROPERTY_IMAGE_CODE_VALUE_BUILDTIME.equals(System.getProperty(PROPERTY_IMAGE_CODE_KEY));
  }
}
