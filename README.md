About this library
======================

Prerequisites
-------------
couchdb-library requires Java 8 or higher.
In order to test the application, a CouchDB installation must be available.

Basics
------
Implementation is based on Ektorp as CouchDB driver.

When defining your own Entities, take care to extend BaseDocument class:

    public class Product extends BaseDocument { 
    	// your fields, getters, setters here
    }

Also, in order to get viewresult support for queries, you have to provide a "Facts" and a "Summary" class that duplicates the properties of each model you defined. For example:

    public class ProductFacts { 
    	// your fields, getters, setters here (as in the model class)
    }

    public class ProductSummary {
	    private ProductFacts facts;
	
	    public ProductFacts getFacts() {
	        return facts;
	    }
	
	    public void setFacts(ProductFacts facts) {
	        this.facts = facts;
	    }
	}


First, you set up a `CouchDbConnector` as described by the Ektorp documentation. 

    CouchDbConnector db = ...

Then you create a repository for each type of entities. For an entity type like `Product` you write

    CouchDbCrudRepository<Product,String> productRepository = new EktorpCrudRepository(Product.class, db);

The type `CouchDbCrudRepository` extends Spring Data's `CrudRepository` so that you are able to query CouchDB views immediately by using the `find` method.

One thing is still missing. The repository requires a design document (by default named `_design/Product`) that provides a view named `by_id`.

This view must provide ID and revision for each entity of the given type. The reduce function must be `_count`. The design document may look like this.

    {
      "_id" : "_design/Product",
      "language" : "javascript",
      "views" : {
        "by_id" : {
          "map" : "function(doc) { if(doc.type == 'Product') {emit(doc._id, { _id : doc._id, _rev: doc._rev } )} }",
          "reduce" : "_count"
        }
    } 

One way to load the design document into the database is `DocumentLoader` but you can use any other mean, of course.
You can use the repository as soon the design document is available in the database.

    DocumentLoader loader = new DocumentLoader(new EktorpCrudRepository(Map.class, db));
    loader.loadJson(getClass().getResourceAsStream("../Product.json")); // design document Product.json is taken from the classpath

In case you prefer YAML documents, you can use the method `loadYaml` instead of `loadJson`.
In YAML documents you don't have to care about the formatting of the contained Javascript functions.  
    
Finally, your Java-based Spring configuration might look like this.

    @Configuration
    public class CouchDbRepositoriesConfiguration {
    
        @Bean
        public StdCouchDbConnector connector() throws Exception { ... }
    
        @Bean
        public CouchDbCrudRepository<Product,String> productRepository(CouchDbConnector db) { ... }

        @Bean @Lazy(false)
        public String initializeDatabaseAndDesignDocuments(CouchDbConnector db) { ... }
    }
  
Automatic implementation of finder methods
------------------------------------------  

If you want to use the automatic implementation of finder methods, you create an interface that contains the desired methods.

    public interface ProductRepository extends CouchDbCrudRepository<Product, String> {
    
        List<Comment> findByComment(Object[] key, ViewParams viewParams);

        ViewResult findByComment(Object[] key, Boolean descending, ViewParams viewParams, Class<?> valueType);
    }

The method parameter `viewParams` allow you to specify arbitrary parameters for the view query.
But usually you will add more parameters to the method signature because they cover your main use cases, here: `key`.
Parameters set in `viewParams` do not override parameters that are contained in the method signature, i.e the key in `viewParams` is ignored. 

Making queries with ViewParams
------------------------------

Follow the example below to submit a query using ViewParams and returning result in ViewResult object. Note that binding to the "Summary" class is automatically resolved.

        private ViewParams params = new ViewParams();
        params.setKey("Your Key Here");
        
        ViewResult viewResults = productRepository.find(params);
        
        ProductSummary summary = viewResults.getRows().get(0).getValue();


Further references
------------------

To see library in action, in typical use cases, please refer to the test class "AbstractCrudRepositoryTest"

License
----------
Copyright &copy;2020 by Vinicio Flamini <io@vinicioflamini.it>

