package net.a.g.excel.engine.cdi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;

import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import jakarta.inject.Inject;
import net.a.g.excel.exception.ExcelEngineException;
import net.a.g.excel.exception.ExcelResourceNotFoundException;
import net.a.g.excel.model.ExcelResource;
import net.a.g.excel.repository.cdi.ExcelRepositoryImpl;

@DisplayName("ExcelRepository through CDI")
public class RepositoryCdiTest extends CdiTestSupport {

	@Inject
	ExcelRepositoryImpl repoImpl;

	private static ExcelResource resource(String name, byte[] doc) {
		ExcelResource r = new ExcelResource();
		r.setName(name);
		r.setFile(name + ".xlsx");
		r.setDoc(doc);
		return r;
	}

	@Test
	void addCountContainsGet() throws IOException {
		byte[] doc = Files.readAllBytes(SAMPLE.resolve("Type.xlsx"));
		assertTrue(repo.add(resource("Type", doc)));
		assertEquals(1, repo.count());
		assertTrue(repo.contains("Type"));
		assertEquals("Type.xlsx", repo.get("Type").getFile());
	}

	@Test
	void addNullIsRejected() {
		assertThrows(IllegalArgumentException.class, () -> repo.add(null));
	}

	@Test
	void addUnrecognisedDocumentReturnsFalse() {
		assertFalse(repo.add(resource("Bad", new byte[] { 1, 2, 3 })));
		assertFalse(repo.contains("Bad"));
	}

	@Test
	void addEmptyDocumentReturnsFalse() {
		assertFalse(repo.add(resource("Bad", new byte[0])));
		assertFalse(repo.contains("Bad"));
	}

	@ParameterizedTest(name = "get(''{0}'')")
	@NullAndEmptySource
	void getWithBlankNameIsRejected(String name) {
		assertThrows(IllegalArgumentException.class, () -> repo.get(name));
		assertThrows(IllegalArgumentException.class, () -> repo.retrieveWorkbook(name));
	}

	@Test
	void unknownResourceIsTyped() {
		ExcelResourceNotFoundException ex = assertThrows(ExcelResourceNotFoundException.class, () -> repo.get("Nope"));
		assertEquals("Nope", ex.getResourceName());
		assertThrows(ExcelResourceNotFoundException.class, () -> repo.retrieveWorkbook("Nope"));
		assertThrows(ExcelResourceNotFoundException.class, () -> repo.openWorkbook("Nope"));
		assertFalse(repo.contains("Nope"));
	}

	@Test
	void retrieveWorkbookIsCachedUntilReplaced() {
		load("Type", "Type.xlsx");
		Workbook first = repo.retrieveWorkbook("Type");
		assertSame(first, repo.retrieveWorkbook("Type"));
		load("Type", "Test1.xlsx");
		Workbook second = repo.retrieveWorkbook("Type");
		assertNotSame(first, second);
		assertEquals(3, second.getNumberOfSheets());
	}

	@Test
	void openWorkbookReturnsAPrivateCopy() throws IOException {
		load("Type", "Type.xlsx");
		try (Workbook a = repo.openWorkbook("Type"); Workbook b = repo.openWorkbook("Type")) {
			assertNotSame(a, b);
			assertNotSame(repo.retrieveWorkbook("Type"), a);
			a.getSheet("Sheet1").getRow(0).getCell(0).setCellValue(99);
			assertEquals(0.0, b.getSheet("Sheet1").getRow(0).getCell(0).getNumericCellValue());
			assertEquals(0.0, repo.retrieveWorkbook("Type").getSheet("Sheet1").getRow(0).getCell(0).getNumericCellValue());
		}
	}

	@Test
	void removeExistingAndMissing() {
		load("Type", "Type.xlsx");
		assertTrue(repo.remove("Type"));
		assertFalse(repo.remove("Type"));
		assertEquals(0, repo.count());
	}

	@Test
	void purgeEmptiesTheRepository() {
		load("Type", "Type.xlsx");
		load("T1", "Test1.xlsx");
		repo.retrieveWorkbook("T1");
		repo.purge();
		assertEquals(0, repo.count());
		assertTrue(repo.listOfResources().isEmpty());
	}

	@Test
	void closedRepositoryRefusesUse() {
		load("Type", "Type.xlsx");
		repoImpl.close();
		assertThrows(IllegalStateException.class, () -> repo.get("Type"));
		assertThrows(IllegalStateException.class, () -> repo.retrieveWorkbook("Type"));
		assertThrows(IllegalStateException.class, () -> repo.purge());
		assertThrows(IllegalStateException.class, () -> repo.remove("Type"));
	}
}
