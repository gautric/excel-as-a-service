package net.a.g.excel.engine.cdi;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import net.a.g.excel.engine.ExcelEngine;
import net.a.g.excel.load.ExcelLoader;
import net.a.g.excel.load.cdi.ExcelLoaderImpl;
import net.a.g.excel.param.ExcelParameter;
import net.a.g.excel.param.cdi.ExcelParameterImpl;
import net.a.g.excel.repository.ExcelRepository;
import net.a.g.excel.repository.cdi.ExcelRepositoryImpl;

/**
 * Shared Weld SE setup: the four CDI beans of this module, one container per
 * test. The repository is purged before each test because the loader's startup
 * observer loads whatever {@code excel.static.resouces.uri} points to.
 */
@ExtendWith(WeldJunit5Extension.class)
public abstract class CdiTestSupport {

	static final Path SAMPLE = Path.of("..", "sample");

	@WeldSetup
	public WeldInitiator weld = WeldInitiator
			.from(ExcelEngineImpl.class, ExcelParameterImpl.class, ExcelLoaderImpl.class, ExcelRepositoryImpl.class)
			.activate(RequestScoped.class, SessionScoped.class).build();

	@Inject
	ExcelEngine engine;

	@Inject
	ExcelLoader loader;

	@Inject
	ExcelRepository repo;

	@Inject
	ExcelParameter param;

	@BeforeEach
	void cleanRepository() {
		repo.purge();
	}

	/** Injects {@code ../sample/<file>} under {@code name}, keeping the file name for cross-references. */
	void load(String name, String file) {
		load(name, SAMPLE.resolve(file));
	}

	void load(String name, Path path) {
		try (InputStream in = Files.newInputStream(path)) {
			assertTrue(loader.injectResource(name, path.getFileName().toString(), in), "inject " + path);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
