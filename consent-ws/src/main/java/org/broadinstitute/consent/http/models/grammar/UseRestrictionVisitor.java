package org.broadinstitute.consent.http.models.grammar;

public interface UseRestrictionVisitor {

    void startChildren();

    void endChildren();

    boolean visit(UseRestriction r);

}
