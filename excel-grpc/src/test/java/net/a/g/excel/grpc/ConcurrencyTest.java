package net.a.g.excel.grpc;

import static net.a.g.excel.grpc.Fixtures.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;

class ConcurrencyTest {

	private static final String[] COUNTRIES = { "FR", "JP", "CY", "RU", "KP" };
	private static final double[] COUNTRY_SCORE = { 0, 0, 25, 50, 100 };
	private static final String[] AMOUNTS = { "0", "100000", "1000000", "10000000", "100000000" };
	private static final double[] AMOUNT_SCORE = { 0, 25, 50, 75, 100 };

	ExcelService service;
	ExecutorService pool;

	@BeforeEach
	void setup() {
		service = service(ALL);
		pool = Executors.newFixedThreadPool(16);
		// warm the shared workbook cache once: the cold-start race is covered by its own @Disabled test
		single(service.computeCell(kycapi("false", "FR", "0").build()));
	}

	@Test
	void coldStartParallelComputesByApiName() throws Exception {
		for (int round = 0; round < 20; round++) {
			ExcelService cold = service("KYCAPI");
			List<Callable<Double>> tasks = new ArrayList<>();
			for (int i = 0; i < 16; i++) {
				ExcelRequest r = kycapi("true", "CY", "1000000").build();
				tasks.add(() -> dbl(single(cold.computeCell(r))));
			}
			race(tasks).forEach(v -> assertEquals(125.0, v));
		}
	}

	@AfterEach
	void teardown() {
		pool.shutdownNow();
	}

	/** Starts every task at once (latch) and returns their results. */
	private <T> List<T> race(List<Callable<T>> tasks) throws Exception {
		CountDownLatch start = new CountDownLatch(1);
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
			out.add(f.get(60, java.util.concurrent.TimeUnit.SECONDS));
		}
		return out;
	}

	@Test
	void parallelComputesWithDifferentInputsDoNotSeeEachOthersInputs() throws Exception {
		List<Callable<Double>> tasks = new ArrayList<>();
		List<Double> expected = new ArrayList<>();
		for (int i = 0; i < 100; i++) {
			boolean pep = i % 2 == 0;
			int c = i % COUNTRIES.length;
			int a = (i / COUNTRIES.length) % AMOUNTS.length;
			expected.add((pep ? 50 : 0) + COUNTRY_SCORE[c] + AMOUNT_SCORE[a]);
			ExcelRequest r = kycapi(Boolean.toString(pep), COUNTRIES[c], AMOUNTS[a]).build();
			tasks.add(() -> dbl(single(service.computeCell(r))));
		}
		assertEquals(expected, race(tasks));
	}

	@Test
	void parallelComputesLeaveTheSharedTemplateIntact() throws Exception {
		List<Callable<Double>> tasks = new ArrayList<>();
		for (int i = 0; i < 50; i++) {
			ExcelRequest r = kycapi("true", "KP", "100000000").build();
			tasks.add(() -> dbl(single(service.computeCell(r))));
		}
		race(tasks).forEach(v -> assertEquals(250.0, v));
		assertEquals(0.0, dbl(single(service.computeCell(req("KYCAPI", "ComputeKYC").addOutputs("SCORE").build()))));
	}

	@Test
	void parallelListingsOfTheCachedWorkbookAreConsistent() throws Exception {
		List<Callable<Integer>> tasks = new ArrayList<>();
		for (int i = 0; i < 64; i++) {
			tasks.add(() -> items(service.listOfCell(req("KYCAPI", "ComputeKYC").build())).size());
		}
		race(tasks).forEach(n -> assertEquals(15, n));
	}

	@Test
	void mixedRpcsInParallel() throws Exception {
		List<Callable<Object>> tasks = new ArrayList<>();
		for (int i = 0; i < 40; i++) {
			switch (i % 4) {
			case 0 -> tasks.add(() -> items(service.listOfResource(com.google.protobuf.Empty.getDefaultInstance())).size());
			case 1 -> tasks.add(() -> items(service.listOfSheet(req("Test1", "").build())).size());
			case 2 -> tasks.add(() -> items(service.listOfCell(req("Type", "Sheet1").build())).size());
			default -> tasks.add(() -> dbl(single(service.computeCell(kycapi("true", "CY", "1000000").build()))));
			}
		}
		List<Object> results = race(tasks);
		for (int i = 0; i < results.size(); i++) {
			Object expected = switch (i % 4) {
			case 0 -> 6;
			case 1 -> 3;
			case 2 -> 11;
			default -> 125.0;
			};
			assertEquals(expected, results.get(i), "task " + i);
		}
	}

	@Test
	void parallelGlobalComputes() throws Exception {
		List<Callable<Double>> tasks = new ArrayList<>();
		for (int i = 0; i < 24; i++) {
			int c1 = i;
			tasks.add(() -> items(service.computeCell(req("Primary", "Feuil1").addOutputs("B1")
					.putInputs("C1", Integer.toString(c1)).setGlobal(true).build())).stream().map(Fixtures::dbl)
					.findFirst().orElseThrow());
		}
		List<Double> got = race(tasks);
		for (int i = 0; i < got.size(); i++) {
			assertEquals(42.0 + 2 + i, got.get(i), "C1=" + i);
		}
	}

	@Test
	void failuresInParallelKeepTheirOwnStatus() throws Exception {
		List<Callable<Status.Code>> tasks = new ArrayList<>();
		for (int i = 0; i < 30; i++) {
			ExcelRequest r = switch (i % 3) {
			case 0 -> req("NOPE", "ComputeKYC").addOutputs("SCORE").build();
			case 1 -> kycapi("true", "CY", "abc").build();
			default -> kycapi("true", "CY", "1000000").build();
			};
			tasks.add(() -> {
				try {
					items(service.computeCell(r));
					return Status.Code.OK;
				} catch (StatusRuntimeException e) {
					return e.getStatus().getCode();
				}
			});
		}
		List<Status.Code> got = race(tasks);
		for (int i = 0; i < got.size(); i++) {
			Status.Code expected = switch (i % 3) {
			case 0 -> Status.Code.NOT_FOUND;
			case 1 -> Status.Code.INVALID_ARGUMENT;
			default -> Status.Code.OK;
			};
			assertEquals(expected, got.get(i), "task " + i);
		}
	}

	@Test
	void streamCanBeConsumedOnAnotherThreadWithBackpressure() {
		Multi<ExcelCell> multi = service.listOfCell(req("Type", "Sheet1").build()).emitOn(pool);
		AssertSubscriber<ExcelCell> sub = multi.subscribe().withSubscriber(AssertSubscriber.create(3));
		sub.awaitItems(3, Duration.ofSeconds(10));
		assertEquals(3, sub.getItems().size());
		sub.request(Long.MAX_VALUE);
		sub.awaitCompletion(Duration.ofSeconds(10));
		assertEquals(11, sub.getItems().size());
	}

	@Test
	void failureIsSignalledToASubscriberOnAnotherThread() {
		AssertSubscriber<ExcelCell> sub = service.computeCell(req("NOPE", "X").addOutputs("A1").build()).emitOn(pool)
				.subscribe().withSubscriber(AssertSubscriber.create(1));
		sub.awaitFailure(Duration.ofSeconds(10));
		assertEquals(Status.Code.NOT_FOUND, ((StatusRuntimeException) sub.getFailure()).getStatus().getCode());
	}
}
