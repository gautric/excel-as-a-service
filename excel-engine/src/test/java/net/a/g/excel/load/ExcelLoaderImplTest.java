package net.a.g.excel.load;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.a.g.excel.exception.ExcelInvalidInputException;
import net.a.g.excel.model.ExcelResource;
import net.a.g.excel.repository.ExcelRepositoryImpl;

public class ExcelLoaderImplTest {

	private ExcelRepositoryImpl repo;
	private ExcelLoaderImpl loader;

	@BeforeEach
	void setUp() {
		repo = new ExcelRepositoryImpl();
		loader = new ExcelLoaderImpl();
		loader.setRepository(repo);
	}

	@AfterEach
	void tearDown() {
		repo.close();
	}

	private Set<String> names() {
		return repo.listOfResources().stream().map(ExcelResource::getName).collect(Collectors.toSet());
	}

	@Test
	void gettersAndSetters() {
		assertSame(repo, loader.getRepository());
		assertNull(loader.getResouceUri());
		loader.setResouceUri("x.xlsx");
		assertEquals("x.xlsx", loader.getResouceUri());
	}

	@Test
	void injectResourceStoresBytesNameAndFile() throws Exception {
		byte[] bytes = Files.readAllBytes(new File("../sample/Test1.xlsx").toPath());
		assertTrue(loader.injectResource("Test1", "Test1.xlsx", new ByteArrayInputStream(bytes)));
		ExcelResource r = repo.get("Test1");
		assertEquals("Test1.xlsx", r.getFile());
		assertArrayEquals(bytes, r.getDoc());
	}

	@Test
	void injectResourceUnreadableStreamReturnsFalse() {
		InputStream failing = new InputStream() {
			@Override
			public int read() throws IOException {
				throw new IOException("disk gone");
			}
		};
		assertFalse(loader.injectResource("X", "X.xlsx", failing));
		assertEquals(0, repo.count());
	}

	@Test
	void injectResourceInvalidNamePropagatesValidationError() throws Exception {
		try (InputStream in = new FileInputStream("../sample/KYC.xlsx")) {
			assertThrows(ExcelInvalidInputException.class, () -> loader.injectResource("bad name", "KYC.xlsx", in));
		}
	}

	@Test
	void injectResourceNonExcelReportsFalse() {
		assertFalse(loader.injectResource("junk", "junk.xlsx", new ByteArrayInputStream("junk".getBytes())));
		assertFalse(repo.contains("junk"));
	}

	@Test
	void injectResourceNullStreamThrows() {
		assertThrows(NullPointerException.class, () -> loader.injectResource("X", "X.xlsx", null));
	}

	@Test
	void initWithoutUriLoadsNothing() {
		loader.init();
		assertEquals(0, repo.count());
	}

	@Test
	void initFromClasspath() {
		loader.setResouceUri("KYC.xlsx");
		loader.init();
		assertEquals(Set.of("KYC"), names());
		assertEquals("KYC.xlsx", repo.get("KYC").getFile());
	}

	@Test
	void initFromSingleFile() {
		loader.setResouceUri("../sample/Type.xlsx");
		loader.init();
		assertEquals(Set.of("Type"), names());
		assertEquals("Type.xlsx", repo.get("Type").getFile());
	}

	@Test
	void initFromDirectoryLoadsOnlyExcelFiles(@TempDir Path dir) throws Exception {
		Files.copy(Path.of("../sample/KYC.xlsx"), dir.resolve("KYC.xlsx"));
		Files.copy(Path.of("../sample/Test1.xlsx"), dir.resolve("Test1.xlsx"));
		Files.copy(Path.of("../sample/Type.xlsx"), dir.resolve("~lock.xlsx"));
		Files.writeString(dir.resolve("readme.txt"), "x");
		loader.setResouceUri(dir.toString());
		loader.init();
		assertEquals(Set.of("KYC", "Test1"), names());
	}

	@Test
	void initFromSampleDirectoryLoadsAllSix() {
		loader.setResouceUri("../sample");
		loader.init();
		assertEquals(Set.of("KYC", "KYCAPI", "Primary", "Secondary", "Test1", "Type"), names());
	}

	@Test
	void initFromMissingPathLoadsNothing() {
		loader.setResouceUri("../sample/does-not-exist.xlsx");
		assertDoesNotThrow(loader::init);
		assertEquals(0, repo.count());
	}

	@Test
	void initSwallowsInvalidResourceName(@TempDir Path dir) throws Exception {
		Path f = dir.resolve("bad name.xlsx");
		Files.copy(Path.of("../sample/KYC.xlsx"), f, StandardCopyOption.REPLACE_EXISTING);
		loader.setResouceUri(f.toString());
		assertDoesNotThrow(loader::init);
		assertEquals(0, repo.count());
	}
}
