package net.a.g.excel.lambda;

import static net.a.g.excel.lambda.LambdaFixture.inputs;
import static net.a.g.excel.lambda.LambdaFixture.request;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import net.a.g.excel.engine.ExcelEngine;
import net.a.g.excel.exception.ExcelCalculationException;
import net.a.g.excel.exception.ExcelEngineException;
import net.a.g.excel.exception.ExcelInvalidInputException;
import net.a.g.excel.exception.ExcelResourceNotFoundException;
import net.a.g.excel.model.ExcelRequest;
import net.a.g.excel.model.ExcelResult;

/** Every failure comes back as ExcelResult.error with code 404/400/500 and the request uuid; nothing is thrown. */
class ErrorMappingTest {

	private static ExcelLambdaHandler handler;

	@BeforeAll
	static void setup() {
		handler = LambdaFixture.handler("KYCAPI", "KYC", "Test1");
	}

	private static void assertError(ExcelResult res, String code, String uuid) {
		assertNotNull(res.getError(), "expected error " + code);
		assertEquals(code, res.getError().getCode(), res.getError().getError());
		assertEquals(uuid, res.getUuid());
		assertNull(res.getCells());
		assertEquals(0, res.getCount());
	}

	// ---- 404 ---------------------------------------------------------------

	static Stream<Arguments> notFound() {
		return Stream.of(
				Arguments.of("unknown resource", request("n1", "NOPE", null, List.of("A!A1"), null)),
				Arguments.of("unknown resource with sheet", request("n2", "Missing", "ComputeKYC", List.of("SCORE"), null)),
				Arguments.of("resource name is case-sensitive", request("n3", "kycapi", "ComputeKYC", List.of("SCORE"), null)),
				Arguments.of("unknown sheet", request("n4", "KYCAPI", "Nope", List.of("A1"), null)),
				Arguments.of("sheet of another workbook", request("n6", "KYCAPI", "Sheet1", List.of("A1"), null)),
				Arguments.of("sheet name with forbidden chars", request("n7", "KYCAPI", "Bad/Sheet", List.of("A1"), null)));
	}

	@ParameterizedTest(name = "404: {0}")
	@MethodSource("notFound")
	void notFoundIs404(String label, ExcelRequest req) {
		assertError(handler.handleRequest(req, null), "404", req.getUuid());
	}

	@Test
	void notFoundMessageNamesTheResource() {
		ExcelResult res = handler.handleRequest(request("m", "Ghost", null, List.of("A!A1"), null), null);
		assertTrue(res.getError().getError().contains("Ghost"), res.getError().getError());
	}

	// ---- 400 ---------------------------------------------------------------

	static Stream<Arguments> badRequest() {
		return Stream.of(
				Arguments.of("invalid resource name", request("b1", "bad name!", null, List.of("A!A1"), null)),
				Arguments.of("empty resource", request("b2", "", null, List.of("A!A1"), null)),
				Arguments.of("blank resource", request("b3", "   ", null, List.of("A!A1"), null)),
				Arguments.of("no output and no sheet", request("b4", "KYCAPI", null, List.of(), null)),
				Arguments.of("no output and sheet without @output", request("b5", "Test1", "Sheet2", List.of(), null)),
				Arguments.of("unnamed @output comments give no default", request("b6", "KYC", "ComputeKYC", List.of(), null)),
				Arguments.of("relative output without sheet", request("b7", "KYCAPI", null, List.of("C6"), null)),
				Arguments.of("name output without sheet", request("b8", "KYCAPI", null, List.of("SCORE"), null)),
				Arguments.of("unknown output name", request("b9", "KYCAPI", "ComputeKYC", List.of("RISK"), null)),
				Arguments.of("output names are case-sensitive", request("b10", "KYCAPI", "ComputeKYC", List.of("score"), null)),
				Arguments.of("blank output", request("b11", "KYCAPI", "ComputeKYC", List.of(" "), null)),
				Arguments.of("malformed output ref", request("b12", "KYCAPI", null, List.of("ComputeKYC!1A"), null)),
				Arguments.of("unknown input name", request("b13", "KYCAPI", "ComputeKYC", List.of("SCORE"), inputs("RISK", "1"))),
				Arguments.of("input names are case-sensitive", request("b14", "KYCAPI", "ComputeKYC", List.of("SCORE"), inputs("pep", "true"))),
				Arguments.of("non-numeric amount", request("b15", "KYCAPI", "ComputeKYC", List.of("SCORE"), inputs("AMOUNT", "lots"))),
				Arguments.of("empty amount", request("b16", "KYCAPI", "ComputeKYC", List.of("SCORE"), inputs("AMOUNT", ""))),
				Arguments.of("formula injection =", request("b17", "KYCAPI", "ComputeKYC", List.of("SCORE"), inputs("COUNTRY", "=1+1"))),
				Arguments.of("formula injection @", request("b18", "KYCAPI", "ComputeKYC", List.of("SCORE"), inputs("COUNTRY", "@SUM(A1)"))),
				Arguments.of("formula injection + after spaces", request("b19", "KYCAPI", "ComputeKYC", List.of("SCORE"), inputs("COUNTRY", "  +CY"))),
				Arguments.of("leading minus is rejected as injection", request("b20", "KYCAPI", "ComputeKYC", List.of("SCORE"), inputs("AMOUNT", "-5"))),
				Arguments.of("value longer than an Excel cell", request("b21", "KYCAPI", "ComputeKYC", List.of("SCORE"), inputs("COUNTRY", "X".repeat(32768)))),
				Arguments.of("relative input without sheet", request("b22", "KYCAPI", null, List.of("ComputeKYC!C6"), inputs("B2", "true"))));
	}

	@ParameterizedTest(name = "400: {0}")
	@MethodSource("badRequest")
	void badRequestIs400(String label, ExcelRequest req) {
		assertError(handler.handleRequest(req, null), "400", req.getUuid());
	}

	@Test
	void valueAtExcelCellLimitIsAccepted() {
		ExcelResult res = handler.handleRequest(request("ok", "KYCAPI", "ComputeKYC", List.of("B3"),
				inputs("COUNTRY", "X".repeat(32767))), null);
		assertNull(res.getError());
	}

	@Test
	void badRequestMessageIsSurfaced() {
		ExcelResult res = handler.handleRequest(
				request("m", "KYCAPI", "ComputeKYC", List.of("SCORE"), inputs("AMOUNT", "lots")), null);
		String msg = res.getError().getError();
		assertTrue(msg.contains("lots") && msg.contains("B4"), msg);
	}

	@Test
	void sheetCaseIsHandledConsistently() {
		ExcelResult res = handler.handleRequest(
				request("cs", "KYCAPI", "computekyc", List.of("SCORE"), LambdaFixture.inputs("PEP", "true")), null);
		if (res.getError() != null) {
			assertEquals("404", res.getError().getCode(), res.getError().getError());
		} else {
			assertEquals(50.0, LambdaFixture.single(res));
		}
	}

	@Test
	void inputToMissingCellIs400() {
		ExcelResult res = handler.handleRequest(
				request("z", "KYCAPI", "ComputeKYC", List.of("SCORE"), inputs("Z99", "1")), null);
		assertError(res, "400", "z");
	}

	// ---- 500 ---------------------------------------------------------------

	private static ExcelLambdaHandler throwing(RuntimeException e) {
		ExcelLambdaHandler h = new ExcelLambdaHandler();
		h.engine = (ExcelEngine) Proxy.newProxyInstance(ExcelEngine.class.getClassLoader(),
				new Class<?>[] { ExcelEngine.class }, (p, m, a) -> {
					throw e;
				});
		return h;
	}

	static Stream<Arguments> exceptionCodes() {
		return Stream.of(
				Arguments.of(new ExcelResourceNotFoundException("X"), "404"),
				Arguments.of(new ExcelInvalidInputException("bad"), "400"),
				Arguments.of(new IllegalArgumentException("bad"), "400"),
				Arguments.of(new ExcelCalculationException("A1", "boom"), "500"),
				Arguments.of(new ExcelEngineException("boom"), "500"),
				Arguments.of(new IllegalStateException("boom"), "500"),
				Arguments.of(new NullPointerException(), "500"));
	}

	@ParameterizedTest(name = "{0} -> {1}")
	@MethodSource("exceptionCodes")
	void engineExceptionIsMappedNotThrown(RuntimeException e, String code) {
		ExcelRequest req = request("e-" + code, "KYCAPI", "ComputeKYC", List.of("SCORE"), null);
		ExcelResult res = assertDoesNotThrow(() -> throwing(e).handleRequest(req, null));
		assertError(res, code, req.getUuid());
	}

	@Test
	void internalErrorDoesNotLeakDetails() {
		ExcelResult res = throwing(new IllegalStateException("secret /etc/passwd")).handleRequest(
				request("s", "KYCAPI", null, List.of("A!A1"), null), null);
		assertEquals("Internal error", res.getError().getError());
	}

	@Test
	void missingEngineIsInternalError() {
		ExcelResult res = new ExcelLambdaHandler().handleRequest(request("ne", "KYCAPI", null, List.of("A!A1"), null),
				null);
		assertError(res, "500", "ne");
	}

	@Test
	void realEngineFailureIs500() {
		// Primary references Secondary.xlsx: without global mode the external link cannot be resolved.
		ExcelResult res = LambdaFixture.handler("Primary", "Secondary").handleRequest(
				request("x", "Primary", "Feuil1", List.of("A1"), inputs("C1", "1")), null);
		assertError(res, "500", "x");
		assertEquals("Internal error", res.getError().getError());
	}
}
