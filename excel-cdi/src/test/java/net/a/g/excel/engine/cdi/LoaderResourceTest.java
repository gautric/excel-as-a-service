package net.a.g.excel.engine.cdi;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.inject.Inject;
import net.a.g.excel.exception.ExcelEngineException;
import net.a.g.excel.exception.ExcelInvalidInputException;
import net.a.g.excel.load.cdi.ExcelLoaderImpl;
import net.a.g.excel.model.ExcelCell;
import net.a.g.excel.model.ExcelResource;

@DisplayName("Loading resources from directory, file, classpath and stream")
public class LoaderResourceTest extends CdiTestSupport {

	@Inject
	ExcelLoaderImpl loaderImpl;

	@TempDir
	Path tmp;

	private void initFrom(String uri) {
		loaderImpl.setResouceUri(uri);
		loaderImpl.init();
	}

	private List<String> names() {
		return repo.listOfResources().stream().map(ExcelResource::getName).collect(Collectors.toList());
	}

	private Path copySample(String file, String as) throws IOException {
		return Files.copy(SAMPLE.resolve(file), tmp.resolve(as));
	}

	@Test
	void loadsFromClasspath() {
		initFrom("KYC.xlsx");
		assertThat(names(), is(List.of("KYC")));
		assertEquals("KYC.xlsx", repo.get("KYC").getFile());
	}

	@Test
	void loadsSingleFileByPath() {
		initFrom(SAMPLE.resolve("KYCAPI.xlsx").toString());
		assertThat(names(), is(List.of("KYCAPI")));
		assertEquals("KYCAPI.xlsx", repo.get("KYCAPI").getFile());
	}

	@Test
	void loadsEveryWorkbookOfADirectory() {
		initFrom(SAMPLE.toString());
		assertThat(names(), containsInAnyOrder("KYC", "KYCAPI", "Primary", "Secondary", "Test1", "Type"));
	}

	@Test
	void directoryLoadSkipsLockFilesAndNonExcelFiles() throws IOException {
		copySample("Type.xlsx", "Type.xlsx");
		copySample("Test1.xlsx", "~$Test1.xlsx");
		Files.writeString(tmp.resolve("notes.txt"), "not a workbook");
		initFrom(tmp.toString());
		assertThat(names(), is(List.of("Type")));
	}

	@Test
	void directoryLoadDoesNotRecurse() throws IOException {
		copySample("Type.xlsx", "Type.xlsx");
		Path sub = Files.createDirectory(tmp.resolve("nested"));
		Files.copy(SAMPLE.resolve("Test1.xlsx"), sub.resolve("Test1.xlsx"));
		initFrom(tmp.toString());
		assertThat(names(), is(List.of("Type")));
	}

	@Test
	void loadsLegacyXlsAndComputesIt() throws IOException {
		Path xls = tmp.resolve("Legacy.xls");
		try (Workbook wb = new HSSFWorkbook(); OutputStream out = Files.newOutputStream(xls)) {
			Sheet s = wb.createSheet("Data");
			Row r = s.createRow(0);
			r.createCell(0).setCellValue(2);
			r.createCell(1).setCellValue(3);
			r.createCell(2).setCellFormula("A1*B1");
			wb.write(out);
		}
		initFrom(tmp.toString());
		assertThat(names(), is(List.of("Legacy")));
		List<ExcelCell> cells = engine.cellCalculation("Legacy", List.of("Data!C1"), Map.of("Data!A1", "4"));
		assertEquals(12.0, cells.get(0).getValue());
	}

	@Test
	void directoryLoadedWorkbookIsComputable() {
		initFrom(SAMPLE.toString());
		List<ExcelCell> cells = engine.cellCalculation("KYCAPI", List.of("ComputeKYC!C6"),
				Map.of("ComputeKYC!B2", "true", "ComputeKYC!B3", "CY", "ComputeKYC!B4", "1000000"));
		assertEquals(125.0, cells.get(0).getValue());
	}

	@Test
	void oneCorruptFileDoesNotPreventLoadingTheOthers() throws IOException {
		// empty files created before and after the good ones, so one precedes them whatever the listing order
		Files.createFile(tmp.resolve("BrokenFirst.xlsx"));
		for (String f : List.of("KYC.xlsx", "KYCAPI.xlsx", "Primary.xlsx", "Secondary.xlsx", "Test1.xlsx", "Type.xlsx")) {
			copySample(f, f);
		}
		Files.createFile(tmp.resolve("BrokenLast.xlsx"));
		initFrom(tmp.toString());
		assertThat(names(), containsInAnyOrder("KYC", "KYCAPI", "Primary", "Secondary", "Test1", "Type"));
	}

	@Test
	void unknownPathLoadsNothing() {
		initFrom(tmp.resolve("does-not-exist.xlsx").toString());
		assertEquals(0, repo.count());
	}

	@Test
	void nullUriLoadsNothing() {
		initFrom(null);
		assertEquals(0, repo.count());
	}

	@Test
	void injectKeepsNameFileAndBytes() throws IOException {
		byte[] bytes = Files.readAllBytes(SAMPLE.resolve("Test1.xlsx"));
		assertTrue(loader.injectResource("T1", "Test1.xlsx", new ByteArrayInputStream(bytes)));
		ExcelResource r = repo.get("T1");
		assertEquals("T1", r.getName());
		assertEquals("Test1.xlsx", r.getFile());
		assertArrayEquals(bytes, r.getDoc());
	}

	@Test
	void injectAcceptsNullFileName() throws IOException {
		try (InputStream in = Files.newInputStream(SAMPLE.resolve("Type.xlsx"))) {
			assertTrue(loader.injectResource("Type", null, in));
		}
		assertNull(repo.get("Type").getFile());
	}

	@Test
	void injectSameNameReplacesTheWorkbook() {
		load("X", "KYCAPI.xlsx");
		load("X", "Type.xlsx");
		assertEquals(1, repo.count());
		assertEquals("Sheet1", engine.listOfSheet("X").get(0).getName());
	}

	@ParameterizedTest(name = "unreadable bytes ''{0}''")
	@ValueSource(strings = { "PK\u0003\u0004truncated" })
	void injectOfEmptyOrTruncatedWorkbookIsATypedError(String content) {
		InputStream in = new ByteArrayInputStream(content.getBytes());
		assertThrows(ExcelEngineException.class, () -> loader.injectResource("Bad", "Bad.xlsx", in));
		assertFalse(repo.contains("Bad"));
	}

	@Test
	void injectOfUnrecognisedBytesReportsFailure() {
		assertFalse(loader.injectResource("Bad", "Bad.xlsx", new ByteArrayInputStream("not an excel file".getBytes())));
	}

	@Test
	void injectFromFailingStreamReturnsFalse() {
		InputStream failing = new InputStream() {
			@Override
			public int read() throws IOException {
				throw new IOException("disk gone");
			}
		};
		assertFalse(loader.injectResource("Gone", "Gone.xlsx", failing));
		assertEquals(0, repo.count());
	}

	@ParameterizedTest(name = "invalid resource name ''{0}''")
	@NullAndEmptySource
	@ValueSource(strings = { "with space", "../escape" })
	void injectWithInvalidNameIsRejected(String name) throws IOException {
		try (InputStream in = Files.newInputStream(SAMPLE.resolve("Type.xlsx"))) {
			assertThrows(ExcelInvalidInputException.class, () -> loader.injectResource(name, "Type.xlsx", in));
		}
		assertEquals(0, repo.count());
	}
}
