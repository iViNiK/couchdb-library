package it.vinicioflamini.sharedlib.couchdb.internal;

import java.util.ArrayList;
import java.util.List;

import it.vinicioflamini.sharedlib.couchdb.api.ViewResult;
import it.vinicioflamini.sharedlib.couchdb.api.ViewResultRow;

/**
 * Transforms Spring Data specific values to CouchDB driver specific values and the other way around. 
 * 
 * @author Vinicio Flamini (io@vinicioflamini.it)
 */
public class AdapterUtils {

	private AdapterUtils() {}
	
    /**
     * Copies the elements of {@link Iterable} into a list and returns them.
     * 
     * @param iter the iterable that contains the elements
     * @param <E> the type of the elements
     * @return Returns the elements of the {@link Iterable}.
     */
    public static <E> List<E> toList(Iterable<E> iter) {
        List<E> list = new ArrayList<>();
        for (E item : iter) {
            list.add(item);
        }
        return list;
    }

    /**
     * Transforms the view result to a list of keys or values or documents or IDs depending on the given return type. If
     * the return type is null, the view result is not transformed.
     * 
     * @param viewResult the original view result
     * @param returnType
     *            null or "key or "value" or "doc" or "id"
     * @param <E> the type of the elements
     * @return Returns the transformed view result.
     */
    @SuppressWarnings("unchecked")
    public static <E> E transformViewResult(ViewResult viewResult, String returnType) {

        if (returnType == null) {
            return (E) viewResult;
        } else {
            List<Object> list = new ArrayList<>();
            for (ViewResultRow row : viewResult.getRows()) {

                Object obj;
                if (returnType.equals("key")) {
                    obj = row.getKey();
                } else if (returnType.equals("value")) {
                    obj = row.getValue();
                } else if (returnType.equals("doc")) {
                    obj = row.getDoc();
                } else if (returnType.equals("id")) {
                    obj = row.getId();
                } else {
                    throw new IllegalArgumentException("not supported return type: " + returnType);
                }
                list.add(obj);
            }
            return (E) list;
        }
    }

}
