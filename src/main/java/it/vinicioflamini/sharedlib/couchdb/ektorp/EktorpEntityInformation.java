package it.vinicioflamini.sharedlib.couchdb.ektorp;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.ektorp.util.Documents;

import it.vinicioflamini.sharedlib.couchdb.api.EntityInformation;

/**
 * This {@link EntityInformation} is only for entities with ID of type <i>String</i>.
 * 
 * @author Vinicio Flamini (io@vinicioflamini.it)
 */
public class EktorpEntityInformation<T, I extends Serializable> implements EntityInformation<T, I> {

    @Override
    public String toCouchId(I id) {
        if (id == null) {
            return null;
        } else if (id instanceof CharSequence) {
            return ((CharSequence) id).toString();
        } else {
            throw new RuntimeException("unsupported type. Should you use another implementation of "
                    + EntityInformation.class + "?");
        }
    }
    
    @Override
    public List<String> toCouchIds(Iterable<I> iter) {
        List<String> list = new ArrayList<>();
        for (I item : iter) {
            list.add(toCouchId(item));
        }
        return list;
    }

    @Override
    public String getCouchId(T entity) {
        return Documents.getId(entity);
    }
    
    @Override
    public String getRev(T entity) {
        return Documents.getRevision(entity);
    }

    @Override
    public void setId(T entity, String couchId) {
        throw new UnsupportedOperationException("not needed. therefore, not implemented");
    }

    @Override
    public void setRev(T entity, String rev) {
        throw new UnsupportedOperationException("not needed. therefore, not implemented");
    }

    public boolean isNew(T entity) {
        return Documents.isNew(entity);
    }

}
