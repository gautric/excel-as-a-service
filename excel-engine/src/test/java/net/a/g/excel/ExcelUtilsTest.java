package net.a.g.excel;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.a.g.excel.util.ExcelUtils;

public class ExcelUtilsTest {

	@Test
	public void test_ok_address_w_IBM() {
		assertTrue(ExcelUtils.checkFullAdress("IBM!A1"));
	}

	@Test
	public void test_ok_address_w_RHT() {
		assertTrue(ExcelUtils.checkFullAdress("RHT!B2"));
	}

	@Test
	public void test_ok_redhat_w_space() {
		assertTrue(ExcelUtils.checkFullAdress("'RedHat Inc'!B2"));
	}
	
	@Test
	public void test_ko_checkfulladress() {
		assertFalse(ExcelUtils.checkFullAdress("'RedHat Inc!B2"));
	}
	
	@Test
	public void test_ok_checkfulladress_v2() {
		assertTrue(ExcelUtils.checkFullAdress("'RedHatInc'!B2"));
	}
	
	
	@Test
	public void test_ok_checkfulladress_v3() {
		assertTrue(ExcelUtils.checkFullAdress("'Red Hat Inc'!B2"));
	}
	
	@Test
	public void test_ko_checkfulladress_end() {
		assertFalse(ExcelUtils.checkFullAdress("RedHat Inc'!B2"));
	}
	
	@Test
	public void test_ko_checkfulladress_middle() {
		assertFalse(ExcelUtils.checkFullAdress("RedHat'Inc'!B2"));
	}
	
	
	@Test
	public void test_ko_checkfulladress_middle_v2() {
		assertFalse(ExcelUtils.checkFullAdress("Red Ha t'I n c'!B2"));
	}

	@Test
	public void test_ok_redhat_w_space_and_ibm() {
		assertTrue(ExcelUtils.checkFullAdressStrict("'RedHat Inc'!B2,IBM!A1"));
	}
	
	@Test
	public void test_ok_redhat_w_space_and_ibm_and_digital() {
		assertTrue(ExcelUtils.checkFullAdressStrict("'RedHat Inc'!B2,IBM!A1,SUN!A2,DIGITAL!A2"));
	}

	
}