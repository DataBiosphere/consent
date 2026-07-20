/**
 * This package encompasses more complex transactions than are manageable using the standard DAO
 * interface pattern. Standard practice for all classes here should be to establish a transaction,
 * handle updates, then commit. Failures will trigger a rollback. Jdbi.useHandle/withHandle/etc
 * functions will auto-close any open connections. Classes in this package are plain Java
 * orchestration objects, NOT Jdbi SQL object interfaces; they must not be instantiated via
 * jdbi.onDemand().
 */
package org.broadinstitute.consent.http.service.dao;
