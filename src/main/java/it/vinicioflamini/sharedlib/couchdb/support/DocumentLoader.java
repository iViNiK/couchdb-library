package it.vinicioflamini.sharedlib.couchdb.support;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;

import org.yaml.snakeyaml.Yaml;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.vinicioflamini.sharedlib.couchdb.api.CouchDbCrudRepository;

/**
 * Loads a document into the database. Useful for the initialization of a
 * database.
 * 
 * @author Vinicio Flamini (io@vinicioflamini.it)
 */
public class DocumentLoader {

	@SuppressWarnings("rawtypes")
	private CouchDbCrudRepository<Map, String> repository;

	/**
	 * @param repository A repository for {@link Map maps}.
	 */
	@SuppressWarnings("rawtypes")
	public DocumentLoader(CouchDbCrudRepository<Map, String> repository) {
		super();
		this.repository = repository;
	}

	/**
	 * Loads the given JSON document into the database. Overrides an existing
	 * document.
	 * 
	 * @param documentContent the JSON content of a document
	 */
	public void loadJson(InputStream documentContent) {
		save(parseJson(documentContent));
	}

	/**
	 * Loads the given YAML document into the database. Overrides an existing
	 * document.
	 * 
	 * @param documentContent the YAML content of a document
	 */
	public void loadYaml(InputStream documentContent) {
		save(parseYaml(documentContent));
	}

	public void save(Map<String, Object> document) {

		String documentId = (String) document.get("_id");

		if (repository.existsById(documentId)) {
			@SuppressWarnings("rawtypes")
			Optional<Map> documentInDbHolder = repository.findById(documentId);
			if (documentInDbHolder.isPresent()) {
				document.put("_rev", documentInDbHolder.get().get("_rev"));
			}
		}

		repository.save(document);
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> parseJson(InputStream documentContent) {
		try {
			return new ObjectMapper().readValue(documentContent, Map.class);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> parseYaml(InputStream documentContent) {
		return (Map<String, Object>) new Yaml().load(documentContent);
	}

}
