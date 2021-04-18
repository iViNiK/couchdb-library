package it.vinicioflamini.sharedlib.couchdb.ektorp;

import static it.vinicioflamini.sharedlib.couchdb.internal.AdapterUtils.toList;
import static it.vinicioflamini.sharedlib.couchdb.internal.AdapterUtils.transformViewResult;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.ektorp.CouchDbConnector;
import org.ektorp.DocumentNotFoundException;
import org.ektorp.DocumentOperationResult;
import org.ektorp.ViewQuery;
import org.ektorp.ViewResult.Row;
import org.ektorp.impl.NameConventions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.vinicioflamini.sharedlib.couchdb.api.CouchDbCrudRepository;
import it.vinicioflamini.sharedlib.couchdb.api.EntityInformation;
import it.vinicioflamini.sharedlib.couchdb.api.ViewParams;
import it.vinicioflamini.sharedlib.couchdb.api.ViewResult;
import it.vinicioflamini.sharedlib.couchdb.api.ViewResultRow;
import it.vinicioflamini.sharedlib.couchdb.api.exceptions.BulkOperationError;
import it.vinicioflamini.sharedlib.couchdb.api.exceptions.BulkOperationException;

/**
 * This implementation of {@link CouchDbCrudRepository} uses Ektorp's {@link CouchDbConnector}.
 * <p>
 * This implementation requires a special view in a design document. EXAMPLE: <code>
  "views" : {
    "by_id" : {
      "map" : "function(doc) { if(doc.type == '...') {emit(doc._id, { _id : doc._id, _rev: doc._rev } )} }",
      "reduce" : "_count"
    }
  }
 </code>
 * <p>
 * Take care when using {@link #deleteAll()} because it loads the IDs and revisions of all documents of the
 * above mentioned view at once, then deletes the documents.
 * 
 * @author Vinicio Flamini (io@vinicioflamini.it)
 */
public class EktorpCrudRepository<T, I extends Serializable> implements CouchDbCrudRepository<T, I> {

    protected Logger logger = LoggerFactory.getLogger(EktorpCrudRepository.class);

    protected EktorpCouchViewConfigurer viewBuilder = new EktorpCouchViewConfigurer();

    protected Class<T> type;

    protected EntityInformation<T, I> ei;

    protected boolean allOrNothing;

    protected CouchDbConnector db;

    protected ObjectMapper objectMapper;

    public EktorpCrudRepository(Class<T> type, CouchDbConnector db) {
        this(type, false, db, new ObjectMapper(), new EktorpEntityInformation<T, I>());
    }

    public EktorpCrudRepository(Class<T> type, CouchDbConnector db, EntityInformation<T, I> ei) {
        this(type, false, db, new ObjectMapper(), ei);
    }

    public EktorpCrudRepository(Class<T> type, boolean allOrNothing, CouchDbConnector db, ObjectMapper objectMapper,
            EntityInformation<T, I> ei) {
        super();
        this.type = type;
        this.ei = ei;
        this.allOrNothing = allOrNothing;
        this.db = db;
        this.objectMapper = objectMapper;
    }

    @Override
    public <S extends T> S save(S entity) {

        Assert.notNull(entity, "The given entity must not be null.");

        if (ei.isNew(entity)) {
            db.create(entity);
        } else {
            db.update(entity);
        }

        return entity; // Hint: the revision is already added resp. updated by Ektorp
    }

    @Override
    public <S extends T> Iterable<S> saveAll(Iterable<S> entities) {

        Assert.notNull(entities, "The given list of entities must not be null.");

        executeBulk(toList(entities));

        return entities; // Hint: the revision is already added resp. updated by Ektorp
    }

    @Override
    public Optional<T> findById(I id) {

        Assert.notNull(id, "The given ID must not be null.");

        try {
            return Optional.of(db.get(type, ei.toCouchId(id)));
        } catch (DocumentNotFoundException e) {
            logger.debug("document with ID " + id + " not found", e);
            return Optional.ofNullable(null);
        }
    }

    @Override
    public boolean existsById(I id) {

        Assert.notNull(id, "The given ID must not be null.");

        return db.contains(ei.toCouchId(id));
    }

    @Override
    public Iterable<T> findAll() {
        ViewQuery viewQuery = createQuery("by_id").reduce(false).includeDocs(true);
        return db.queryView(viewQuery, type);
    }

    @Override
    public Iterable<T> findAllById(Iterable<I> ids) {
        Assert.notNull(ids, "The given list of IDs must not be null.");

        ViewQuery q = createQuery("by_id").keys(ei.toCouchIds(ids)).reduce(false).includeDocs(true);
        return db.queryView(q, type);
    }

    @Override
    public long count() {
        ViewQuery viewQuery = createQuery("by_id").reduce(true);
        org.ektorp.ViewResult viewResult = db.queryView(viewQuery);
        if (viewResult.getRows().isEmpty()) {
            // there are no documents -> there is no sum
            return 0;
        } else {
            // the sum is in the returned row (there is only one row)
            return viewResult.getRows().get(0).getValueAsInt();
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void deleteById(I id) {

        Assert.notNull(id, "The given ID must not be null.");

        ViewQuery viewQuery = createQuery("by_id").reduce(false).includeDocs(false).key(ei.toCouchId(id));
        List<Map> results = db.queryView(viewQuery, Map.class);
        if (!results.isEmpty()) {
            db.delete((String) results.get(0).get("_id"), (String) results.get(0).get("_rev"));
        }
    }

    @Override
    public void delete(T entity) {

        Assert.notNull(entity, "The given entity must not be null.");

        db.delete(ei.getCouchId(entity), ei.getRev(entity));
    }

    @Override
    public void deleteAll(Iterable<? extends T> entities) {

        Assert.notNull(entities, "The given list of entities must not be null.");

        Collection<Map<String, Object>> collection = new ArrayList<>();
        for (T entity : entities) {
            collection.add(createBulkDeleteDocument(ei.getCouchId(entity), ei.getRev(entity)));
        }

        executeBulk(collection);
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void deleteAll() {

        ViewQuery viewQuery = createQuery("by_id").reduce(false).includeDocs(false);
        List<Map<String, Object>> results = ((List) db.queryView(viewQuery, HashMap.class));
        for (Map result : results) {
            result.put("_deleted", true);
        }

        executeBulk(results);
    }

    @Override
    public <R> R find(ViewParams viewParams) {

        ViewQuery viewQuery = new ViewQuery().dbPath(db.path());
        viewBuilder.configure(viewQuery, viewParams);
        if (viewQuery.getDesignDocId() == null) {
            viewQuery.designDocId(NameConventions.designDocName(type));
        }

        org.ektorp.ViewResult ektorpViewResult = db.queryView(viewQuery);

        ViewResult viewResult = toViewResult(ektorpViewResult, viewParams);

        return transformViewResult(viewResult, viewParams.getReturnType());
    }
    
    /**
     * Creates a ViewQuery 
     */
    protected ViewQuery createQuery(String viewName) {
        return new ViewQuery().dbPath(db.path()).designDocId(NameConventions.designDocName(type)).viewName(viewName);
    }

    /**
     * Transforms a Ektorp-specific view result to CouchRepository's view result.
     * 
     * @param viewResult
     *            the Ektorp-specific view result
     * @param viewParams
     *            the parameters inform about the return type
     * @return Returns the transformed view result.
     */
    protected ViewResult toViewResult(org.ektorp.ViewResult viewResult, ViewParams viewParams) {
        ViewResult result = new ViewResult();
        result.setOffset(result.getOffset());
        result.setTotalRows(viewResult.getTotalRows());
        result.setUpdateSeq(viewResult.getUpdateSeq());

        List<Row> rows = viewResult.getRows();
        List<ViewResultRow> resultRows = new ArrayList<>();
        for (Row row : rows) {
            ViewResultRow resultRow = new ViewResultRow();
            resultRow.setId(id(row));
            resultRow.setDoc(toObject(row.getDocAsNode(), viewParams.getDocumentType()));
            resultRow.setKey(toObject(row.getKeyAsNode(), viewParams.getKeyType()));
            resultRow.setValue(toObject(row.getValueAsNode(), viewParams.getValueType()));
            resultRows.add(resultRow);
        }

        result.setRows(resultRows);

        return result;
    }

    private String id(Row row) {
        try {
            return row.getId();
        } catch (NullPointerException e) {
            return null; // happens when reduce = true 
        }
    }

    protected Object toObject(JsonNode jsonNode, Class<?> valueType) {
        if (jsonNode == null || valueType == null) {
            return null;
        } else {
            try {
                return objectMapper.treeToValue(jsonNode, valueType);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }
    
    protected Map<String, Object> createBulkDeleteDocument(String id, String revision) {
        Map<String, Object> map = new HashMap<String, Object>(3);
        map.put("_id", id);
        map.put("_rev", revision);
        map.put("_deleted", true);
        return map;
    }

    protected void executeBulk(Collection<? extends Object> collection) {

        logger.debug(collection.size() + " documents are going to be processed ...");

        List<DocumentOperationResult> results;
        if (allOrNothing) {
            results = db.executeAllOrNothing(collection);
        } else {
            results = db.executeBulk(collection);
        }

        // handle errors
        List<BulkOperationError> errors = new ArrayList<>();
        List<String> messages = new ArrayList<>();
        for (DocumentOperationResult result : results) {
            if (result.getError() != null) {
                errors.add(new BulkOperationError(result.getId(), result.getRevision(), result.getError(), result
                        .getReason()));
                messages.add(result.toString());
            }
        }
        if (!errors.isEmpty()) {
            throw new BulkOperationException(StringUtils.join(messages, ","), errors);
        } else {
            logger.debug("All documents have been processed.");
        }
    }

}
