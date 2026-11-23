package net.a.g.excel.lambda;

import static net.a.g.excel.lambda.LambdaFixture.inputs;
import static net.a.g.excel.lambda.LambdaFixture.request;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import net.a.g.excel.model.ExcelRequest;
import net.a.g.excel.model.ExcelResult;

/** One handler instance serves concurrent invocations (Lambda SnapStart / Quarkus share the bean). */
class ConcurrencyTest {

	private static final String[] PEP = { "true", "false" };
	private static final String[] COUNTRY = { "FR", "JP", "CY", "RU", "KP" };
	private static final double[] COUNTRY_W = { 0, 0, 25, 50, 100 };
	private static final String[] AMOUNT = { "0", "100000", "1000000", "10000000", "100000000" };
	private static final double[] AMOUNT_W = { 0, 25, 50, 75, 100 };

	private record Expect(ExcelRequest req, Object value) {
	}

	private static Expect kyc(int i) {
		int p = i % 2, c = (i / 2) % 5, a = (i / 10) % 5;
		ExcelRequest req = request("kyc-" + i, "KYCAPI", "ComputeKYC", List.of("SCORE"),
				inputs("PEP", PEP[p], "COUNTRY", COUNTRY[c], "AMOUNT", AMOUNT[a]));
		return new Expect(req, (p == 0 ? 50.0 : 0.0) + COUNTRY_W[c] + AMOUNT_W[a]);
	}

	private static Expect test1(int i) {
		ExcelRequest req = request("t1-" + i, "Test1", "Sheet2", List.of("A1"), inputs("Sheet1!A1", "" + i));
		return new Expect(req, i * 10.0 + 2);
	}

	private static Expect global(int i) {
		ExcelRequest req = request("gl-" + i, "Primary", "Feuil1", List.of("B1"), inputs("C1", "" + i));
		req.setGlobal(true);
		return new Expect(req, 44.0 + i);
	}

	private static Expect error(int i) {
		return new Expect(request("err-" + i, "NOPE" + i, null, List.of("A!A1"), null), "404");
	}

	private static void runAll(ExcelLambdaHandler h, List<Expect> work, int threads) throws Exception {
		ExecutorService pool = Executors.newFixedThreadPool(threads);
		CountDownLatch start = new CountDownLatch(1);
		try {
			List<Future<ExcelResult>> futures = new ArrayList<>();
			for (Expect e : work) {
				Callable<ExcelResult> call = () -> {
					start.await();
					return h.handleRequest(e.req(), null);
				};
				futures.add(pool.submit(call));
			}
			start.countDown();
			for (int i = 0; i < work.size(); i++) {
				Expect e = work.get(i);
				ExcelResult res = futures.get(i).get(60, TimeUnit.SECONDS);
				assertEquals(e.req().getUuid(), res.getUuid(), "uuid mismatch");
				if (e.value() instanceof String code) {
					assertEquals(code, res.getError().getCode(), e.req().getUuid());
				} else {
					assertNull(res.getError(), () -> e.req().getUuid() + ": " + res.getError().getError());
					assertEquals(e.value(), LambdaFixture.single(res), e.req().getUuid());
				}
			}
		} finally {
			pool.shutdownNow();
		}
	}

	@ParameterizedTest(name = "{0} threads, 200 KYCAPI requests")
	@ValueSource(ints = { 2, 8, 32 })
	@Timeout(120)
	void concurrentKycRequestsKeepTheirOwnInputsAndUuid(int threads) throws Exception {
		ExcelLambdaHandler h = LambdaFixture.handler("KYCAPI");
		List<Expect> work = new ArrayList<>();
		for (int i = 0; i < 200; i++) {
			work.add(kyc(i));
		}
		runAll(h, work, threads);
	}

	@Test
	@Timeout(120)
	void concurrentMixOfWorkbooksGlobalModeAndErrors() throws Exception {
		ExcelLambdaHandler h = LambdaFixture.handler("KYCAPI", "Test1", "Primary", "Secondary");
		List<Expect> work = new ArrayList<>();
		for (int i = 0; i < 160; i++) {
			work.add(switch (i % 4) {
			case 0 -> kyc(i);
			case 1 -> test1(i);
			case 2 -> global(i);
			default -> error(i);
			});
		}
		runAll(h, work, 16);
	}

	@Test
	@Timeout(60)
	void concurrentRequestsLeaveTemplateUntouched() throws Exception {
		ExcelLambdaHandler h = LambdaFixture.handler("KYCAPI");
		List<Expect> work = new ArrayList<>();
		for (int i = 0; i < 50; i++) {
			work.add(kyc(i));
		}
		runAll(h, work, 8);
		ExcelResult res = h.handleRequest(request("after", "KYCAPI", "ComputeKYC", List.of("SCORE"), Map.of()), null);
		assertEquals(0.0, LambdaFixture.single(res));
	}
}
