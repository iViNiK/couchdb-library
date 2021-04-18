package it.vinicioflamini.sharedlib.couchdb.test.ektorp;

import java.util.Map;
import java.util.Properties;

import org.ektorp.CouchDbConnector;
import org.ektorp.CouchDbInstance;
import org.ektorp.http.HttpClient;
import org.ektorp.impl.StdCouchDbConnector;
import org.ektorp.impl.StdCouchDbInstance;
import org.ektorp.spring.HttpClientFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import it.vinicioflamini.sharedlib.couchdb.ektorp.EktorpCrudRepository;
import it.vinicioflamini.sharedlib.couchdb.support.DocumentLoader;
import it.vinicioflamini.sharedlib.couchdb.test.model.Product;

/**
 * This configuration uses the the database "/ektorp-integration-tests/", creates/updates/deletes the standard design
 * document including the views of the design document.
 * 
 * @author Vinicio Flamini (io@vinicioflamini.it)
 */
@Configuration
public class EktorpTestConfiguration {
	private final String URI = "10.10.10.136";
	private final String PORT = "5984";
	private final String DBNAME = "ektorp-integration-tests";
	private final String USERNAME = "admin";
	private final String PASSWORD = "password";
	
    @Bean
    public StdCouchDbConnector connector() throws Exception {

        Properties properties = new Properties();
        properties.setProperty("autoUpdateViewOnChange", "true");
        properties.setProperty("username", this.USERNAME);
        properties.setProperty("password", this.PASSWORD);

        HttpClientFactoryBean factory = new HttpClientFactoryBean();
        factory.setUrl(String.format("http://%s:%s", this.URI, this.PORT));
        factory.setProperties(properties);
        factory.afterPropertiesSet();
        HttpClient client = factory.getObject();

        CouchDbInstance dbInstance = new StdCouchDbInstance(client);
        return new StdCouchDbConnector(this.DBNAME, dbInstance);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Bean
    @Lazy(false)
    public String createDatabaseAndUpdateDesignDocuments(CouchDbConnector db) {

        db.createDatabaseIfNotExists();

        DocumentLoader loader = new DocumentLoader(new EktorpCrudRepository(Map.class, db));

        loader.loadYaml(getClass().getResourceAsStream("../Product.yaml"));
        //loader.loadYaml(getClass().getResourceAsStream("../Product.json"));

        return "OK"; // anything
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Bean
    public EktorpCrudRepository<Product, String> productRepository(CouchDbConnector db) {
        return new EktorpCrudRepository(Product.class, db);
    }

}
