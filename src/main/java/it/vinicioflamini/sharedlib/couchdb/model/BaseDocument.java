package it.vinicioflamini.sharedlib.couchdb.model;

import org.ektorp.Attachment;
import org.ektorp.support.Entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

/**
 * @author Vinicio Flamini (io@vinicioflamini.it)
 */
@JsonInclude(Include.NON_NULL)
public class BaseDocument extends Entity {

	private static final long serialVersionUID = -7561585521343509072L;

	@SerializedName("_id")
    private String id;

    @SerializedName("_rev")
    private String revision;
    
    @Override
    @JsonProperty("_id")
    public String getId() {
        return id;
    }

    @Override
    @JsonProperty("_id")
    public void setId(String id) {
        this.id = id;
    }

    @Override
    @JsonProperty("_rev")
    public String getRevision() {
        return revision;
    }

    @Override
    @JsonProperty("_rev")
    public void setRevision(String revision) {
        this.revision = revision;
    }
    
    @Override
    public void addInlineAttachment(Attachment a) {
        super.addInlineAttachment(a);
    }   
    
    @Override
    public void removeAttachment(String attachmentId) {
        super.removeAttachment(attachmentId);
    }
    
    @Override
	public boolean equals(Object o) {
		if (o == this) return true;
		if (o instanceof Entity) {
			Entity a2 = (Entity) o;
			return getId() != null && getId().equals(a2.getId());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return getId() != null ? getId().hashCode() : super.hashCode();
	}
}
