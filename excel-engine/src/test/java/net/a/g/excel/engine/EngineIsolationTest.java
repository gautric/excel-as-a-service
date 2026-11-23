package net.a.g.excel.engine;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.a.g.excel.exception.ExcelInvalidInputException;
import net.a.g.excel.exception.ExcelResourceNotFoundException;
import net.a.g.excel.model.ExcelCell;
import net.a.g.excel.model.ExcelRequest;
import net.a.g.excel.model.ExcelResult;

/**
 * Regression tests: a calculation must never leak its inputs into, or destroy
 * the formulas of, the shared workbook seen by the next request.
 */
@DisplayName("Engine isolation and unified compute()")
public class EngineIsolationTest extends ExcelUnitTest {

	@BeforeEach
	@Override
	public void setup() throws IOException {
		super.setup();
		assertTrue(loader.injectResource("KYC", "KYC.xlsx", FileUtils.openInputStream(new File("../sample/KYC.xlsx"))));
		assertTrue(loader.injectResource("KYCAPI", "KYCAPI.xlsx",
				FileUtils.openInputStream(new File("../sample/KYCAPI.xlsx"))));
	}

	private Object c6(Map<String, String> input) {
		return engine.cellCalculation("KYC", List.of("ComputeKYC!C6"), input, false, false).get(0).getValue();
	}

	@Test
	public void formulaIsKeptAfterComputation() {
		assertThat(c6(Map.of("ComputeKYC!B2", "TRUE", "ComputeKYC!B3", "CY")), is(75.0));

		ExcelCell raw = engine.cellCalculation("KYC", List.of("ComputeKYC!C6")).get(0);
		assertEquals("FORMULA", raw.getType());
		assertEquals("SUM(C2:C4)", raw.getValue());
	}

	@Test
	public void inputsDoNotLeakToNextRequest() {
		assertThat(c6(Map.of("ComputeKYC!B2", "TRUE", "ComputeKYC!B3", "CY")), is(75.0));
		assertThat(c6(Map.of("ComputeKYC!B2", "TRUE")), is(50.0));
	}

	@Test
	public void concurrentRequestsAreIsolated() throws Exception {
		ExecutorService pool = Executors.newFixedThreadPool(8);
		try {
			List<Future<Boolean>> futures = new ArrayList<>();
			for (int i = 0; i < 200; i++) {
				final boolean even = i % 2 == 0;
				futures.add(pool.submit(() -> even
						? Double.valueOf(75.0).equals(c6(Map.of("ComputeKYC!B2", "TRUE", "ComputeKYC!B3", "CY")))
						: Double.valueOf(50.0).equals(c6(Map.of("ComputeKYC!B2", "TRUE")))));
			}
			for (Future<Boolean> f : futures) {
				assertTrue(f.get());
			}
		} finally {
			pool.shutdownNow();
		}
	}

	@Test
	public void computeWithApiNames() {
		ExcelRequest req = new ExcelRequest("id-1");
		req.setResource("KYCAPI");
		req.setSheet("ComputeKYC");
		req.setOutputs(List.of("SCORE"));
		req.setInputs(Map.of("PEP", "true", "COUNTRY", "CY", "AMOUNT", "1000000"));

		ExcelResult res = engine.compute(req);

		assertEquals("id-1", res.getUuid());
		assertEquals(1, res.getCount());
		assertThat(res.getCells().iterator().next().getValue(), is(125.0));
	}

	@Test
	public void computeDefaultsToAllOutputs() {
		ExcelRequest req = new ExcelRequest();
		req.setResource("KYCAPI");
		req.setSheet("ComputeKYC");
		req.setInputs(Map.of("PEP", "true"));

		ExcelResult res = engine.compute(req);
		assertThat(new ArrayList<>(res.getCells()), hasSize(1));
		assertThat(res.getCells().iterator().next().getValue(), is(50.0));
	}

	@Test
	public void computeWithCellReferences() {
		ExcelRequest req = new ExcelRequest();
		req.setResource("KYC");
		req.setSheet("ComputeKYC");
		req.setOutputs(List.of("C6", "ComputeKYC!C3"));
		req.setInputs(Map.of("B2", "TRUE", "ComputeKYC!B3", "CY"));

		List<ExcelCell> cells = new ArrayList<>(engine.compute(req).getCells());
		assertThat(cells.get(0).getValue(), is(75.0));
		assertThat(cells.get(1).getValue(), is(25.0));
	}

	@Test
	public void computeErrorsAreTyped() {
		ExcelRequest unknownResource = new ExcelRequest();
		unknownResource.setResource("NOPE");
		unknownResource.setOutputs(List.of("A!A1"));
		assertThrows(ExcelResourceNotFoundException.class, () -> engine.compute(unknownResource));

		ExcelRequest unknownSheet = new ExcelRequest();
		unknownSheet.setResource("KYC");
		unknownSheet.setSheet("NOPE");
		unknownSheet.setOutputs(List.of("A1"));
		assertThrows(ExcelResourceNotFoundException.class, () -> engine.compute(unknownSheet));

		ExcelRequest unknownOutput = new ExcelRequest();
		unknownOutput.setResource("KYCAPI");
		unknownOutput.setSheet("ComputeKYC");
		unknownOutput.setOutputs(List.of("NOT_A_NAME"));
		assertThrows(ExcelInvalidInputException.class, () -> engine.compute(unknownOutput));

		ExcelRequest badNumber = new ExcelRequest();
		badNumber.setResource("KYCAPI");
		badNumber.setSheet("ComputeKYC");
		badNumber.setInputs(Map.of("AMOUNT", "abc"));
		assertThrows(ExcelInvalidInputException.class, () -> engine.compute(badNumber));
	}
}
