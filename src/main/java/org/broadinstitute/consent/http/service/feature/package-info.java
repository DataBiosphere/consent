/**
 * This package encompasses small feature-specific service functions that might be used by other
 * services. To keep these features compact and isolated, they are implemented in their own service
 * classes. Standard practice for all classes here should be to minimize the surface area and
 * dependencies of the feature, and to avoid introducing circular dependencies between services. The
 * best way to achieve this is to ensure that features in this package do not depend on services and
 * instead only depend on DAO classes
 */
package org.broadinstitute.consent.http.service.feature;
