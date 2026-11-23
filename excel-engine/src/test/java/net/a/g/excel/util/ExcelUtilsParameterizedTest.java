package net.a.g.excel.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Complements ExcelUtilsTest with boundary and invalid-input cases. */
public class ExcelUtilsParameterizedTest {

	@ParameterizedTest
	@CsvSource({ "XFD,16383", "AAAA,18278" })
	void positionOfColumn(String col, int expected) {
		assertEquals(expected, ExcelUtils.position(col));
	}

	@Test
	void positionIsCaseInsensitive() {
		assertEquals(ExcelUtils.position("AB"), ExcelUtils.position("ab"));
	}

	@ParameterizedTest
	@CsvSource({ "C6,2,5", "XFD10485,16383,10484" })
	void getPositionOfValidAddress(String address, int col, int row) {
		assertArrayEquals(new int[] { col, row }, ExcelUtils.getPosition(address));
	}

	@ParameterizedTest
	@ValueSource(strings = { "a1", "AAAAAA1", "A123456", "Sheet!A1" })
	void getPositionOfInvalidAddress(String address) {
		assertArrayEquals(new int[] { -1, -1 }, ExcelUtils.getPosition(address));
	}

	@Test
	void getPositionOfRowZeroGivesMinusOne() {
		assertArrayEquals(new int[] { 0, -1 }, ExcelUtils.getPosition("A0"));
	}

	@Test
	void nullAddressThrows() {
		assertThrows(NullPointerException.class, () -> ExcelUtils.getPosition(null));
		assertThrows(NullPointerException.class, () -> ExcelUtils.checkAdress(null));
		assertThrows(NullPointerException.class, () -> ExcelUtils.filterValidAdress(null));
	}

	@ParameterizedTest
	@ValueSource(strings = { "$A$1", "Sheet1!A1" })
	void checkAdressRejects(String address) {
		assertFalse(ExcelUtils.checkAdress(address));
	}

	@ParameterizedTest
	@ValueSource(strings = { "My Sheet!A1", "S!a1", "S!A1,T!B2" })
	void checkFullAdressRejects(String address) {
		assertFalse(ExcelUtils.checkFullAdress(address));
	}

	@Test
	void checkFullAdressStrictEdgeCases() {
		assertFalse(ExcelUtils.checkFullAdressStrict(""));
		assertFalse(ExcelUtils.checkFullAdressStrict("S!A1, T!B2"));
	}

	@Test
	void filterValidAdressKeepsOrderAndHandlesEmpty() {
		assertEquals(List.of("B!B2", "A!A1"), ExcelUtils.filterValidAdress("B!B2,junk,A!A1"));
		assertTrue(ExcelUtils.filterValidAdress("").isEmpty());
	}
}
