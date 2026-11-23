package net.a.g.excel.grpc;

import static net.a.g.excel.grpc.Fixtures.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.grpc.Status;

/**
 * Primary.xlsx Feuil1: A1 = [Secondary.xlsx]Feuil1!A1+B1+C1 (= 40+1+1), B1 = A1+2+C1, C1 = -2. The
 * {@code global} flag wires the other resources in as referenced workbooks; it only takes effect when the
 * request has inputs.
 */
class GlobalFlagTest {

	static ExcelService service;

	@BeforeAll
	static void setup() {
		service = service(ALL);
	}

	private static ExcelRequest.Builder primary(String c1, boolean global) {
		return req("Primary", "Feuil1").addOutputs("A1").addOutputs("B1").putInputs("C1", c1).setGlobal(global);
	}

	private static Map<String, Double> values(ExcelRequest.Builder b) {
		return items(service.computeCell(b.build())).stream()
				.collect(Collectors.toMap(ExcelCell::getAddress, Fixtures::dbl));
	}

	@ParameterizedTest(name = "C1={0} -> A1={1}, B1={2}")
	@CsvSource({ "0,42,44", "2,42,46", "10.5,42,54.5" })
	void globalResolvesCrossWorkbookReferences(String c1, double a1, double b1) {
		assertEquals(Map.of("Feuil1!A1", a1, "Feuil1!B1", b1), values(primary(c1, true)));
	}

	@Test
	void withoutGlobalTheCrossWorkbookReferenceCannotBeEvaluated() {
		var e = failure(service.computeCell(primary("2", false).build()), Status.Code.INTERNAL);
		assertEquals("Internal error", e.getStatus().getDescription());
	}


	@Test
	void localFormulaOfAGlobalWorkbookDoesNotNeedTheFlag() {
		ExcelCell c = single(service.computeCell(req("Primary", "Feuil1").addOutputs("C1").putInputs("C1", "7").build()));
		assertEquals(7.0, dbl(c));
	}

	@Test
	void globalFailsWhenTheReferencedWorkbookIsNotLoaded() {
		ExcelService s = service("Primary");
		failure(s.computeCell(primary("2", true).build()), Status.Code.INTERNAL);
	}

	@Test
	void globalUsesTheReferencedResourceTemplateNotItsPreviousInputs() {
		// an input sent to Secondary is applied to a private copy and must not change what Primary sees
		single(service.computeCell(req("Secondary", "Feuil1").addOutputs("A1").putInputs("A1", "1000").build()));
		assertEquals(42.0, values(primary("2", true)).get("Feuil1!A1"));
	}

	@ParameterizedTest(name = "global={0}")
	@ValueSource(booleans = { true, false })
	void globalDoesNotChangeASelfContainedComputation(boolean global) {
		assertEquals(125.0, dbl(single(service.computeCell(kycapi("true", "CY", "1000000").setGlobal(global).build()))));
	}

	@Test
	void globalOutputsKeepRequestOrder() {
		ExcelRequest.Builder b = req("Primary", "Feuil1").addOutputs("B1").addOutputs("A1").putInputs("C1", "2")
				.setGlobal(true);
		List<String> order = items(service.computeCell(b.build())).stream().map(ExcelCell::getAddress).toList();
		assertEquals(List.of("Feuil1!B1", "Feuil1!A1"), order);
	}
}
