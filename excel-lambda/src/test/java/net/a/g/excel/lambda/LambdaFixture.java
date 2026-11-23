package net.a.g.excel.lambda;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.a.g.excel.engine.ExcelEngineImpl;
import net.a.g.excel.model.ExcelCell;
import net.a.g.excel.model.ExcelRequest;
import net.a.g.excel.model.ExcelResource;
import net.a.g.excel.model.ExcelResult;
import net.a.g.excel.param.ExcelParameterImpl;
import net.a.g.excel.repository.ExcelRepositoryImpl;

/**
 * Builds an {@link ExcelLambdaHandler} by hand (no Lambda runtime, no Docker)
 * over a repository loaded from {@code ../sample}.
 */
final class LambdaFixture {

	static final Path SAMPLES = Path.of("../sample");

	private LambdaFixture() {
	}

	/** A handler whose repository holds the named sample workbooks (name = file stem). */
	static ExcelLambdaHandler handler(String... samples) {
		ExcelRepositoryImpl repo = new ExcelRepositoryImpl();
		for (String name : samples) {
			ExcelResource r = new ExcelResource();
			r.setName(name);
			r.setFile(name + ".xlsx");
			try {
				r.setDoc(Files.readAllBytes(SAMPLES.resolve(name + ".xlsx")));
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			repo.add(r);
		}
		ExcelEngineImpl engine = new ExcelEngineImpl();
		engine.setRepository(repo);
		engine.setParameter(new ExcelParameterImpl());
		ExcelLambdaHandler h = new ExcelLambdaHandler();
		h.engine = engine;
		return h;
	}

	static ExcelRequest request(String uuid, String resource, String sheet, List<String> outputs,
			Map<String, String> inputs) {
		ExcelRequest req = new ExcelRequest(uuid);
		req.setResource(resource);
		req.setSheet(sheet);
		req.setOutputs(outputs == null ? null : new ArrayList<>(outputs));
		req.setInputs(inputs == null ? null : new LinkedHashMap<>(inputs));
		return req;
	}

	/** Alternating key/value pairs to an ordered map. */
	static Map<String, String> inputs(String... kv) {
		if (kv.length % 2 != 0) {
			throw new IllegalArgumentException("key/value pairs expected: " + Arrays.toString(kv));
		}
		Map<String, String> m = new LinkedHashMap<>();
		for (int i = 0; i < kv.length; i += 2) {
			m.put(kv[i], kv[i + 1]);
		}
		return m;
	}

	static List<ExcelCell> cells(ExcelResult res) {
		return res.getCells() == null ? List.of() : new ArrayList<>(res.getCells());
	}

	static Object value(ExcelResult res, String address) {
		return cells(res).stream()
				.filter(c -> address.equals(c.getAddress()))
				.findFirst()
				.orElseThrow(() -> new AssertionError("no cell " + address + " in " + cells(res)))
				.getValue();
	}

	static Object single(ExcelResult res) {
		List<ExcelCell> cells = cells(res);
		if (cells.size() != 1) {
			throw new AssertionError("expected one cell, got " + cells + " error="
					+ (res.getError() == null ? null : res.getError().getError()));
		}
		return cells.get(0).getValue();
	}
}
