package net.a.g.excel;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.smallrye.common.constraint.Assert;
import net.a.g.excel.util.ExcelUtils;

public class ExcelUtilsTest {

	@Test
	public void testAddressIBM() {
		assertTrue(ExcelUtils.checkFullAdress("IBM!A1"));
	}

	@Test
	public void testAddressRHT() {
		assertTrue(ExcelUtils.checkFullAdress("RHT!B2"));
	}

	@Test
	public void testAddressRedHat_Inc() {
		assertTrue(ExcelUtils.checkFullAdress("'RedHat Inc'!B2"));
	}

	@Test
	public void testAddressRedHat_Inc_IBM() {
		assertTrue(ExcelUtils.checkFullAdressStrict("'RedHat Inc'!B2,IBM!A1"));
	}
	
	@Test
	public void testAddressRedHat_Inc_IBM_Sun() {
		assertTrue(ExcelUtils.checkFullAdressStrict("'RedHat Inc'!B2,IBM!A1,SUN!A2,DIGITAL!A2"));
	}

	
}