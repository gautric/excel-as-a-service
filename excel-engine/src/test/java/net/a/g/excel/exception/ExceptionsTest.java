package net.a.g.excel.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.IOException;

import org.junit.jupiter.api.Test;

public class ExceptionsTest {

	@Test
	void engineExceptionMessage() {
		ExcelEngineException e = new ExcelEngineException("boom");
		assertEquals("boom", e.getMessage());
		assertNull(e.getCause());
		assertInstanceOf(RuntimeException.class, e);
	}

	@Test
	void engineExceptionMessageAndCause() {
		IOException cause = new IOException("io");
		ExcelEngineException e = new ExcelEngineException("boom", cause);
		assertEquals("boom", e.getMessage());
		assertSame(cause, e.getCause());
	}

	@Test
	void engineExceptionCauseOnlyUsesCauseToString() {
		IOException cause = new IOException("io");
		ExcelEngineException e = new ExcelEngineException(cause);
		assertSame(cause, e.getCause());
		assertEquals("java.io.IOException: io", e.getMessage());
	}

	@Test
	void calculationExceptionFormatsMessage() {
		ExcelCalculationException e = new ExcelCalculationException("S!A1", "div by zero");
		assertEquals("Calculation error at S!A1: div by zero", e.getMessage());
		assertEquals("S!A1", e.getCellAddress());
		assertNull(e.getCause());
		assertInstanceOf(ExcelEngineException.class, e);
	}

	@Test
	void calculationExceptionWithCause() {
		RuntimeException cause = new RuntimeException("x");
		ExcelCalculationException e = new ExcelCalculationException("S!B2", "m", cause);
		assertEquals("Calculation error at S!B2: m", e.getMessage());
		assertEquals("S!B2", e.getCellAddress());
		assertSame(cause, e.getCause());
	}

	@Test
	void calculationExceptionNullAddress() {
		ExcelCalculationException e = new ExcelCalculationException(null, "m");
		assertNull(e.getCellAddress());
		assertEquals("Calculation error at null: m", e.getMessage());
	}

	@Test
	void invalidInputThreeArgs() {
		ExcelInvalidInputException e = new ExcelInvalidInputException("AMOUNT", "abc", "Expected a numeric value");
		assertEquals("Invalid input 'AMOUNT' with value 'abc': Expected a numeric value", e.getMessage());
		assertEquals("AMOUNT", e.getInputName());
		assertEquals("abc", e.getInputValue());
		assertInstanceOf(ExcelEngineException.class, e);
	}

	@Test
	void invalidInputMessageOnly() {
		ExcelInvalidInputException e = new ExcelInvalidInputException("bad");
		assertEquals("bad", e.getMessage());
		assertNull(e.getInputName());
		assertNull(e.getInputValue());
	}

	@Test
	void invalidInputNullValue() {
		ExcelInvalidInputException e = new ExcelInvalidInputException("X", null, "m");
		assertEquals("Invalid input 'X' with value 'null': m", e.getMessage());
		assertNull(e.getInputValue());
	}

	@Test
	void resourceNotFoundDefaultMessage() {
		ExcelResourceNotFoundException e = new ExcelResourceNotFoundException("KYC");
		assertEquals("Excel resource not found: KYC", e.getMessage());
		assertEquals("KYC", e.getResourceName());
		assertInstanceOf(ExcelEngineException.class, e);
	}

	@Test
	void resourceNotFoundCustomMessage() {
		ExcelResourceNotFoundException e = new ExcelResourceNotFoundException("KYC", "Sheet not found: X");
		assertEquals("Sheet not found: X", e.getMessage());
		assertEquals("KYC", e.getResourceName());
	}

	@Test
	void resourceNotFoundNullName() {
		ExcelResourceNotFoundException e = new ExcelResourceNotFoundException(null);
		assertNull(e.getResourceName());
		assertEquals("Excel resource not found: null", e.getMessage());
	}
}
