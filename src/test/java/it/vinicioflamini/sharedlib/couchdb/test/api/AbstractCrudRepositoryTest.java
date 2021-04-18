package it.vinicioflamini.sharedlib.couchdb.test.api;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static it.vinicioflamini.sharedlib.couchdb.internal.AdapterUtils.toList;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.MathContext;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.ektorp.Attachment;
import org.ektorp.AttachmentInputStream;
import org.ektorp.CouchDbConnector;
import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import it.vinicioflamini.sharedlib.couchdb.api.CouchDbCrudRepository;
import it.vinicioflamini.sharedlib.couchdb.api.ViewParams;
import it.vinicioflamini.sharedlib.couchdb.api.ViewResult;
import it.vinicioflamini.sharedlib.couchdb.api.ViewResultRow;
import it.vinicioflamini.sharedlib.couchdb.api.exceptions.BulkOperationException;
import it.vinicioflamini.sharedlib.couchdb.model.BaseDocument;
import it.vinicioflamini.sharedlib.couchdb.test.api.viewresult.ProductSummary;
import it.vinicioflamini.sharedlib.couchdb.test.model.Product;
import it.vinicioflamini.sharedlib.couchdb.test.model.ProductRating;

/**
 * Tests implementations of {@link CrudRepository}.
 * 
 * @author Vinicio Flamini (io@vinicioflamini.it)
 */
public abstract class AbstractCrudRepositoryTest extends AbstractJUnit4SpringContextTests {

	@Autowired
	private CouchDbConnector db;
	
    @Autowired
    private CouchDbCrudRepository<Product, String> productRepository;

    private Product p1 = newProduct("Tavolo 1", "Lumberjack1 Inc.");
    private Product p2 = newProduct("Tavolo 2", "Lumberjack1 Inc.");
    private Product p3 = newProduct("Tavolo 3", "Lumberjack Inc.");
    
    private ViewParams params = new ViewParams();
    
    @Test
    public void testSaveAndFindOneAndDeletebyEntityAndExists() throws Exception {
    	productRepository.deleteAll();
        
    	// given
        Product newProduct = newProduct("Tavolo 1", "Lumberjack Inc.");

        // when
        Optional<Product> existingProduct = productRepository.findById(newProduct.getId());
        if (existingProduct.isPresent()) {
            productRepository.delete(existingProduct.get());
        }

        // then
        assertFalse(productRepository.findById(newProduct.getId()).isPresent());

        // when
        productRepository.save(newProduct);

        // then
        assertTrue(productRepository.existsById(newProduct.getId()));
        Optional<Product> oFoundProduct = productRepository.findById(newProduct.getId());
        assertTrue(oFoundProduct.isPresent());
        Product foundProduct = oFoundProduct.get(); 
        assertEquals(newProduct.isHidden(), foundProduct.isHidden());
        assertEquals(newProduct.getId(), foundProduct.getId());
        assertEquals(newProduct.getIsoProductCode(), foundProduct.getIsoProductCode());
        assertEquals(newProduct.getLastModification(), foundProduct.getLastModification());
        assertEquals(newProduct.getManufacturerId(), foundProduct.getManufacturerId());
        assertEquals(newProduct.getNumBuyers(), foundProduct.getNumBuyers());
        assertEquals(newProduct.getPrice().doubleValue(), foundProduct.getPrice().doubleValue(), 0.001);
        assertEquals(newProduct.getRating(), foundProduct.getRating());
        assertEquals(newProduct.getTags(), foundProduct.getTags());
        assertEquals(newProduct.getText(), foundProduct.getText());
        assertEquals(newProduct.getWeight(), foundProduct.getWeight(), 0.0001);

        // when
        productRepository.delete(foundProduct);

        // then
        assertFalse(productRepository.existsById(newProduct.getId()));
        assertFalse(productRepository.findById(foundProduct.getId()).isPresent());
    }

    @Test
    public void testSaveIterableAndFindIterableAndDeleteIterable() throws Exception {
    	productRepository.deleteAll();
        
        // given
        Product newProduct1 = newProduct("Tavolo 2", "Lumberjack Inc.");
        Product newProduct2 = newProduct("Tavolo 3", "Lumberjack_1 Inc.");
        Product newProduct3 = newProduct("Tavolo 4", "Lumberjack Inc.");
        productRepository.saveAll(asList(newProduct1, newProduct2, newProduct3));

        // when
        List<String> productIds = asList(newProduct1.getId(), newProduct2.getId());
        List<Product> foundProducts = toList(productRepository.findAllById(productIds));

        // then
        assertEqualsIdSet(productIds, foundProducts);

        // when
        productRepository.deleteAll(asList(foundProducts.get(0), foundProducts.get(1)));

        // then
        assertEqualsIdSet(singleton(newProduct3.getId()), toList(productRepository.findAll()));
    }

    @Test
    public void testSaveIterableErrorOnBulkOperation() throws Exception {
    	productRepository.deleteAll();
        
        // given
        Product newProduct1 = newProduct("Tavolo 5", "Lumberjack Inc.");
        Product newProduct2 = newProduct("Tavolo 6", "Lumberjack_1 Inc.");
        Product newProduct3 = newProduct("Tavolo 7", "Lumberjack Inc.");
        
        productRepository.saveAll(asList(newProduct1, newProduct2, newProduct3));
        
        String oldRevision2 = newProduct2.getRevision(); 
        String oldRevision3 = newProduct3.getRevision(); 
        newProduct2.setHidden(!newProduct2.isHidden());
        newProduct3.setHidden(!newProduct3.isHidden());

        productRepository.saveAll(asList(newProduct1, newProduct2, newProduct3));

        // when
        newProduct2.setRevision(oldRevision2);
        newProduct3.setRevision(oldRevision3);
        catchException(productRepository).saveAll(asList(newProduct1, newProduct2, newProduct3));
        assertTrue(caughtException() instanceof BulkOperationException );
        BulkOperationException exception = caughtException();
        assertEquals(2, exception.getErrors().size());
    }

    @Test
    public void testDeleteById() throws Exception {
    	productRepository.deleteAll();
        
        // given
        Product newProduct = newProduct("Tavolo 8", "Lumberjack Inc.");
        productRepository.save(newProduct);

        assertTrue(productRepository.existsById(newProduct.getId()));

        // when
        productRepository.deleteById(newProduct.getId());

        // then
        assertFalse(productRepository.existsById(newProduct.getId()));
    }

    @Test
    public void testSaveNewDocumentVsUpdatedDocument() throws Exception {
    	productRepository.deleteAll();
        
        // given
        Product newProduct = newProduct(null, "Lumberjack Inc.");

        // when
        productRepository.save(newProduct);
        
        // then
        assertNotNull(newProduct.getId());
        assertNotNull(newProduct.getRevision());

        // when
        String oldRevision = newProduct.getRevision(); 
        newProduct.setHidden(!newProduct.isHidden());
        productRepository.save(newProduct);
        
        // then
        assertNotEquals(oldRevision, newProduct.getRevision());
    }

    @Test
    public void testSaveWithAttachAndFindOneAndDeleteAttachmentAndExists() throws Exception  {
    	productRepository.deleteAll();
        
       // given
        Product newProduct = newProduct("Tavolo 9", "Lumberjack Inc.");

        // when
        Base64.Encoder enc = Base64.getEncoder();
        
        byte[] inputFile1 = Files.readAllBytes(Paths.get(getClass().getResource("../attachments/logo.jpg").toURI()));
        Attachment attachment = new Attachment("attachment-name-1", enc.encodeToString(inputFile1), "image/jpeg");
        newProduct.addInlineAttachment(attachment);
        
        // then
        assertFalse(productRepository.findById(newProduct.getId()).isPresent());

        // when
        productRepository.save(newProduct);

        // then
        assertTrue(productRepository.existsById(newProduct.getId()));
        Optional<Product> oFoundProduct = productRepository.findById(newProduct.getId());
        assertTrue(oFoundProduct.isPresent());
        Product foundProduct = oFoundProduct.get(); 
        assertEquals(newProduct.isHidden(), foundProduct.isHidden());
        assertEquals(newProduct.getId(), foundProduct.getId());
        assertEquals(newProduct.getIsoProductCode(), foundProduct.getIsoProductCode());
        assertEquals(newProduct.getLastModification(), foundProduct.getLastModification());
        assertEquals(newProduct.getManufacturerId(), foundProduct.getManufacturerId());
        assertEquals(newProduct.getNumBuyers(), foundProduct.getNumBuyers());
        assertEquals(newProduct.getPrice().doubleValue(), foundProduct.getPrice().doubleValue(), 0.001);
        assertEquals(newProduct.getRating(), foundProduct.getRating());
        assertEquals(newProduct.getTags(), foundProduct.getTags());
        assertEquals(newProduct.getText(), foundProduct.getText());
        assertEquals(newProduct.getWeight(), foundProduct.getWeight(), 0.0001);
        assertEquals(foundProduct.getAttachments().size(), 1);
        
        AttachmentInputStream ais = db.getAttachment(foundProduct.getId(), "attachment-name-1");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        IOUtils.copy(ais, outputStream);
        ais.close();
        assertEquals(outputStream.toString(), new String(inputFile1));
        
        //when
        byte[] inputFile2 = Files.readAllBytes(Paths.get(getClass().getResource("../attachments/doc.pdf").toURI()));
        attachment = new Attachment("attachment-name-2", enc.encodeToString(inputFile2), "application/pdf");
        foundProduct.addInlineAttachment(attachment);
        productRepository.save(foundProduct);
        
        //then
        assertTrue(productRepository.existsById(foundProduct.getId()));
        oFoundProduct = productRepository.findById(foundProduct.getId());
        assertTrue(oFoundProduct.isPresent());
        Product updatedProduct = oFoundProduct.get();
        assertEquals(foundProduct.getAttachments().size(), 2);
        
        ais = db.getAttachment(foundProduct.getId(), "attachment-name-1");
        outputStream = new ByteArrayOutputStream();
        IOUtils.copy(ais, outputStream);
        ais.close();
        assertEquals(outputStream.toString(), new String(inputFile1));

        ais = db.getAttachment(foundProduct.getId(), "attachment-name-2");
        outputStream = new ByteArrayOutputStream();
        IOUtils.copy(ais, outputStream);
        ais.close();
        assertEquals(outputStream.toString(), new String(inputFile2));
        
        updatedProduct.removeAttachment("attachment-name-1");
        assertNull(updatedProduct.getAttachments().get("attachment-name-1"));

    }

    @Test
    public void testCompleteResult() throws Exception {
        deleteProductRepoAndCreateSomeProducts();

        params.setKey("Lumberjack Inc.");

        ViewResult viewResults = productRepository.find(params);
        assertEquals(1, viewResults.getRows().size());
        ProductSummary summary = viewResults.getRows().get(0).getValue();
        assertEquals(p3.getId(), summary.getFacts().getDocId());
        assertEquals(p3.getIsoProductCode(), summary.getFacts().getIsoProductCode());
        assertEquals(p3.getLastModification(), summary.getFacts().getLastModification());
        assertEquals(p3.getNumBuyers(), summary.getFacts().getNumBuyers());
        assertEquals(p3.getPrice().doubleValue(), summary.getFacts().getPrice().doubleValue(), 0.0001);
        assertEquals(p3.getRating(), summary.getFacts().getRating());
        assertEquals(p3.getRevision(), summary.getFacts().getRevision());
        assertEquals(p3.getTags(), summary.getFacts().getTags());
        assertEquals(p3.getText(), summary.getFacts().getText());
        assertEquals(p3.getWeight(), summary.getFacts().getWeight(), 0.00001);
        assertEquals(p3.getManufacturerId(), summary.getFacts().getManufacturerId());
        assertNotEquals(p1.getManufacturerId(), summary.getFacts().getManufacturerId());
    }

    @Test
    public void testSetKey() throws Exception {
        deleteProductRepoAndCreateSomeProducts();

        params.setKey("Lumberjack1 Inc.");

        ViewResult summaries = productRepository.find(params);
        assertEquals(2, summaries.getRows().size());

        List<String> docIds = toDocIds(summaries.getRows());
        List<String> expectedDocsIds = Arrays.asList(p1.getId(), p2.getId());
        assertEquals(new HashSet<String>(expectedDocsIds), new HashSet<String>(docIds));
    }


    @Test
    public void testSetReduce() throws Exception {
        deleteProductRepoAndCreateSomeProducts();

        params.setView("by_manufacturerId");
        params.setReduce(true);
        params.setKeyType(String.class); // any
        params.setValueType(Integer.class);

        ViewResult summaries = productRepository.find(params);
        assertEquals(3, (int) summaries.getRows().get(0).getValue());

    }

    @Test
    public void testSetReturnValueToKey() throws Exception {
        deleteProductRepoAndCreateSomeProducts();

        params.setKey("Lumberjack1 Inc.");
        params.setReturnType("key");

        List<String> keys = productRepository.find(params);
        assertEquals(2, keys.size());
        assertEquals("Lumberjack1 Inc.", keys.get(0));
        assertEquals("Lumberjack1 Inc.", keys.get(1));
    }

    @Test
    public void testSetReturnValueToId() throws Exception {
        deleteProductRepoAndCreateSomeProducts();

        params.setKey("Lumberjack1 Inc.");
        params.setReturnType("id");

        List<String> docIds = productRepository.find(params);
        assertEquals(2, docIds.size());

        List<String> expectedDocsIds = Arrays.asList(p1.getId(), p2.getId());
        assertEquals(new HashSet<String>(expectedDocsIds), new HashSet<String>(docIds));
    }

    /*
     * LOCAL METHODS
     */
    protected Product newProduct(String productId, String manufacturerId) {
        Product product = new Product();

        product.setHidden(true);
        product.setId(productId);
        product.setIsoProductCode(123);
        product.setLastModification(DateTime.now().minusDays(1).toDate());
        product.setManufacturerId(manufacturerId);
        product.setNumBuyers(null); // we want to set a property with value null
        product.setPrice(new BigDecimal(1.23000000, new MathContext(10)));
        product.setRating(ProductRating.FourStars);
        product.setTags(asList("di legno", "blue"));
        product.setText("Vintage\nEconomico");
        product.setWeight(34.00);

        return product;
    }
    
    private void assertEqualsIdSet(Iterable<String> ids, Iterable<? extends BaseDocument> docs) {
        Set<String> idsFromDocs = new HashSet<>();
        for (BaseDocument doc : toList(docs)) {
            idsFromDocs.add(doc.getId());
        }

        ids = new HashSet<>(toList(ids));

        assertEquals(ids, idsFromDocs);
    }

    private void deleteProductRepoAndCreateSomeProducts() {
        productRepository.deleteAll();

        productRepository.save(p1);
        productRepository.save(p2);
        productRepository.save(p3);

        params.setView("by_manufacturerId");
        params.setReduce(false);
        params.setKeyType(String.class);
        params.setValueType(ProductSummary.class);
    }

    private List<String> toDocIds(List<ViewResultRow> results) {
        List<String> docIds = new ArrayList<String>();
        for (ViewResultRow result : results) {
            docIds.add(result.getId());
        }
        return docIds;
    }

}
