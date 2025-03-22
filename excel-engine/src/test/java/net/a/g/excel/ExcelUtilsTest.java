package net.a.g.excel;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import net.a.g.excel.util.ExcelUtils;

@DisplayName("Excel Utils Tests")
public class ExcelUtilsTest {

    @Nested
    @DisplayName("Column Position Tests")
    class PositionTests {
        
        @Test
        @DisplayName("Single letter column positions")
        public void testSingleLetterPosition() {
            assertEquals(0, ExcelUtils.position("A"));
            assertEquals(1, ExcelUtils.position("B"));
            assertEquals(25, ExcelUtils.position("Z"));
        }
        
        @Test
        @DisplayName("Double letter column positions")
        public void testDoubleLetterPosition() {
            assertEquals(26, ExcelUtils.position("AA"));
            assertEquals(27, ExcelUtils.position("AB"));
            assertEquals(51, ExcelUtils.position("AZ"));
            assertEquals(52, ExcelUtils.position("BA"));
            assertEquals(255, ExcelUtils.position("IV"));
            assertEquals(701, ExcelUtils.position("ZZ"));
            assertEquals(702, ExcelUtils.position("AAA"));

        }
    }
    
    @Nested
    @DisplayName("Cell Position Tests")
    class CellPositionTests {
        
        @Test
        @DisplayName("Valid cell positions")
        public void testValidCellPositions() {
            assertArrayEquals(new int[]{0, 0}, ExcelUtils.getPosition("A1"));
            assertArrayEquals(new int[]{1, 0}, ExcelUtils.getPosition("B1"));
            assertArrayEquals(new int[]{0, 1}, ExcelUtils.getPosition("A2"));
            assertArrayEquals(new int[]{25, 99}, ExcelUtils.getPosition("Z100"));
        }
        
        @Test
        @DisplayName("Invalid cell positions")
        public void testInvalidCellPositions() {
            assertArrayEquals(new int[]{-1, -1}, ExcelUtils.getPosition("1A"));
            assertArrayEquals(new int[]{-1, -1}, ExcelUtils.getPosition("A"));
            assertArrayEquals(new int[]{-1, -1}, ExcelUtils.getPosition("123"));
            assertArrayEquals(new int[]{-1, -1}, ExcelUtils.getPosition(""));
        }
    }
    
    @Nested
    @DisplayName("Cell Address Format Tests")
    class CellAddressTests {
        
        @Test
        @DisplayName("Valid cell addresses")
        public void testValidCellAddresses() {
            assertTrue(ExcelUtils.checkAdress("A1"));
            assertTrue(ExcelUtils.checkAdress("B2"));
            assertTrue(ExcelUtils.checkAdress("Z100"));
            assertTrue(ExcelUtils.checkAdress("AA1"));
            assertTrue(ExcelUtils.checkAdress("AAAAA99999"));
        }
        
        @Test
        @DisplayName("Invalid cell addresses")
        public void testInvalidCellAddresses() {
            assertFalse(ExcelUtils.checkAdress("1A"));
            assertFalse(ExcelUtils.checkAdress("A"));
            assertFalse(ExcelUtils.checkAdress("123"));
            assertFalse(ExcelUtils.checkAdress(""));
            assertFalse(ExcelUtils.checkAdress("A1B"));
        }
    }
    
    @Nested
    @DisplayName("Full Address Format Tests")
    class FullAddressTests {
        
        @Test
        @DisplayName("Valid sheet names without spaces")
        public void testValidSheetNamesWithoutSpaces() {
            assertTrue(ExcelUtils.checkFullAdress("IBM!A1"));
            assertTrue(ExcelUtils.checkFullAdress("RHT!B2"));
            assertTrue(ExcelUtils.checkFullAdress("Sheet1!Z100"));
        }
        
        @Test
        @DisplayName("Valid sheet names with spaces")
        public void testValidSheetNamesWithSpaces() {
            assertTrue(ExcelUtils.checkFullAdress("'RedHat Inc'!B2"));
            assertTrue(ExcelUtils.checkFullAdress("'RedHatInc'!B2"));
            assertTrue(ExcelUtils.checkFullAdress("'Red Hat Inc'!B2"));
        }
        
        @Test
        @DisplayName("Invalid sheet names")
        public void testInvalidSheetNames() {
            assertFalse(ExcelUtils.checkFullAdress("'RedHat Inc!B2"));  // Missing closing quote
            assertFalse(ExcelUtils.checkFullAdress("RedHat Inc'!B2"));  // Missing opening quote
            assertFalse(ExcelUtils.checkFullAdress("RedHat'Inc'!B2"));  // Quote in the middle
            assertFalse(ExcelUtils.checkFullAdress("Red Ha t'I n c'!B2"));  // Multiple quotes
            assertFalse(ExcelUtils.checkFullAdress("Sheet1!"));  // Missing cell reference
            assertFalse(ExcelUtils.checkFullAdress("!A1"));  // Missing sheet name
        }
    }
    
    @Nested
    @DisplayName("Strict Full Address Tests")
    class StrictFullAddressTests {
        
        @Test
        @DisplayName("Valid comma-separated addresses")
        public void testValidCommaAddresses() {
            assertTrue(ExcelUtils.checkFullAdressStrict("IBM!A1"));
            assertTrue(ExcelUtils.checkFullAdressStrict("'RedHat Inc'!B2,IBM!A1"));
            assertTrue(ExcelUtils.checkFullAdressStrict("'RedHat Inc'!B2,IBM!A1,SUN!A2,DIGITAL!A2"));
        }
        
        @Test
        @DisplayName("Invalid comma-separated addresses")
        public void testInvalidCommaAddresses() {
          //  assertFalse(ExcelUtils.checkFullAdressStrict("'RedHat Inc'!B2,Invalid!A1"));
            assertFalse(ExcelUtils.checkFullAdressStrict("'RedHat Inc'!B2,IBM!A1,!A2"));
            assertFalse(ExcelUtils.checkFullAdressStrict("'RedHat Inc!B2,IBM!A1"));
        }
    }
    
    @Nested
    @DisplayName("Filter Valid Address Tests")
    class FilterValidAddressTests {
        
        @Test
        @DisplayName("Filter valid addresses")
        public void testFilterValidAddresses() {
            List<String> expected1 = Arrays.asList("IBM!A1", "SUN!A2");
            List<String> actual1 = ExcelUtils.filterValidAdress("IBM!A1,Invalid,SUN!A2");
            assertEquals(expected1, actual1);
            
            List<String> expected2 = Arrays.asList("'RedHat Inc'!B2", "IBM!A1");
            List<String> actual2 = ExcelUtils.filterValidAdress("'RedHat Inc'!B2,IBM!A1,!A2");
            assertEquals(expected2, actual2);
            
            List<String> expected3 = Arrays.asList();
            List<String> actual3 = ExcelUtils.filterValidAdress("Invalid1,Invalid2");
            assertEquals(expected3, actual3);
        }
    }
}
