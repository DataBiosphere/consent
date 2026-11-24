package org.broadinstitute.consent.http.filters;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Bounds an upload endpoint's request before Jersey reads the body ({@link
 * UploadSizeLimitFeature}), so an oversized request never reaches temporary disk.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface UploadSizeLimited {

  /** This endpoint's limit in bytes; 0 uses the configured {@code uploads.maxRequestBytes}. */
  long maxBytes() default 0;
}
