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

public abstract class ExcelUnitTest {

	protected ExcelEngine engine;
	ExcelLoaderImpl loader;
	ExcelRepository repo;

	public ExcelUnitTest() {
		super();
	}

	@BeforeEach
	public void setup() throws MalformedURLException, IOException {

		engine = new ExcelEngineImpl();
		repo = new ExcelRepositoryImpl();
		loader = new ExcelLoaderImpl();

		loader.setRepo(repo);
		((ExcelEngineImpl) engine).setParam(new ExcelParameterImpl());
		((ExcelEngineImpl) engine).setRepo(repo);

		loader.init();
	}

	@AfterEach
	public void close() {
		repo.purge();
		assertEquals(0, repo.count());
	}

}