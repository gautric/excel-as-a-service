package net.a.g.excel.grpc;

import static net.a.g.excel.grpc.Fixtures.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.grpc.Status;
import net.a.g.excel.repository.ExcelRepositoryImpl;

class ListOfSheetTest {

	static ExcelService service;

	@BeforeAll
	static void setup() {
		service = service(ALL);
	}

	private static List<String> sheets(ExcelService s, String resource) {
		return items(s.listOfSheet(ExcelRequest.newBuilder().setResource(resource).build())).stream()
				.map(ExcelSheet::getName).toList();
	}

	@ParameterizedTest(name = "{0} -> {1}")
	@CsvSource(delimiter = ';', value = { //
			"KYC;ComputeKYC,COUNTRY,AMOUNT", //
			"KYCAPI;ComputeKYC,COUNTRY,AMOUNT", //
			"Primary;Feuil1", //
			"Secondary;Feuil1", //
			"Test1;Sheet1,Sheet2,Sheet3", //
			"Type;Sheet1" })
	void listsSheetsInWorkbookOrder(String resource, String expected) {
		assertEquals(List.of(expected.split(",")), sheets(service, resource));
	}

	@Test
	void sheetFieldOfTheRequestIsIgnored() {
		List<String> got = items(service.listOfSheet(req("Test1", "DoesNotExist").build())).stream()
				.map(ExcelSheet::getName).toList();
		assertEquals(List.of("Sheet1", "Sheet2", "Sheet3"), got);
	}

	@Test
	void sheetNamesWithSpacesArePreserved() {
		XSSFWorkbook wb = new XSSFWorkbook();
		wb.createSheet("My Sheet");
		wb.createSheet("Other");
		ExcelRepositoryImpl repo = new ExcelRepositoryImpl();
		repo.add(resource("Spaced", "Spaced.xlsx", bytes(wb)));
		assertEquals(List.of("My Sheet", "Other"), sheets(service(repo), "Spaced"));
	}

	@ParameterizedTest(name = "unknown resource \"{0}\"")
	@ValueSource(strings = { "NOPE", "kyc" })
	void unknownResourceIsNotFound(String resource) {
		var e = failure(service.listOfSheet(ExcelRequest.newBuilder().setResource(resource).build()),
				Status.Code.NOT_FOUND);
		assertTrue(e.getStatus().getDescription().contains(resource));
	}

	@ParameterizedTest(name = "invalid resource name \"{0}\"")
	@ValueSource(strings = { "", "bad name", "../KYC", "KYC.xlsx" })
	void malformedResourceNameIsInvalidArgument(String resource) {
		failure(service.listOfSheet(ExcelRequest.newBuilder().setResource(resource).build()),
				Status.Code.INVALID_ARGUMENT);
	}

	@Test
	void closedRepositoryIsInternal() {
		ExcelRepositoryImpl repo = repo("KYC");
		ExcelService s = service(repo);
		repo.close();
		var e = failure(s.listOfSheet(ExcelRequest.newBuilder().setResource("KYC").build()), Status.Code.INTERNAL);
		assertEquals("Internal error", e.getStatus().getDescription());
	}

	@Test
	void errorsAreDeliveredOnTheStreamNotThrownToTheCaller() {
		assertDoesNotThrow(() -> service.listOfSheet(ExcelRequest.newBuilder().setResource("NOPE").build()));
	}
}
