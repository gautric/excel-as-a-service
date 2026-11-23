package net.a.g.excel.lambda;

import static net.a.g.excel.lambda.LambdaFixture.inputs;
import static net.a.g.excel.lambda.LambdaFixture.request;
import static net.a.g.excel.lambda.LambdaFixture.single;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import net.a.g.excel.model.ExcelRequest;
import net.a.g.excel.model.ExcelResult;

class NullFieldsAndUuidTest {

	private static ExcelLambdaHandler handler;

	@BeforeAll
	static void setup() {
		handler = LambdaFixture.handler("KYCAPI");
	}

	private static Map<String, String> withNull(String key, String value) {
		Map<String, String> m = new HashMap<>();
		m.put(key, value);
		return m;
	}

	// ---- null fields -------------------------------------------------------

	@Test
	void nullRequestIs400WithNullUuid() {
		ExcelResult res = assertDoesNotThrow(() -> handler.handleRequest(null, null));
		assertEquals("400", res.getError().getCode());
		assertNull(res.getUuid());
	}

	@Test
	void nullResourceIs400() {
		ExcelResult res = handler.handleRequest(request("r", null, "ComputeKYC", List.of("SCORE"), null), null);
		assertEquals("400", res.getError().getCode());
		assertEquals("r", res.getUuid());
	}

	@ParameterizedTest(name = "sheet=[{0}] with absolute refs")
	@NullAndEmptySource
	@ValueSource(strings = { "   " })
	void nullOrBlankSheetAllowsAbsoluteReferences(String sheet) {
		ExcelResult res = handler.handleRequest(request("s", "KYCAPI", sheet, List.of("ComputeKYC!C6"),
				inputs("ComputeKYC!B2", "true")), null);
		assertNull(res.getError());
		assertEquals(50.0, single(res));
	}

	@Test
	void nullInputsMapComputesDefaults() {
		ExcelResult res = handler.handleRequest(request("i", "KYCAPI", "ComputeKYC", List.of("SCORE"), null), null);
		assertNull(res.getError());
		assertEquals(0.0, single(res));
	}

	@Test
	void nullOutputsAndNullInputsTogether() {
		ExcelResult res = handler.handleRequest(request("o", "KYCAPI", "ComputeKYC", null, null), null);
		assertNull(res.getError());
		assertEquals("ComputeKYC!C6", LambdaFixture.cells(res).get(0).getAddress());
	}

	@Test
	void nullGlobalMeansLocal() {
		ExcelRequest req = request("g", "KYCAPI", "ComputeKYC", List.of("SCORE"), inputs("PEP", "true"));
		req.setGlobal(null);
		assertEquals(50.0, single(handler.handleRequest(req, null)));
	}

	@Test
	void nullOutputEntryIs400() {
		ExcelResult res = handler.handleRequest(request("oe", "KYCAPI", "ComputeKYC",
				java.util.Arrays.asList("SCORE", null), null), null);
		assertEquals("400", res.getError().getCode());
		assertEquals("oe", res.getUuid());
	}

	@Test
	void nullInputKeyIs400() {
		ExcelResult res = handler.handleRequest(
				request("ik", "KYCAPI", "ComputeKYC", List.of("SCORE"), withNull(null, "true")), null);
		assertEquals("400", res.getError().getCode());
	}

	@ParameterizedTest(name = "{0}=null is not an internal error")
	@ValueSource(strings = { "PEP", "COUNTRY", "AMOUNT" })
	void nullInputValueIsNotInternalError(String name) {
		ExcelResult res = handler.handleRequest(
				request("nv", "KYCAPI", "ComputeKYC", List.of("SCORE"), withNull(name, null)), null);
		if (res.getError() != null) {
			assertEquals("400", res.getError().getCode(), res.getError().getError());
		}
		assertEquals("nv", res.getUuid());
	}

	// ---- uuid correlation --------------------------------------------------

	@ParameterizedTest(name = "uuid [{0}] echoed on success")
	@NullAndEmptySource
	@ValueSource(strings = { "b00f82d6-206a-450b-bd31-d1d3a3ac5d4a", "not-a-uuid", "  spaced  ", "é-ü-ß-日本" })
	void uuidEchoedOnSuccess(String uuid) {
		ExcelResult res = handler.handleRequest(request(uuid, "KYCAPI", "ComputeKYC", List.of("SCORE"), null), null);
		assertNull(res.getError());
		assertEquals(uuid, res.getUuid());
	}

	@ParameterizedTest(name = "uuid [{0}] echoed on error")
	@NullAndEmptySource
	@ValueSource(strings = { "b00f82d6-206a-450b-bd31-d1d3a3ac5d4a", "not-a-uuid" })
	void uuidEchoedOnError(String uuid) {
		ExcelResult res = handler.handleRequest(request(uuid, "NOPE", null, List.of("A!A1"), null), null);
		assertEquals("404", res.getError().getCode());
		assertEquals(uuid, res.getUuid());
	}

	@Test
	void defaultConstructorGeneratesUuidThatIsEchoed() {
		ExcelRequest req = new ExcelRequest();
		req.setResource("KYCAPI");
		req.setSheet("ComputeKYC");
		assertNotNull(req.getUuid());
		assertDoesNotThrow(() -> UUID.fromString(req.getUuid()));
		assertEquals(req.getUuid(), handler.handleRequest(req, null).getUuid());
		assertNotEquals(req.getUuid(), new ExcelRequest().getUuid());
	}

	@Test
	void sameRequestObjectTwiceGivesSameUuidAndValue() {
		ExcelRequest req = request("again", "KYCAPI", "ComputeKYC", List.of("SCORE"), inputs("PEP", "true"));
		ExcelResult a = handler.handleRequest(req, null);
		ExcelResult b = handler.handleRequest(req, null);
		assertEquals(a.getUuid(), b.getUuid());
		assertEquals(single(a), single(b));
	}
}
