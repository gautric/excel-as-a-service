package net.a.g.excel.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import net.a.g.excel.exception.ExcelEngineException;
import net.a.g.excel.exception.ExcelInvalidInputException;
import net.a.g.excel.exception.ExcelResourceNotFoundException;
import net.a.g.excel.model.ExcelResource;

public class ExcelRepositoryImplTest {

	private ExcelRepositoryImpl repo;

	static ExcelResource sample(String name, String file) throws Exception {
		ExcelResource r = new ExcelResource(name);
		r.setFile(file);
		r.setDoc(Files.readAllBytes(new File("../sample/" + file).toPath()));
		return r;
	}

	@BeforeEach
	void setUp() {
		repo = new ExcelRepositoryImpl();
	}

	@AfterEach
	void tearDown() {
		repo.close();
	}

	@Test
	void emptyRepository() {
		assertEquals(0, repo.count());
		assertTrue(repo.listOfResources().isEmpty());
		assertFalse(repo.contains("KYC"));
	}

	@Test
	void addValidResource() throws Exception {
		assertTrue(repo.add(sample("KYC", "KYC.xlsx")));
		assertEquals(1, repo.count());
		assertTrue(repo.contains("KYC"));
		assertEquals("KYC.xlsx", repo.get("KYC").getFile());
	}

	@Test
	void addSeveralResources() throws Exception {
		repo.add(sample("KYC", "KYC.xlsx"));
		repo.add(sample("Test1", "Test1.xlsx"));
		repo.add(sample("Type", "Type.xlsx"));
		assertEquals(3, repo.count());
		Set<String> names = repo.listOfResources().stream().map(ExcelResource::getName).collect(Collectors.toSet());
		assertEquals(Set.of("KYC", "Test1", "Type"), names);
	}

	@Test
	void deprecatedListOfResourceMatchesNewOne() throws Exception {
		repo.add(sample("KYC", "KYC.xlsx"));
		Collection<ExcelResource> a = repo.listOfResource();
		Collection<ExcelResource> b = repo.listOfResources();
		assertEquals(a.size(), b.size());
		assertEquals(a.iterator().next(), b.iterator().next());
	}

	@Test
	void addNullThrows() {
		assertThrows(IllegalArgumentException.class, () -> repo.add(null));
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = { "KYC.xlsx" })
	void addInvalidNameThrows(String name) throws Exception {
		ExcelResource r = sample("x", "KYC.xlsx");
		r.setName(name);
		assertThrows(ExcelInvalidInputException.class, () -> repo.add(r));
		assertEquals(0, repo.count());
	}

	@Test
	void addUnreadableDocReturnsFalse() {
		ExcelResource r = new ExcelResource("junk");
		r.setDoc("not excel".getBytes());
		assertFalse(repo.add(r));
		assertFalse(repo.contains("junk"));
	}

	@Test
	void addNullDocIsWrapped() {
		ExcelResource r = new ExcelResource("nodoc");
		ExcelEngineException e = assertThrows(ExcelEngineException.class, () -> repo.add(r));
		assertEquals("Failed to add resource: nodoc", e.getMessage());
		assertFalse(repo.contains("nodoc"));
	}

	@Test
	void addReplacesExistingAndEvictsCachedWorkbook() throws Exception {
		repo.add(sample("R", "KYC.xlsx"));
		Workbook first = repo.retrieveWorkbook("R");
		assertNotNull(first.getSheet("ComputeKYC"));
		repo.add(sample("R", "Test1.xlsx"));
		assertEquals(1, repo.count());
		Workbook second = repo.retrieveWorkbook("R");
		assertNotSame(first, second);
		assertNotNull(second.getSheet("Sheet1"));
	}

	@Test
	void getUnknownThrowsNotFound() {
		ExcelResourceNotFoundException e = assertThrows(ExcelResourceNotFoundException.class, () -> repo.get("NOPE"));
		assertEquals("NOPE", e.getResourceName());
	}

	@ParameterizedTest
	@NullAndEmptySource
	void getBlankThrowsIllegalArgument(String name) {
		assertThrows(IllegalArgumentException.class, () -> repo.get(name));
	}

	@Test
	void retrieveWorkbookIsCached() throws Exception {
		repo.add(sample("KYC", "KYC.xlsx"));
		Workbook a = repo.retrieveWorkbook("KYC");
		assertSame(a, repo.retrieveWorkbook("KYC"));
		assertEquals(3, a.getNumberOfSheets());
	}

	@Test
	void retrieveWorkbookUnknownThrowsNotFound() {
		assertThrows(ExcelResourceNotFoundException.class, () -> repo.retrieveWorkbook("NOPE"));
	}

	@ParameterizedTest
	@ValueSource(strings = { "\t" })
	void retrieveWorkbookBlankThrows(String name) {
		assertThrows(IllegalArgumentException.class, () -> repo.retrieveWorkbook(name));
	}

	@Test
	void openWorkbookReturnsPrivateCopy() throws Exception {
		repo.add(sample("KYC", "KYC.xlsx"));
		Workbook cached = repo.retrieveWorkbook("KYC");
		try (Workbook copy = repo.openWorkbook("KYC")) {
			assertNotSame(cached, copy);
			copy.getSheet("ComputeKYC").getRow(2).getCell(1).setCellValue("CY");
			assertEquals("FR", cached.getSheet("ComputeKYC").getRow(2).getCell(1).getStringCellValue());
		}
	}

	@Test
	void openWorkbookUnknownThrowsNotFound() {
		assertThrows(ExcelResourceNotFoundException.class, () -> repo.openWorkbook("NOPE"));
	}

	@Test
	void removeExisting() throws Exception {
		repo.add(sample("KYC", "KYC.xlsx"));
		repo.retrieveWorkbook("KYC");
		assertTrue(repo.remove("KYC"));
		assertFalse(repo.contains("KYC"));
		assertEquals(0, repo.count());
		assertThrows(ExcelResourceNotFoundException.class, () -> repo.retrieveWorkbook("KYC"));
	}

	@Test
	void removeUnknownReturnsFalse() {
		assertFalse(repo.remove("NOPE"));
	}

	@Test
	void defaultRemoveOfInterfaceIsUnsupported() {
		ExcelRepository minimal = new ExcelRepository() {
			public boolean add(ExcelResource resource) { return false; }
			public int count() { return 0; }
			public Collection<ExcelResource> listOfResource() { return Set.of(); }
			public Collection<ExcelResource> listOfResources() { return Set.of(); }
			public void purge() { }
			public boolean contains(String name) { return false; }
			public ExcelResource get(String name) { return new ExcelResource(name); }
			public Workbook retrieveWorkbook(String name) { return null; }
		};
		assertThrows(UnsupportedOperationException.class, () -> minimal.remove("x"));
	}

	@Test
	void defaultOpenWorkbookFailsOnUnreadableDoc() {
		ExcelRepository minimal = new ExcelRepository() {
			public boolean add(ExcelResource resource) { return false; }
			public int count() { return 0; }
			public Collection<ExcelResource> listOfResource() { return Set.of(); }
			public Collection<ExcelResource> listOfResources() { return Set.of(); }
			public void purge() { }
			public boolean contains(String name) { return true; }
			public ExcelResource get(String name) {
				ExcelResource r = new ExcelResource(name);
				r.setDoc("junk".getBytes());
				return r;
			}
			public Workbook retrieveWorkbook(String name) { return null; }
		};
		ExcelEngineException e = assertThrows(ExcelEngineException.class, () -> minimal.openWorkbook("R"));
		assertEquals("Failed to load workbook: R", e.getMessage());
	}

	@Test
	void purgeClearsEverything() throws Exception {
		repo.add(sample("KYC", "KYC.xlsx"));
		repo.add(sample("Type", "Type.xlsx"));
		repo.retrieveWorkbook("KYC");
		repo.purge();
		assertEquals(0, repo.count());
		assertFalse(repo.contains("KYC"));
	}

	@Test
	void closedRepositoryRejectsOperations() throws Exception {
		ExcelResource r = sample("KYC", "KYC.xlsx");
		repo.add(r);
		repo.close();
		assertThrows(IllegalStateException.class, () -> repo.add(r));
		assertThrows(IllegalStateException.class, () -> repo.get("KYC"));
		assertThrows(IllegalStateException.class, () -> repo.retrieveWorkbook("KYC"));
		assertThrows(IllegalStateException.class, () -> repo.remove("KYC"));
		assertThrows(IllegalStateException.class, () -> repo.purge());
	}

	@Test
	void closedRepositoryStillAnswersReadOnlyQueries() throws Exception {
		repo.add(sample("KYC", "KYC.xlsx"));
		repo.close();
		assertEquals(1, repo.count());
		assertTrue(repo.contains("KYC"));
	}

	@Test
	void closeIsIdempotent() {
		repo.close();
		repo.close();
		assertThrows(IllegalStateException.class, () -> repo.get("x"));
	}

	@Test
	void containsNullIsFalse() {
		assertFalse(repo.contains(null));
	}

	@Test
	void removeNullIsFalse() {
		assertFalse(repo.remove(null));
	}
}
