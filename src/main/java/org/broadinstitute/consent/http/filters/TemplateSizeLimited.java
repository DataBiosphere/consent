package org.broadinstitute.consent.http.filters;

import jakarta.ws.rs.NameBinding;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Binds {@link TemplateSizeLimitFilter} to a template upload endpoint, so the request is bounded
 * before Jersey reads the body rather than after. Carried by the filter as well as by the methods
 * it guards, which is what makes the binding take effect.
 */
@NameBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface TemplateSizeLimited {}
