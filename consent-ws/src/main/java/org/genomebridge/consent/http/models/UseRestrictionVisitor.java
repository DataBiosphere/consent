package org.genomebridge.consent.http.models;

public interface UseRestrictionVisitor {

    public void startChildren();
    public void endChildren();

    public boolean visit(UseRestriction r);
}
