package it.vinicioflamini.sharedlib.couchdb.api;

import java.io.Serializable;

import org.springframework.data.repository.CrudRepository;

/**
 * This {@link CrudRepository} for CouchDB databases allows you to query views.
 * 
 * @author Vinicio Flamini (io@vinicioflamini.it)
 * @param <T> See type parameter in {@link CrudRepository}
 * @param <I> See type parameter in {@link CrudRepository}
 */
public interface CouchDbCrudRepository<T, I extends Serializable> extends CrudRepository<T, I> {

    /**
     * Queries the database with the given parameters.
     * 
     * @param viewParams the query parameters
     * @param <R> the return type, depends on {@link ViewParams#getReturnType()}.
     * @return Returns the result of the query. The type of the return value depends on {@link ViewParams#getReturnType()}.
     */
    <R> R find(ViewParams viewParams);

}
