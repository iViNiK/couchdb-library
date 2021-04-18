package it.vinicioflamini.sharedlib.couchdb.test.ektorp;

import org.springframework.test.context.ContextConfiguration;

import it.vinicioflamini.sharedlib.couchdb.test.api.AbstractCrudRepositoryTest;

@ContextConfiguration(classes = { EktorpTestConfiguration.class })
public class EktorpCrudRepositoryTest extends AbstractCrudRepositoryTest {

}
