package net.a.g.excel.engine.cdi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import net.a.g.excel.model.ExcelCell;
import net.a.g.excel.model.ExcelRequest;

@DisplayName("Concurrent use of the application-scoped engine")
@Timeout(value = 60, unit = TimeUnit.SECONDS)
public class ConcurrencyTest extends CdiTestSupport {

	private static final int THREADS = 16;
	private static final int TASKS = 200;

	private static final String[] COUNTRIES = { "FR", "JP", "CY", "RU", "KP" };
	private static final double[] COUNTRY_SCORE = { 0, 0, 25, 50, 100 };

	/** Runs every task released by one latch and returns their results in submission order. */
	private static <T> List<T> runAll(List<Callable<T>> tasks) throws Exception {
		ExecutorService pool = Executors.newFixedThreadPool(THREADS);
		CountDownLatch start = new CountDownLatch(1);
		try {
			List<Future<T>> futures = new ArrayList<>();
			for (Callable<T> t : tasks) {
				futures.add(pool.submit(() -> {
					start.await();
					return t.call();
				}));
			}
			start.countDown();
			List<T> out = new ArrayList<>();
			for (Future<T> f : futures) {
				out.add(f.get(30, TimeUnit.SECONDS));
			}
			return out;
		} finally {
			pool.shutdownNow();
		}
	}

	private static List<ExcelRequest> kycRequests(boolean byName, List<Double> expected) {
		List<ExcelRequest> out = new ArrayList<>();
		for (int i = 0; i < TASKS; i++) {
			boolean pep = i % 2 == 0;
			int c = i % COUNTRIES.length;
			ExcelRequest r = new ExcelRequest();
			r.setResource("KYC");
			if (byName) {
				r.setSheet("ComputeKYC");
				r.setInputs(Map.of("PEP", Boolean.toString(pep), "COUNTRY", COUNTRIES[c], "AMOUNT", "0"));
				r.setOutputs(List.of("SCORE"));
			} else {
				r.setInputs(Map.of("ComputeKYC!B2", Boolean.toString(pep), "ComputeKYC!B3", COUNTRIES[c]));
				r.setOutputs(List.of("ComputeKYC!C6"));
			}
			expected.add((pep ? 50.0 : 0.0) + COUNTRY_SCORE[c]);
			out.add(r);
		}
		return out;
	}

	private List<Double> computeAll(List<ExcelRequest> requests) throws Exception {
		List<Callable<Double>> tasks = new ArrayList<>();
		for (ExcelRequest r : requests) {
			tasks.add(() -> (Double) engine.compute(r).getCells().iterator().next().getValue());
		}
		return runAll(tasks);
	}

	@Test
	void parallelComputesByReferenceDoNotSeeEachOthersInputs() throws Exception {
		load("KYC", "KYCAPI.xlsx");
		List<Double> expected = new ArrayList<>();
		List<ExcelRequest> requests = kycRequests(false, expected);
		assertEquals(expected, computeAll(requests));
		assertEquals("SUM(C2:C4)", engine.cellCalculation("KYC", List.of("ComputeKYC!C6")).get(0).getValue());
	}

	@Test
	void parallelCellCalculationsByReference() throws Exception {
		load("T1", "Test1.xlsx");
		List<Callable<List<ExcelCell>>> tasks = new ArrayList<>();
		for (int i = 0; i < TASKS; i++) {
			String a1 = Integer.toString(i);
			tasks.add(() -> engine.cellCalculation("T1", List.of("Sheet2!A1", "Sheet3!A1"), Map.of("Sheet1!A1", a1)));
		}
		List<List<ExcelCell>> results = runAll(tasks);
		for (int i = 0; i < TASKS; i++) {
			assertEquals(i * 10.0 + 2, results.get(i).get(0).getValue());
			assertEquals(i + 12.0, results.get(i).get(1).getValue());
		}
	}

	@Test
	void parallelInjectionOfDistinctResources() throws Exception {
		List<Callable<Boolean>> tasks = new ArrayList<>();
		for (int i = 0; i < 40; i++) {
			String name = "R" + i;
			String file = i % 2 == 0 ? "Type.xlsx" : "Test1.xlsx";
			tasks.add(() -> {
				load(name, file);
				return engine.isSheetExists(name, "Sheet1");
			});
		}
		assertTrue(runAll(tasks).stream().allMatch(Boolean::booleanValue));
		assertEquals(40, repo.count());
	}

	@Test
	void apiNamesResolveUnderConcurrentFirstUse() throws Exception {
		load("KYC", "KYCAPI.xlsx");
		List<Double> expected = new ArrayList<>();
		List<ExcelRequest> requests = kycRequests(true, expected);
		List<Callable<Boolean>> tasks = new ArrayList<>();
		for (int i = 0; i < TASKS; i++) {
			ExcelRequest r = requests.get(i);
			double want = expected.get(i);
			tasks.add(i % 2 == 0 ? () -> engine.listOfAPI("KYC", "ComputeKYC").size() == 4
					: () -> engine.compute(r).getCells().iterator().next().getValue().equals(want));
		}
		assertTrue(runAll(tasks).stream().allMatch(Boolean::booleanValue));
	}
}
