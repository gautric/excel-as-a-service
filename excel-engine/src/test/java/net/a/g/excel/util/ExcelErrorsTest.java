package net.a.g.excel.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import net.a.g.excel.exception.ExcelCalculationException;
import net.a.g.excel.exception.ExcelEngineException;
import net.a.g.excel.exception.ExcelInvalidInputException;
import net.a.g.excel.exception.ExcelResourceNotFoundException;
import net.a.g.excel.model.ExcelResult;

public class ExcelErrorsTest {

	static Stream<Arguments> codes() {
		return Stream.of(
				Arguments.of(new ExcelResourceNotFoundException("R"), "404"),
				Arguments.of(new ExcelInvalidInputException("bad"), "400"),
				Arguments.of(new IllegalArgumentException("x"), "400"),
				Arguments.of(new ExcelCalculationException("A1", "m"), "500"),
				Arguments.of(new ExcelEngineException("m"), "500"),
				Arguments.of(new IllegalStateException("closed"), "500"),
				Arguments.of(new Error("fatal"), "500"));
	}

	@ParameterizedTest
	@MethodSource("codes")
	void codeMapsExceptionType(Throwable t, String expected) {
		assertEquals(expected, ExcelErrors.code(t));
	}

	@Test
	void codeOfNullIs500() {
		assertEquals("500", ExcelErrors.code(null));
	}

	@Test
	void toResultClientErrorKeepsMessage() {
		ExcelResult r = ExcelErrors.toResult(new ExcelInvalidInputException("At least one output is required"), "u1");
		assertEquals("u1", r.getUuid());
		assertEquals("400", r.getError().getCode());
		assertEquals("At least one output is required", r.getError().getError());
		assertNull(r.getCells());
		assertEquals(0, r.getCount());
	}

	@Test
	void toResultNotFoundKeepsMessage() {
		ExcelResult r = ExcelErrors.toResult(new ExcelResourceNotFoundException("NOPE"), "u2");
		assertEquals("404", r.getError().getCode());
		assertEquals("Excel resource not found: NOPE", r.getError().getError());
	}

	@Test
	void toResultServerErrorHidesMessage() {
		ExcelResult r = ExcelErrors.toResult(new ExcelEngineException("secret path /etc/x"), "u3");
		assertEquals("500", r.getError().getCode());
		assertEquals("Internal error", r.getError().getError());
	}

	@Test
	void toResultAcceptsNullUuid() {
		ExcelResult r = ExcelErrors.toResult(new IllegalArgumentException("x"), null);
		assertNull(r.getUuid());
		assertNotNull(r.getError());
	}

	@Test
	void toResultWithNullThrowableIsInternalError() {
		ExcelResult r = ExcelErrors.toResult(null, "u");
		assertEquals("500", r.getError().getCode());
		assertEquals("Internal error", r.getError().getError());
	}

	@Test
	void constructorIsPrivate() throws Exception {
		Constructor<ExcelErrors> c = ExcelErrors.class.getDeclaredConstructor();
		assertEquals(true, Modifier.isPrivate(c.getModifiers()));
		c.setAccessible(true);
		assertNotNull(c.newInstance());
	}
}
