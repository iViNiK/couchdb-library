package it.vinicioflamini.sharedlib.couchdb.test.api.viewresult;

/**
 * This class assists in testing views.
 * 
 * @author Vinicio Flamini (io@vinicioflamini.it)
 */
public class ProductSummary {

    private ProductFacts facts;

    public ProductFacts getFacts() {
        return facts;
    }

    public void setFacts(ProductFacts facts) {
        this.facts = facts;
    }
}
