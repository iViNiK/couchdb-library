package it.vinicioflamini.sharedlib.couchdb.support;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Persistable;
import org.springframework.util.ReflectionUtils;

import it.vinicioflamini.sharedlib.couchdb.api.EntityInformation;

/**
 * This {@link EntityInformation} is only for entities with ID of type <i>String</i>.
 * <p>
 * It looks up the ID and the revision in the properties "_id", "id" resp. "_rev", "rev", "revision".
 * 
 * @author Vinicio Flamini (io@vinicioflamini.it)
 */
public class GenericEntityInformation<T, I extends Serializable> implements EntityInformation<T, I> {

    protected Class<T> type;

    protected Class<I> idType;

    public GenericEntityInformation(Class<T> type, Class<I> idType) {
        super();
        this.type = type;
        this.idType = idType;
    }

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
        List<String> list = new ArrayList<String>();
        for (I item : iter) {
            list.add(toCouchId(item));
        }
        return list;
    }


    @SuppressWarnings("unchecked")
    protected I toId(String couchId) {
        if (couchId == null) {
            return null;
        } else if (idType == String.class) {
            return (I) couchId;
        } else {
            throw new RuntimeException("unsupported type. Should you use another implementation of "
                    + EntityInformation.class + "?");
        }
    }
    
    @Override
    public String getCouchId(T entity) {
        return (String) getPropertyValue(entity, false, "_id", "id");
    }

    @Override
    public String getRev(T entity) {
        return (String) getPropertyValue(entity, false, "_rev", "rev", "revision");
    }

    @Override
    public void setId(T entity, String couchId) {
        setPropertyValue(entity, toId(couchId), "_id", "_id", "id");
    }

    @Override
    public void setRev(T entity, String rev) {
        setPropertyValue(entity, rev, "_rev", "_rev", "rev", "revision");
    }

    @SuppressWarnings("rawtypes")
    public boolean isNew(T entity) {
        if (entity instanceof Persistable) {
            return ((Persistable) entity).isNew();
        } else {
            return getRev(entity) == null;
        }
    }

    @SuppressWarnings("rawtypes")
    protected Object getPropertyValue(T entity, boolean exceptionOnMissingProperty, String... properties) {

        for (String property : properties) {
        	
            if (entity instanceof Map) {
                Map map = (Map) entity;
                if (map.containsKey(property)) {
                    return map.get(property);
                }
            }
            Field field = ReflectionUtils.findField(type, property);
            if (field != null) {
                ReflectionUtils.makeAccessible(field);
                return ReflectionUtils.getField(field, entity);
            }

        }

        if (exceptionOnMissingProperty) {
        	throw new RuntimeException("value for document properties " + properties + " not found");
        }
        else {
        	return null;
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected void setPropertyValue(T entity, Object newValue, String defaultPropertyInMap, String... properties) {

        for (String property : properties) {

            if (entity instanceof Map) {
                Map map = (Map) entity;
                if (map.containsKey(property)) {
                    map.put(property, newValue);
                    return;
                }
            }
            Field field = ReflectionUtils.findField(type, property);
            if (field != null) {
                ReflectionUtils.makeAccessible(field);
                ReflectionUtils.setField(field, entity, newValue);
                return;
            }

        }
        
        if (defaultPropertyInMap != null) {
            Map map = (Map) entity;
                map.put(defaultPropertyInMap, newValue);
        }
        else{
        	throw new RuntimeException("value for document properties " + properties + " not found");
        }

    }

}
