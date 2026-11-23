package net.a.g.excel.grpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.poi.ss.usermodel.Workbook;

import com.google.protobuf.BoolValue;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.smallrye.mutiny.Multi;
import net.a.g.excel.engine.ExcelEngineImpl;
import net.a.g.excel.model.ExcelResource;
import net.a.g.excel.param.ExcelParameterImpl;
import net.a.g.excel.repository.ExcelRepositoryImpl;

/** Builds an {@link ExcelService} by hand (no CDI, no server) over the workbooks in ../sample. */
final class Fixtures {

	static final String[] ALL = { "KYC", "KYCAPI", "Primary", "Secondary", "Test1", "Type" };

	private Fixtures() {
	}

	static ExcelResource resource(String name, String file, byte[] doc) {
		ExcelResource r = new ExcelResource();
		r.setName(name);
		r.setFile(file);
		r.setDoc(doc);
		return r;
	}

	static ExcelResource sample(String name) {
		try {
			return resource(name, name + ".xlsx", Files.readAllBytes(Path.of("../sample/" + name + ".xlsx")));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	static ExcelRepositoryImpl repo(String... names) {
		ExcelRepositoryImpl repo = new ExcelRepositoryImpl();
		for (String n : names) {
			repo.add(sample(n));
		}
		return repo;
	}

	static ExcelEngineImpl engine(ExcelRepositoryImpl repo) {
		ExcelEngineImpl engine = new ExcelEngineImpl();
		engine.setRepository(repo);
		engine.setParameter(new ExcelParameterImpl());
		return engine;
	}

	static ExcelService service(ExcelRepositoryImpl repo) {
		ExcelService s = new ExcelService();
		s.repo = repo;
		s.engine = engine(repo);
		return s;
	}

	static ExcelService service(String... names) {
		return service(repo(names));
	}

	static byte[] bytes(Workbook wb) {
		try (wb; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			wb.write(out);
			return out.toByteArray();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	static ExcelRequest.Builder req(String resource, String sheet) {
		return ExcelRequest.newBuilder().setResource(resource).setSheet(sheet);
	}

	static ExcelRequest.Builder kycapi(String pep, String country, String amount) {
		return req("KYCAPI", "ComputeKYC").addOutputs("SCORE").putInputs("PEP", pep).putInputs("COUNTRY", country)
				.putInputs("AMOUNT", amount);
	}

	static <T> List<T> items(Multi<T> multi) {
		return multi.collect().asList().await().indefinitely();
	}

	static ExcelCell single(Multi<ExcelCell> multi) {
		List<ExcelCell> cells = items(multi);
		assertEquals(1, cells.size(), () -> "expected one cell, got " + cells);
		return cells.get(0);
	}

	static StatusRuntimeException failure(Multi<?> multi, Status.Code expected) {
		StatusRuntimeException e = assertThrows(StatusRuntimeException.class, () -> items(multi));
		assertEquals(expected, e.getStatus().getCode(), () -> "description: " + e.getStatus().getDescription());
		return e;
	}

	static <M extends Message> M unpack(ExcelCell cell, Class<M> type) {
		assertTrue(cell.hasValue(), () -> "no value in " + cell);
		assertTrue(cell.getValue().is(type), () -> "expected " + type.getSimpleName() + " in " + cell);
		try {
			return cell.getValue().unpack(type);
		} catch (InvalidProtocolBufferException e) {
			throw new AssertionError(e);
		}
	}

	static double dbl(ExcelCell cell) {
		return unpack(cell, DoubleValue.class).getValue();
	}

	static boolean bool(ExcelCell cell) {
		return unpack(cell, BoolValue.class).getValue();
	}

	static String str(ExcelCell cell) {
		return unpack(cell, StringValue.class).getValue();
	}
}
