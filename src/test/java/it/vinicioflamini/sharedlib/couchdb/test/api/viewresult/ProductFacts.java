package it.vinicioflamini.sharedlib.couchdb.test.api.viewresult;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import it.vinicioflamini.sharedlib.couchdb.test.model.Product;
import it.vinicioflamini.sharedlib.couchdb.test.model.ProductRating;

/**
 * This class duplicates the properties of a {@link Product} and assists in testing views.
 * 
 * @author Vinicio Flamini (io@vinicioflamini.it)
 */
public class ProductFacts {

    private String docId;

    private Date lastModification;

    private String text;

    private ProductRating rating;

    private boolean hidden;

    private Integer numBuyers;

    private double weight;

    private BigDecimal price;

    private List<String> tags;

    private int isoProductCode;

    private String revision;
    
    private String manufacturerId;

    public Date getLastModification() {
        return lastModification;
    }

    public void setLastModification(Date lastModification) {
        this.lastModification = lastModification;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public ProductRating getRating() {
        return rating;
    }

    public void setRating(ProductRating rating) {
        this.rating = rating;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public Integer getNumBuyers() {
        return numBuyers;
    }

    public void setNumBuyers(Integer numBuyers) {
        this.numBuyers = numBuyers;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public int getIsoProductCode() {
        return isoProductCode;
    }

    public void setIsoProductCode(int isoProductCode) {
        this.isoProductCode = isoProductCode;
    }

    public String getDocId() {
        return docId;
    }

    public void setDocId(String docId) {
        this.docId = docId;
    }

    public String getRevision() {
        return revision;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }

	public String getManufacturerId() {
		return manufacturerId;
	}

	public void setManufacturerId(String manufacturerId) {
		this.manufacturerId = manufacturerId;
	}
}
