package org.broadinstitute.consent.http.filters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.FeatureContext;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class UploadSizeLimitFeatureTest {

  private final UploadSizeLimitFeature feature = new UploadSizeLimitFeature(100);

  @UploadSizeLimited
  void defaultLimit() {}

  @UploadSizeLimited(maxBytes = 300)
  void ownLimit() {}

  void notAnUpload() {}

  @Test
  void testAnUploadGetsTheConfiguredLimit() throws Exception {
    assertEquals(100, registeredLimit("defaultLimit"));
  }

  // An endpoint can set its own limit.
  @Test
  void testAnUploadCanSetItsOwnLimit() throws Exception {
    assertEquals(300, registeredLimit("ownLimit"));
  }

  @Test
  void testOtherEndpointsAreNotBounded() throws Exception {
    FeatureContext context = mock(FeatureContext.class);

    feature.configure(resource("notAnUpload"), context);

    verify(context, never()).register(any(Object.class), anyInt());
  }

  private long registeredLimit(String method) throws Exception {
    FeatureContext context = mock(FeatureContext.class);
    feature.configure(resource(method), context);
    ArgumentCaptor<Object> filter = ArgumentCaptor.forClass(Object.class);
    verify(context).register(filter.capture(), eq(Priorities.USER));
    return ((UploadSizeLimitFilter) filter.getValue()).maxBytes();
  }

  private static ResourceInfo resource(String method) throws Exception {
    Method resourceMethod = UploadSizeLimitFeatureTest.class.getDeclaredMethod(method);
    ResourceInfo info = mock(ResourceInfo.class);
    when(info.getResourceMethod()).thenReturn(resourceMethod);
    return info;
  }
}
