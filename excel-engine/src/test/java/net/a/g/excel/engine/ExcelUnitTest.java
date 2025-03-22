package net.a.g.excel.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import net.a.g.excel.load.ExcelLoaderImpl;
import net.a.g.excel.param.ExcelParameterImpl;
import net.a.g.excel.repository.ExcelRepository;
import net.a.g.excel.repository.ExcelRepositoryImpl;

/**
 * Base test class providing common setup and teardown functionality for Excel engine tests.
 * This abstract class initializes the core components needed for testing Excel operations:
 * - ExcelEngine for Excel operations
 * - ExcelRepository for resource management
 * - ExcelLoader for loading Excel files
 */
public abstract class ExcelUnitTest {

    /** The Excel engine instance used for operations */
    protected ExcelEngine engine;
    
    /** Loader for injecting Excel resources */
    protected ExcelLoaderImpl loader;
    
    /** Repository for storing Excel resources */
    protected ExcelRepository repo;

    /**
     * Default constructor.
     */
    public ExcelUnitTest() {
        super();
    }

    /**
     * Sets up the test environment before each test.
     * Initializes:
     * - ExcelEngine implementation
     * - Repository for Excel resources
     * - Loader for Excel files
     * - Configures dependencies between components
     *
     * @throws MalformedURLException if resource URLs are invalid
     * @throws IOException if there are issues reading resources
     */
    @BeforeEach
    public void setup() throws MalformedURLException, IOException {

		engine = new ExcelEngineImpl();
		repo = new ExcelRepositoryImpl();
		loader = new ExcelLoaderImpl();

		loader.setRepository(repo);
		((ExcelEngineImpl) engine).setParameter(new ExcelParameterImpl());
		((ExcelEngineImpl) engine).setRepository(repo);

		loader.init();
	}

    /**
     * Cleans up the test environment after each test.
     * Purges all resources from the repository and verifies cleanup.
     */
    @AfterEach
    public void close() {
		repo.purge();
		assertEquals(0, repo.count());
	}

}
