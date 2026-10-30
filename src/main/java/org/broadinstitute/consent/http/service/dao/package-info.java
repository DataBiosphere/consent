/**
 * This package encompasses more complex transactions than are manageable using the standard DAO
 * interface pattern. Run each multi-statement write in Jdbi.useTransaction/inTransaction, which
 * commits on success and rolls back on any exception; never turn off auto-commit on the connection
 * directly, because Jdbi does not then roll back. Classes in this package are plain Java
 * orchestration objects, NOT Jdbi SQL object interfaces; they must not be instantiated via
 * jdbi.onDemand().
 */
package org.broadinstitute.consent.http.service.dao;
