package net.a.g.excel.kafka;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.smallrye.reactive.messaging.kafka.Record;
import net.a.g.excel.engine.ExcelEngine;
import net.a.g.excel.model.ExcelCell;
import net.a.g.excel.model.ExcelResult;

/**
 * ExcelConsumer.receive with a real engine over ../sample/KYCAPI.xlsx and KYC.xlsx.
 * KYCAPI!ComputeKYC: SCORE = 50*PEP + COUNTRY(FR 0, JP 0, CY 25, RU 50, KP 100)
 * + AMOUNT band (>=0: 0, >=1e5: 25, >=1e6: 50, >=1e7: 75, >=1e8: 100).
 */
public class ExcelConsumerTest {

	private static final String KEY = "record-key";

	private ExcelConsumer consumer;
	private ObjectMapper om;

	@BeforeEach
	public void setup() {
		om = KafkaTestSupport.om();
		consumer = new ExcelConsumer();
		consumer.engine = KafkaTestSupport.engine();
		consumer.om = om;
	}

	private ExcelResult receive(String key, String json) {
		return consumer.receive(Record.of(key, json));
	}

	private ExcelResult receive(String json) {
		return receive(KEY, json);
	}

	/** JSON for KYCAPI!ComputeKYC; null map values are left out of "inputs". */
	private String kyc(String uuid, Map<String, String> inputs, List<String> outputs) throws Exception {
		Map<String, Object> req = new LinkedHashMap<>();
		req.put("uuid", uuid);
		req.put("resource", "KYCAPI");
		req.put("sheet", "ComputeKYC");
		if (outputs != null) {
			req.put("outputs", outputs);
		}
		Map<String, String> in = new LinkedHashMap<>();
		inputs.forEach((k, v) -> {
			if (v != null) {
				in.put(k, v);
			}
		});
		req.put("inputs", in);
		return om.writeValueAsString(req);
	}

	private static Map<String, String> in(String pep, String country, String amount) {
		Map<String, String> m = new LinkedHashMap<>();
		m.put("PEP", pep);
		m.put("COUNTRY", country);
		m.put("AMOUNT", amount);
		return m;
	}

	private static double score(ExcelResult res) {
		assertNull(res.getError(), () -> "unexpected error " + res.getError().getError());
		assertEquals(1, res.getCount());
		return ((Number) res.getCells().iterator().next().getValue()).doubleValue();
	}

	private static Map<String, Object> byAddress(ExcelResult res) {
		return res.getCells().stream().collect(Collectors.toMap(ExcelCell::getAddress, ExcelCell::getValue));
	}

	private static void assertError(ExcelResult res, String code, String uuid) {
		assertNotNull(res.getError(), "an error result was expected");
		assertEquals(code, res.getError().getCode());
		assertEquals(uuid, res.getUuid());
		assertNull(res.getCells());
	}

	// ---------------------------------------------------------------- valid compute

	@Test
	public void computesScoreByApiNames() throws Exception {
		ExcelResult res = receive(kyc("u-1", in("true", "CY", "1000000"), List.of("SCORE")));

		assertEquals("u-1", res.getUuid());
		assertEquals(125.0, score(res));
		ExcelCell cell = res.getCells().iterator().next();
		assertEquals("ComputeKYC!C6", cell.getAddress());
		assertEquals("NUMERIC", cell.getType());
		assertEquals("@output(SCORE)", cell.getMetadata());
	}

	@Test
	public void pepAloneGives50() throws Exception {
		assertEquals(50.0, score(receive(kyc("u", in("true", null, null), List.of("SCORE")))));
	}

	@Test
	public void noInputsEvaluatesTemplateDefaults() throws Exception {
		// template: PEP false, COUNTRY FR, AMOUNT 0
		assertEquals(0.0, score(receive(kyc("u", Map.of(), List.of("SCORE")))));
	}

	@Test
	public void acceptsJsonBooleanAndNumberLiteralsAsInputs() throws Exception {
		// ../sample/api_post_ComputeKYC.json carries "PEP": true and "AMOUNT": 1000000 (not strings)
		String json = Files.readString(Path.of("../sample/api_post_ComputeKYC.json"));
		ExcelResult res = receive(json);
		assertEquals("b00f82d6-206a-450b-bd31-d1d3a3ac5d4a", res.getUuid());
		assertEquals(125.0, score(res));
	}

	@Test
	public void computesCellReferenceSampleOnWorkbookWithoutApiNames() throws Exception {
		// ../sample/kafka_ComputeKYC.json: resource KYC, outputs C6,C4,C3,C2, inputs ComputeKYC!B2..B4
		String json = Files.readString(Path.of("../sample/kafka_ComputeKYC.json"));
		ExcelResult res = receive(json);

		assertNull(res.getError());
		assertEquals("b00f82d6-206a-450b-bd31-d1d3a3ac5d4a", res.getUuid());
		assertEquals(4, res.getCount());
		Map<String, Object> cells = byAddress(res);
		assertEquals(125.0, cells.get("ComputeKYC!C6"));
		assertEquals(50.0, cells.get("ComputeKYC!C4"));
		assertEquals(25.0, cells.get("ComputeKYC!C3"));
		assertEquals(50.0, cells.get("ComputeKYC!C2"));
	}

	@Test
	public void outputsKeepRequestedOrder() throws Exception {
		ExcelResult res = receive(kyc("u", in("true", "CY", "1000000"), List.of("C2", "SCORE", "C3")));
		List<String> addresses = res.getCells().stream().map(ExcelCell::getAddress).toList();
		assertEquals(List.of("ComputeKYC!C2", "ComputeKYC!C6", "ComputeKYC!C3"), addresses);
	}

	@ParameterizedTest(name = "output {0}")
	@CsvSource({ "SCORE, ComputeKYC!C6, 125.0", "C6, ComputeKYC!C6, 125.0", "ComputeKYC!C6, ComputeKYC!C6, 125.0",
			"C3, ComputeKYC!C3, 25.0", "COUNTRY!B5, COUNTRY!B5, 100.0" })
	public void outputCanBeApiNameRelativeOrAbsoluteReference(String output, String address, double value)
			throws Exception {
		ExcelResult res = receive(kyc("u", in("true", "CY", "1000000"), List.of(output)));
		assertEquals(address, res.getCells().iterator().next().getAddress());
		assertEquals(value, score(res));
	}

	@Test
	public void inputsByCellReferenceEqualInputsByName() throws Exception {
		Map<String, String> refs = Map.of("B2", "true", "ComputeKYC!B3", "CY", "B4", "1000000");
		assertEquals(125.0, score(receive(kyc("u", refs, List.of("SCORE")))));
	}

	@Test
	public void unknownJsonPropertiesAreIgnored() throws Exception {
		String json = "{\"uuid\":\"u\",\"resource\":\"KYCAPI\",\"sheet\":\"ComputeKYC\",\"extra\":42,"
				+ "\"outputs\":[\"SCORE\"],\"inputs\":{\"PEP\":\"true\"}}";
		assertEquals(50.0, score(receive(json)));
	}

	// ---------------------------------------------------------------- default outputs

	@ParameterizedTest(name = "outputs {0}")
	@CsvSource(value = { "absent", "empty", "null" })
	public void defaultOutputsAreTheSheetOutputs(String variant) throws Exception {
		String outputs = switch (variant) {
		case "empty" -> ",\"outputs\":[]";
		case "null" -> ",\"outputs\":null";
		default -> "";
		};
		String json = "{\"uuid\":\"d\",\"resource\":\"KYCAPI\",\"sheet\":\"ComputeKYC\"" + outputs
				+ ",\"inputs\":{\"PEP\":\"true\",\"COUNTRY\":\"CY\",\"AMOUNT\":\"1000000\"}}";
		ExcelResult res = receive(json);
		assertEquals("d", res.getUuid());
		assertEquals("ComputeKYC!C6", res.getCells().iterator().next().getAddress());
		assertEquals(125.0, score(res));
	}

	@Test
	public void noDefaultOutputsWithoutSheet() throws Exception {
		ExcelResult res = receive("{\"uuid\":\"d\",\"resource\":\"KYCAPI\"}");
		assertError(res, "400", "d");
		assertTrue(res.getError().getError().contains("At least one output"));
	}

	@Test
	public void noDefaultOutputsOnSheetWithoutApiNames() throws Exception {
		assertError(receive("{\"uuid\":\"d\",\"resource\":\"KYC\",\"sheet\":\"ComputeKYC\"}"), "400", "d");
	}

	@Test
	public void absoluteReferencesWorkWithoutSheet() throws Exception {
		String json = "{\"uuid\":\"n\",\"resource\":\"KYCAPI\",\"outputs\":[\"ComputeKYC!C6\"],"
				+ "\"inputs\":{\"ComputeKYC!B2\":\"true\"}}";
		assertEquals(50.0, score(receive(json)));
	}

	// ---------------------------------------------------------------- errors

	static Stream<Arguments> invalidJson() {
		return Stream.of(Arguments.of("truncated", "{\"uuid\":\"x\",\"resource\":"),
				Arguments.of("not json", "not json at all"), Arguments.of("empty", ""),
				Arguments.of("array", "[1,2]"), Arguments.of("wrong type", "{\"outputs\":\"SCORE\",\"inputs\":[1]}"));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("invalidJson")
	public void invalidJsonIs400CorrelatedByRecordKey(String label, String json) {
		ExcelResult res = receive("k-" + label, json);
		assertError(res, "400", "k-" + label);
		assertTrue(res.getError().getError().startsWith("Invalid JSON request"), res.getError().getError());
	}

	@Test
	public void nullRecordValueIs400WithRecordKey() {
		assertError(receive("k-null", null), "400", "k-null");
	}

	@Test
	public void jsonNullLiteralIs400WithRecordKey() {
		assertError(receive("k-lit", "null"), "400", "k-lit");
	}

	@ParameterizedTest(name = "{0} -> {2}")
	@CsvSource(delimiter = '|', value = {
			// label | json fragment after uuid | code
			"unknown resource | \"resource\":\"NOPE\",\"sheet\":\"ComputeKYC\",\"outputs\":[\"SCORE\"] | 404",
			"unknown sheet | \"resource\":\"KYCAPI\",\"sheet\":\"Nope\",\"outputs\":[\"SCORE\"] | 404",
			"sheet of other workbook name | \"resource\":\"KYC\",\"sheet\":\"KYCAPI\",\"outputs\":[\"C6\"] | 404",
			"missing resource | \"sheet\":\"ComputeKYC\",\"outputs\":[\"SCORE\"] | 400",
			"path-like resource | \"resource\":\"../KYCAPI\",\"outputs\":[\"ComputeKYC!C6\"] | 400",
			"unknown output name | \"resource\":\"KYCAPI\",\"sheet\":\"ComputeKYC\",\"outputs\":[\"NOPE\"] | 400",
			"blank output | \"resource\":\"KYCAPI\",\"sheet\":\"ComputeKYC\",\"outputs\":[\" \"] | 400",
			"name without sheet | \"resource\":\"KYCAPI\",\"outputs\":[\"SCORE\"] | 400",
			"unknown input name | \"resource\":\"KYCAPI\",\"sheet\":\"ComputeKYC\",\"inputs\":{\"NOPE\":\"1\"} | 400",
			"bad number | \"resource\":\"KYCAPI\",\"sheet\":\"ComputeKYC\",\"inputs\":{\"AMOUNT\":\"abc\"} | 400",
			"number with comma | \"resource\":\"KYCAPI\",\"sheet\":\"ComputeKYC\",\"inputs\":{\"AMOUNT\":\"1,5\"} | 400",
			"empty number | \"resource\":\"KYCAPI\",\"sheet\":\"ComputeKYC\",\"inputs\":{\"AMOUNT\":\"\"} | 400",
			"formula injection | \"resource\":\"KYCAPI\",\"sheet\":\"ComputeKYC\",\"inputs\":{\"COUNTRY\":\"=1+1\"} | 400",
			"negative number rejected as injection | \"resource\":\"KYCAPI\",\"sheet\":\"ComputeKYC\",\"inputs\":{\"AMOUNT\":\"-5\"} | 400" })
	public void errorsMapToCodeAndKeepRequestUuid(String label, String fragment, String code) {
		String uuid = "uuid-" + label.replace(' ', '-');
		ExcelResult res = receive("other-key", "{\"uuid\":\"" + uuid + "\"," + fragment + "}");
		assertError(res, code, uuid);
		assertFalse(res.getError().getError().isBlank());
	}

	@Test
	public void unknownResourceMessageNamesTheResource() {
		ExcelResult res = receive("{\"uuid\":\"r\",\"resource\":\"NOPE\",\"outputs\":[\"A!A1\"]}");
		assertEquals("Excel resource not found: NOPE", res.getError().getError());
	}

	@Test
	public void badNumberMessageNamesTheCellAndValue() throws Exception {
		ExcelResult res = receive(kyc("b", in(null, null, "abc"), List.of("SCORE")));
		assertTrue(res.getError().getError().contains("'B4'"), res.getError().getError());
		assertTrue(res.getError().getError().contains("abc"), res.getError().getError());
	}

	@Test
	public void unexpectedEngineFailureIs500WithoutLeakingDetails() {
		consumer.engine = KafkaTestSupport.fake(ExcelEngine.class, (name, args) -> {
			throw new IllegalStateException("secret stack detail");
		});
		ExcelResult res = receive("{\"uuid\":\"boom\",\"resource\":\"KYCAPI\"}");
		assertError(res, "500", "boom");
		assertEquals("Internal error", res.getError().getError());
	}

	@Test
	public void outputOnUnknownSheetIsAnError() {
		ExcelResult res = receive("{\"uuid\":\"o\",\"resource\":\"KYCAPI\",\"outputs\":[\"NOPE!A1\"]}");
		assertNotNull(res.getError());
	}

	@Test
	public void inputOnUnknownSheetIsAClientError() {
		ExcelResult res = receive("{\"uuid\":\"i\",\"resource\":\"KYCAPI\",\"outputs\":[\"ComputeKYC!C6\"],"
				+ "\"inputs\":{\"NOPE!A1\":\"1\"}}");
		assertError(res, "400", "i");
	}

	// ---------------------------------------------------------------- uuid correlation

	@Test
	public void requestUuidWinsOverRecordKey() throws Exception {
		assertEquals("from-json", receive("from-key", kyc("from-json", Map.of(), List.of("SCORE"))).getUuid());
	}

	@Test
	public void successAndErrorResultsAreCorrelatedAlike() throws Exception {
		ExcelResult ok = receive("k", kyc("same", in("true", null, null), List.of("SCORE")));
		ExcelResult ko = receive("k", kyc("same", in(null, null, "abc"), List.of("SCORE")));
		assertEquals(ok.getUuid(), ko.getUuid());
	}

	@Test
	public void missingUuidFallsBackToRecordKey() {
		ExcelResult res = receive("the-key",
				"{\"resource\":\"KYCAPI\",\"sheet\":\"ComputeKYC\",\"inputs\":{\"PEP\":\"true\"}}");
		assertEquals("the-key", res.getUuid());
	}

	@Test
	public void missingUuidStillYieldsANonNullUuid() {
		// current behaviour behind the BUG above: a fresh random uuid, never null
		ExcelResult res = receive("the-key",
				"{\"resource\":\"KYCAPI\",\"sheet\":\"ComputeKYC\",\"inputs\":{\"PEP\":\"true\"}}");
		assertNotNull(res.getUuid());
		assertEquals(50.0, score(res));
	}

	// ---------------------------------------------------------------- wire format

	@Test
	public void successResultSerialisesWithoutError() throws Exception {
		JsonNode n = om.readTree(om.writeValueAsString(receive(kyc("w", in("true", "CY", "1000000"), null))));
		assertEquals("w", n.get("uuid").asText());
		assertEquals(1, n.get("_count").asInt());
		assertEquals(125.0, n.get("cells").get(0).get("value").asDouble());
		assertFalse(n.has("error"));
	}

	@Test
	public void errorResultSerialisesCodeAndMessageWithoutCells() throws Exception {
		JsonNode n = om.readTree(om.writeValueAsString(receive("{\"uuid\":\"e\",\"resource\":\"NOPE\"}")));
		assertEquals("e", n.get("uuid").asText());
		assertEquals("404", n.get("error").get("code").asText());
		assertTrue(n.get("error").has("message"));
		assertFalse(n.has("cells"));
		assertEquals(0, n.get("_count").asInt());
	}

	// ---------------------------------------------------------------- input combinations

	private static final Map<String, Double> COUNTRY = new LinkedHashMap<>(
			Map.of("FR", 0.0, "JP", 0.0, "CY", 25.0, "RU", 50.0, "KP", 100.0));
	private static final Map<String, Double> AMOUNT = new LinkedHashMap<>();
	static {
		AMOUNT.put("0", 0.0);
		AMOUNT.put("100000", 25.0);
		AMOUNT.put("1000000", 50.0);
		AMOUNT.put("10000000", 75.0);
		AMOUNT.put("100000000", 100.0);
		AMOUNT.put("2500000", 50.0);
	}

	static Stream<Arguments> matrix() {
		List<Arguments> ret = new ArrayList<>();
		for (boolean pep : new boolean[] { false, true }) {
			COUNTRY.forEach((c, cs) -> AMOUNT.forEach(
					(a, as) -> ret.add(Arguments.of(Boolean.toString(pep), c, a, (pep ? 50.0 : 0.0) + cs + as))));
		}
		return ret.stream();
	}

	@ParameterizedTest(name = "PEP={0} COUNTRY={1} AMOUNT={2} -> {3}")
	@MethodSource("matrix")
	public void scoreMatrix(String pep, String country, String amount, double expected) throws Exception {
		assertEquals(expected, score(receive(kyc("m", in(pep, country, amount), List.of("SCORE")))));
	}

	@ParameterizedTest(name = "AMOUNT={0} -> {1}")
	@CsvSource({ "99999.99, 0.0", "100000.01, 25.0", "999999, 25.0", "1e6, 50.0", "1.5E7, 75.0", "1000000000, 100.0",
			"0.0, 0.0" })
	public void amountBands(String amount, double expected) throws Exception {
		assertEquals(expected, score(receive(kyc("a", in(null, null, amount), List.of("SCORE")))));
	}

	@ParameterizedTest(name = "PEP={0} -> {1}")
	@CsvSource({ "TRUE, 50.0", "True, 50.0", "false, 0.0", "yes, 0.0", "1, 0.0" })
	public void pepIsParsedWithBooleanParseBoolean(String pep, double expected) throws Exception {
		// shortcut: anything but a case-insensitive "true" silently means false (no 400 for "yes" or "1")
		assertEquals(expected, score(receive(kyc("p", in(pep, null, null), List.of("SCORE")))));
	}

	@ParameterizedTest(name = "COUNTRY={0}")
	@CsvSource({ "XX", "DE", "' FR'" })
	public void unknownCountryYieldsExcelErrorCellNotAnErrorResult(String country) throws Exception {
		// VLOOKUP exact match fails inside the workbook: the formula result is #N/A, surfaced as type ERROR
		ExcelResult res = receive(kyc("c", in(null, country, null), List.of("SCORE")));
		assertNull(res.getError());
		ExcelCell cell = res.getCells().iterator().next();
		assertEquals("ERROR", cell.getType());
		assertEquals("#ERROR", cell.getValue());
	}

	@Test
	public void countryLookupIsCaseInsensitiveLikeExcel() throws Exception {
		assertEquals(25.0, score(receive(kyc("c", in(null, "cy", null), List.of("SCORE")))));
	}

	// ---------------------------------------------------------------- isolation and concurrency

	@Test
	public void inputsDoNotLeakIntoTheNextRequest() throws Exception {
		assertEquals(200.0, score(receive(kyc("1", in("true", "KP", "1000000"), List.of("SCORE")))));
		assertEquals(0.0, score(receive(kyc("2", Map.of(), List.of("SCORE")))));
	}

	@Test
	public void concurrentRequestsAreComputedIndependentlyAndCorrelated() throws Exception {
		List<Arguments> cases = matrix().toList();
		int n = 200;
		ExecutorService pool = Executors.newFixedThreadPool(16);
		try {
			List<Callable<ExcelResult>> tasks = new ArrayList<>();
			for (int i = 0; i < n; i++) {
				Object[] c = cases.get(i % cases.size()).get();
				String json = kyc("c-" + i, in((String) c[0], (String) c[1], (String) c[2]), List.of("SCORE"));
				tasks.add(() -> receive("k", json));
			}
			List<Future<ExcelResult>> futures = pool.invokeAll(tasks, 120, TimeUnit.SECONDS);
			for (int i = 0; i < n; i++) {
				ExcelResult res = futures.get(i).get();
				assertEquals("c-" + i, res.getUuid());
				assertEquals((double) cases.get(i % cases.size()).get()[3], score(res), "request c-" + i);
			}
		} finally {
			pool.shutdownNow();
		}
		// the cached template workbook must be untouched afterwards
		assertEquals(0.0, score(receive(kyc("after", Map.of(), List.of("SCORE")))));
	}

	@Test
	public void concurrentMixOfValidAndInvalidRequests() throws Exception {
		ExecutorService pool = Executors.newFixedThreadPool(8);
		try {
			List<Callable<ExcelResult>> tasks = new ArrayList<>();
			for (int i = 0; i < 80; i++) {
				String uuid = "x-" + i;
				String json = switch (i % 4) {
				case 0 -> kyc(uuid, in("true", "CY", "1000000"), List.of("SCORE"));
				case 1 -> kyc(uuid, in(null, null, "abc"), List.of("SCORE"));
				case 2 -> "{\"uuid\":\"" + uuid + "\",\"resource\":\"NOPE\"}";
				default -> "{broken";
				};
				tasks.add(() -> receive(uuid, json));
			}
			List<Future<ExcelResult>> futures = pool.invokeAll(tasks, 120, TimeUnit.SECONDS);
			for (int i = 0; i < 80; i++) {
				ExcelResult res = futures.get(i).get();
				assertEquals("x-" + i, res.getUuid());
				switch (i % 4) {
				case 0 -> assertEquals(125.0, score(res));
				case 1, 3 -> assertEquals("400", res.getError().getCode());
				default -> assertEquals("404", res.getError().getCode());
				}
			}
		} finally {
			pool.shutdownNow();
		}
	}
}
