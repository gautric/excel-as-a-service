package net.a.g.excel.grpc;

import static net.a.g.excel.grpc.Fixtures.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.poi.ss.usermodel.Cell;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.grpc.Status;
import io.smallrye.mutiny.Multi;
import net.a.g.excel.engine.ExcelEngineImpl;
import net.a.g.excel.exception.ExcelCalculationException;
import net.a.g.excel.exception.ExcelEngineException;
import net.a.g.excel.exception.ExcelInvalidInputException;
import net.a.g.excel.exception.ExcelResourceNotFoundException;
import net.a.g.excel.model.ExcelResult;

/** Every RPC maps the same engine exception to the same gRPC status. */
class StatusMappingTest {

	/** An engine whose every entry point throws the given exception. */
	private static ExcelService throwing(RuntimeException ex) {
		ExcelService s = service("KYCAPI");
		s.engine = new ExcelEngineImpl() {
			@Override
			public ExcelResult compute(net.a.g.excel.model.ExcelRequest request) {
				throw ex;
			}

			@Override
			public List<net.a.g.excel.model.ExcelSheet> listOfSheet(String name) {
				throw ex;
			}

			@Override
			public boolean isSheetExists(String resource, String sheetName) {
				return true;
			}

			@Override
			public List<net.a.g.excel.model.ExcelCell> listOfCell(String r, String s, Predicate<Cell> p) {
				throw ex;
			}
		};
		return s;
	}

	static Stream<Arguments> mapping() {
		return Stream.of( //
				Arguments.of(new ExcelResourceNotFoundException("R", "gone"), Status.Code.NOT_FOUND, "gone"),
				Arguments.of(new ExcelInvalidInputException("bad input"), Status.Code.INVALID_ARGUMENT, "bad input"),
				Arguments.of(new IllegalArgumentException("illegal"), Status.Code.INVALID_ARGUMENT, "illegal"),
				Arguments.of(new ExcelCalculationException("A1", "boom"), Status.Code.INTERNAL, "Internal error"),
				Arguments.of(new ExcelEngineException("engine secret"), Status.Code.INTERNAL, "Internal error"),
				Arguments.of(new IllegalStateException("closed"), Status.Code.INTERNAL, "Internal error"));
	}

	private static void check(Multi<?> multi, Status.Code code, String description) {
		assertEquals(description, failure(multi, code).getStatus().getDescription());
	}

	@ParameterizedTest(name = "listOfSheet {0} -> {1}")
	@MethodSource("mapping")
	void listOfSheet(RuntimeException ex, Status.Code code, String description) {
		check(throwing(ex).listOfSheet(req("KYCAPI", "").build()), code, description);
	}

	@ParameterizedTest(name = "listOfCell {0} -> {1}")
	@MethodSource("mapping")
	void listOfCell(RuntimeException ex, Status.Code code, String description) {
		check(throwing(ex).listOfCell(req("KYCAPI", "ComputeKYC").build()), code, description);
	}

	@ParameterizedTest(name = "computeCell {0} -> {1}")
	@MethodSource("mapping")
	void computeCell(RuntimeException ex, Status.Code code, String description) {
		check(throwing(ex).computeCell(req("KYCAPI", "ComputeKYC").addOutputs("SCORE").build()), code, description);
	}
}
