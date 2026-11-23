package net.a.g.excel.grpc;

import static net.a.g.excel.grpc.Fixtures.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.grpc.Status;

class ListOfCellTest {

	static ExcelService service;

	@BeforeAll
	static void setup() {
		service = service(ALL);
	}

	private static List<ExcelCell> cells(String resource, String sheet) {
		return items(service.listOfCell(req(resource, sheet).build()));
	}

	private static Map<String, ExcelCell> byAddress(String resource, String sheet) {
		return cells(resource, sheet).stream().collect(Collectors.toMap(ExcelCell::getAddress, c -> c));
	}

	@ParameterizedTest(name = "{0}!{1} has {2} cells")
	@CsvSource({ "KYC,ComputeKYC,15", "KYCAPI,COUNTRY,10", "Primary,Feuil1,3", "Test1,Sheet1,5", "Test1,Sheet2,2", "Test1,Sheet3,1",
			"Type,Sheet1,11" })
	void streamsEveryPhysicalCellOfTheSheet(String resource, String sheet, int count) {
		assertEquals(count, cells(resource, sheet).size());
	}

	@Test
	void cellsAreStreamedInRowMajorOrderWithSheetQualifiedAddresses() {
		List<String> addresses = cells("Test1", "Sheet1").stream().map(ExcelCell::getAddress).toList();
		assertEquals(List.of("Sheet1!A1", "Sheet1!B1", "Sheet1!A2", "Sheet1!B2", "Sheet1!A3"), addresses);
	}

	@Test
	void formulasAreReturnedAsTextNotEvaluated() {
		ExcelCell c6 = byAddress("KYCAPI", "ComputeKYC").get("ComputeKYC!C6");
		assertEquals("FORMULA", c6.getType());
		assertEquals("SUM(C2:C4)", str(c6));
	}

	@Test
	void commentsAreExposedAsMetadata() {
		Map<String, ExcelCell> m = byAddress("KYCAPI", "ComputeKYC");
		assertEquals("@input(PEP)", m.get("ComputeKYC!B2").getMetadata());
		assertEquals("@input(COUNTRY)", m.get("ComputeKYC!B3").getMetadata());
		assertEquals("@input(AMOUNT)", m.get("ComputeKYC!B4").getMetadata());
		assertEquals("@output(SCORE)", m.get("ComputeKYC!C6").getMetadata());
	}

	@Test
	void cellsWithoutCommentHaveNoMetadataField() {
		ExcelCell a2 = byAddress("KYCAPI", "ComputeKYC").get("ComputeKYC!A2");
		assertFalse(a2.hasMetadata());
		assertEquals("", a2.getMetadata());
	}

	@Test
	void everyCellCarriesAddressTypeAndValue() {
		for (ExcelCell c : cells("Type", "Sheet1")) {
			assertTrue(c.hasAddress(), c::toString);
			assertTrue(c.hasType(), c::toString);
			assertTrue(c.hasValue(), c::toString);
		}
	}

	@Test
	void sheetNameInADifferentCaseIsEitherFoundOrNotFound() {
		assertEquals(15, cells("KYCAPI", "computekyc").size());
	}

	@ParameterizedTest(name = "resource={0} sheet={1}")
	@CsvSource({ "NOPE,ComputeKYC", "KYCAPI,NOPE", "Test1,Sheet4", "Type,Sheet2", "KYCAPI,''", "KYCAPI,' '" })
	void unknownResourceOrSheetIsNotFound(String resource, String sheet) {
		var e = failure(service.listOfCell(req(resource, sheet).build()), Status.Code.NOT_FOUND);
		assertTrue(e.getStatus().getDescription().contains(resource));
	}

	@Test
	void notFoundDescriptionNamesTheSheet() {
		var e = failure(service.listOfCell(req("KYCAPI", "Missing").build()), Status.Code.NOT_FOUND);
		assertEquals("Sheet 'Missing' not found in resource 'KYCAPI'", e.getStatus().getDescription());
	}

	@Test
	void malformedResourceNameIsInvalidArgumentLikeTheOtherRpcs() {
		failure(service.listOfCell(req("bad name", "ComputeKYC").build()), Status.Code.INVALID_ARGUMENT);
	}

	@Test
	void listingDoesNotEvaluateOrMutateTheTemplateAfterACompute() {
		items(service.computeCell(kycapi("true", "KP", "100000000").build()));
		Map<String, ExcelCell> m = byAddress("KYCAPI", "ComputeKYC");
		assertFalse(bool(m.get("ComputeKYC!B2")));
		assertEquals("FR", str(m.get("ComputeKYC!B3")));
		assertEquals(0.0, dbl(m.get("ComputeKYC!B4")));
		assertEquals("SUM(C2:C4)", str(m.get("ComputeKYC!C6")));
	}
}
