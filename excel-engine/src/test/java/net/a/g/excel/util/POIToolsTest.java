package net.a.g.excel.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;

import org.apache.poi.EmptyFileException;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class POIToolsTest {

	@ParameterizedTest
	@CsvSource({ "KYC.xlsx,3", "Primary.xlsx,1", "Type.xlsx,1" })
	void loadsEverySample(String file, int sheets) throws Exception {
		byte[] bytes = Files.readAllBytes(new File("../sample/" + file).toPath());
		try (Workbook wb = POITools.convertByteToWorkbook(bytes)) {
			assertNotNull(wb);
			assertEquals(sheets, wb.getNumberOfSheets());
		}
	}

	@Test
	void loadsLegacyXls() throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (HSSFWorkbook wb = new HSSFWorkbook()) {
			wb.createSheet("Legacy");
			wb.write(out);
		}
		try (Workbook wb = POITools.convertByteToWorkbook(out.toByteArray())) {
			assertEquals("Legacy", wb.getSheetName(0));
		}
	}

	@Test
	void loadsInMemoryXlsx() throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (XSSFWorkbook wb = new XSSFWorkbook()) {
			wb.createSheet("A");
			wb.createSheet("B");
			wb.write(out);
		}
		try (Workbook wb = POITools.convertByteToWorkbook(out.toByteArray())) {
			assertEquals(2, wb.getNumberOfSheets());
		}
	}

	@Test
	void garbageBytesReturnNull() {
		assertNull(POITools.convertByteToWorkbook("not a workbook".getBytes()));
	}

	@Test
	void emptyBytesReturnNull() {
		assertNull(POITools.convertByteToWorkbook(new byte[0]));
	}


	@Test
	void nullBytesThrowNpe() {
		assertThrows(NullPointerException.class, () -> POITools.convertByteToWorkbook(null));
	}
}
