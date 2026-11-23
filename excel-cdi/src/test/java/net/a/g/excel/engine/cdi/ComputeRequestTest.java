package net.a.g.excel.engine.cdi;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import net.a.g.excel.exception.ExcelCalculationException;
import net.a.g.excel.exception.ExcelInvalidInputException;
import net.a.g.excel.exception.ExcelResourceNotFoundException;
import net.a.g.excel.model.ExcelCell;
import net.a.g.excel.model.ExcelRequest;
import net.a.g.excel.model.ExcelResult;

@DisplayName("Unified compute(ExcelRequest) with API names and cell references")
public class ComputeRequestTest extends CdiTestSupport {

	@BeforeEach
	void loadWorkbooks() {
		load("KYC", "KYCAPI.xlsx");
	}

	static ExcelRequest request(String sheet, Map<String, String> inputs, String... outputs) {
		ExcelRequest r = new ExcelRequest();
		r.setResource("KYC");
		r.setSheet(sheet);
		r.setInputs(inputs == null ? null : new HashMap<>(inputs));
		r.setOutputs(outputs == null ? null : List.of(outputs));
		return r;
	}

	private Object score(ExcelRequest r) {
		ExcelResult res = engine.compute(r);
		assertEquals(1, res.getCount());
		return res.getCells().iterator().next().getValue();
	}

	// PEP: +50 | COUNTRY FR/JP 0, CY 25, RU 50, KP 100 | AMOUNT <1e5 0, 1e5 25, 1e6 50, 1e7 75, >=1e8 100
	@ParameterizedTest(name = "PEP={0} COUNTRY={1} AMOUNT={2} -> SCORE {3}")
	@CsvSource({ "false,FR,0,0.0", "false,CY,99999,25.0", "false,RU,100000,75.0", "true,JP,999999,75.0",
			"false,KP,10000000,175.0", "true,KP,100000000,250.0" })
	void scoreByApiNames(String pep, String country, String amount, double expected) {
		ExcelRequest r = request("ComputeKYC", Map.of("PEP", pep, "COUNTRY", country, "AMOUNT", amount), "SCORE");
		assertEquals(expected, score(r));
	}

	@ParameterizedTest(name = "inputs {0}/{1}/{2} output {3}")
	@CsvSource({ "B2,B3,B4,C6", "ComputeKYC!B2,ComputeKYC!B3,ComputeKYC!B4,ComputeKYC!C6", "PEP,B3,ComputeKYC!B4,SCORE" })
	void namesRelativeAndAbsoluteReferencesAreEquivalent(String pep, String country, String amount, String out) {
		ExcelRequest r = request("ComputeKYC", Map.of(pep, "true", country, "CY", amount, "1000000"), out);
		assertEquals(125.0, score(r));
	}

	@Test
	void absoluteReferencesWorkWithoutSheet() {
		ExcelRequest r = request(null, Map.of("ComputeKYC!B2", "true"), "ComputeKYC!C6", "ComputeKYC!C2");
		ExcelResult res = engine.compute(r);
		assertThat(res.getCells().stream().map(ExcelCell::getValue).collect(toList()), contains(50.0, 50.0));
	}

	@Test
	void missingOutputsDefaultToEveryDeclaredOutput() {
		ExcelResult res = engine.compute(request("ComputeKYC", Map.of("COUNTRY", "RU"), (String[]) null));
		assertEquals(1, res.getCount());
		ExcelCell cell = res.getCells().iterator().next();
		assertEquals("ComputeKYC!C6", cell.getAddress());
		assertEquals("@output(SCORE)", cell.getMetadata());
		assertEquals(50.0, cell.getValue());
	}

	@Test
	void formulasAreAlwaysEvaluatedAndUuidIsEchoed() {
		ExcelRequest r = request("ComputeKYC", null, "SCORE");
		r.setUuid("req-42");
		ExcelResult res = engine.compute(r);
		assertEquals("req-42", res.getUuid());
		assertEquals(0.0, res.getCells().iterator().next().getValue());
	}

	@Test
	void blankSheetIsTreatedAsNoSheet() {
		ExcelInvalidInputException ex = assertThrows(ExcelInvalidInputException.class,
				() -> engine.compute(request("  ", Map.of(), "SCORE")));
		assertThat(ex.getMessage(), containsString("Unknown output 'SCORE'"));
	}

	@Test
	void noOutputAndNoSheetIsRejected() {
		assertThrows(ExcelInvalidInputException.class, () -> engine.compute(request(null, Map.of())));
	}

	@Test
	void unnamedAnnotationsGiveNoDefaultOutput() {
		load("KYCOLD", "KYC.xlsx");
		ExcelRequest r = request("ComputeKYC", Map.of());
		r.setResource("KYCOLD");
		assertThrows(ExcelInvalidInputException.class, () -> engine.compute(r));
	}

	@ParameterizedTest(name = "unknown {0}")
	@CsvSource({ "output,SCOREX,", "input,,b2" })
	void unknownNamesAreTypedErrors(String kind, String out, String in) {
		ExcelRequest r = out != null ? request("ComputeKYC", Map.of(), out) : request("ComputeKYC", Map.of(in, "1"), "SCORE");
		ExcelInvalidInputException ex = assertThrows(ExcelInvalidInputException.class, () -> engine.compute(r));
		assertThat(ex.getMessage(), containsString("Unknown " + kind));
	}

	@Test
	void nullRequestIsRejected() {
		assertThrows(ExcelInvalidInputException.class, () -> engine.compute(null));
	}

	@Test
	void unknownResourceAndSheetAreNotFound() {
		ExcelRequest r = request("ComputeKYC", Map.of(), "SCORE");
		r.setResource("Missing");
		assertThrows(ExcelResourceNotFoundException.class, () -> engine.compute(r));
		assertThrows(ExcelResourceNotFoundException.class, () -> engine.compute(request("Nope", Map.of(), "C6")));
	}

	@ParameterizedTest(name = "value ''{0}''")
	@ValueSource(strings = { "=1+1", "-1", "@SUM(1)" })
	void formulaInjectionInInputsIsRejected(String value) {
		ExcelInvalidInputException ex = assertThrows(ExcelInvalidInputException.class,
				() -> engine.compute(request("ComputeKYC", Map.of("AMOUNT", value), "SCORE")));
		assertEquals("ComputeKYC!B4", ex.getInputName());
	}

	@Test
	void crossWorkbookReferencesNeedGlobal() {
		load("Primary", "Primary.xlsx");
		load("Secondary", "Secondary.xlsx");
		ExcelRequest r = new ExcelRequest();
		r.setResource("Primary");
		r.setSheet("Feuil1");
		r.setInputs(Map.of("C1", "10"));
		r.setOutputs(List.of("B1"));
		r.setGlobal(true);
		assertEquals(54.0, engine.compute(r).getCells().iterator().next().getValue());
		r.setGlobal(false);
		assertThrows(ExcelCalculationException.class, () -> engine.compute(r));
	}
}
