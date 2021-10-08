/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.a.g.excel;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.apache.poi.hssf.usermodel.HSSFFormulaEvaluator;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFFormulaEvaluator;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.a.g.excel.util.ExcelConfiguration;
import net.a.g.excel.util.ExcelConstants;
import net.a.g.excel.util.ExcelUtils;

@RequestScoped
public class ExcelEngine {

	public final static Logger LOG = LoggerFactory.getLogger(ExcelEngine.class);

	@Inject
	ExcelConfiguration conf;

	private Map<String, byte[]> map = new HashMap<String, byte[]>();

	public boolean addFile(String s, InputStream is) {
		try {
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			int nRead;
			byte[] data = new byte[16384];
			while ((nRead = is.read(data, 0, data.length)) != -1) {
				buffer.write(data, 0, nRead);
			}
			buffer.flush();

			data = buffer.toByteArray();

			if (extractedWorkbook("", data) == null) {
				return false;
			}
			map.put(s, data);
		} catch (IOException ex) {
			LOG.error("", ex);
		}
		return true;

	}

	public Set<String> listOfFile() {
		return map.keySet();
	}

	public boolean title(String name) {
		return map.containsKey(name);
	}

	public Workbook retrieveWorkbook(String name) {
		byte[] byteArray = map.get(name);

		Workbook workbook = extractedWorkbook(name, byteArray);

		return workbook;
	}

	private Workbook extractedWorkbook(String name, byte[] byteArray) {
		Workbook workbook = null;
		ByteArrayInputStream is = new ByteArrayInputStream(byteArray);
		try {
			workbook = new XSSFWorkbook(is);
			return workbook;
		} catch (Exception ex) {
			LOG.error("Workbook '{}' is not a XSSF file", name, ex);
		}
		try {
			workbook = new HSSFWorkbook(is);
		} catch (Exception ex) {
			LOG.error("Workbook '{}' is not a HSSF file", name, ex);
		}
		return workbook;
	}

	public FormulaEvaluator formula(Workbook wb) {

		if (wb instanceof HSSFWorkbook) {
			return new HSSFFormulaEvaluator((HSSFWorkbook) wb);
		}

		return new XSSFFormulaEvaluator((XSSFWorkbook) wb);
	}

	public List<String> listOfSheet(String name) {

		Workbook workbook = retrieveWorkbook(name);

		int numberOfSheet = workbook.getNumberOfSheets();

		List<String> list = new ArrayList<String>(numberOfSheet);

		for (int i = 0; i < numberOfSheet; i++) {
			list.add(workbook.getSheetAt(i).getSheetName());
		}

		return list;
	}

	public boolean sheet(String excelName, String sheetName) {
		Workbook workbook = retrieveWorkbook(excelName);
		Sheet sheet = workbook.getSheet(sheetName);
		return sheet != null;
	}

	public Map<String, String> cellFormular(String excelName, String sheetName) {
		Workbook workbook = retrieveWorkbook(excelName);
		Sheet sheet = workbook.getSheet(sheetName);
		Map<String, String> map = new HashMap<String, String>();

		sheet.forEach(row -> {
			row.forEach(cell -> {
				if (cell.getCellType() == CellType.FORMULA) {
					map.put(cell.getAddress().formatAsString(), cell.getCellFormula().toString());
				}
			});
		});

		return map;
	}

	public Map<String, Object> computeCell(String excelName, String sheetName, String[] cellNames,
			Map<String, List<String>> names) {

		Workbook workbook = retrieveWorkbook(excelName);
		
		//Inject Value to the workbook
		names.forEach((address, value) -> injectValue(address, value, workbook, sheetName));

		FormulaEvaluator exec = formula(workbook);

		exec.setDebugEvaluationOutputForNextEval(true);

		
		Map<String, Object> map = new HashMap<String, Object>();

		for (String cellName : cellNames) {

			Cell cell = retrieveCellByAdress(cellName, workbook, sheetName);

			String fullCellName = extracted(cell, sheetName);

			map.put(fullCellName, getRawCell(exec.evaluateInCell(cell)));
		}
		
		LOG.info(map.toString());

		return map;
	}

	private String extracted(Cell cell, String defaultSheetName) {
		return (defaultSheetName.compareTo(cell.getSheet().getSheetName()) != 0)
				? cell.getSheet().getSheetName() + "!"+cell.getAddress().formatAsString()
				: cell.getAddress().formatAsString();
	}

	private void injectValue(String cellAdress, List<String> value, Workbook workbook, String defaultSheetName) {
		updateCell(retrieveCellByAdress(cellAdress, workbook, defaultSheetName), value.get(0));
	}

	private Cell retrieveCellByAdress(String cellAddress, Workbook workbook, String defaultSheetName) {
		CellReference cr = new CellReference(cellAddress);
		String sheetOfCell = (cr.getSheetName() == null) ? defaultSheetName : cr.getSheetName();
		Sheet mySheet = workbook.getSheet(sheetOfCell);
		Row row = mySheet.getRow(cr.getRow());
		Cell cell = row.getCell(cr.getCol());
		return cell;
	}

	public Object getRawCell(Cell cell) {

		Object ret = "";
		if (cell != null) {

			switch (cell.getCellType()) {
			case BOOLEAN:
				ret = Boolean.toString(cell.getBooleanCellValue());
				break;
			case NUMERIC:

				if (DateUtil.isCellDateFormatted(cell)) {
					SimpleDateFormat sdf = new SimpleDateFormat(ExcelConstants.DATE_FORMAT);
					ret = sdf.format(cell.getDateCellValue());
				} else {
					ret = cell.getNumericCellValue();
				}

				break;
			case STRING:
				ret = "" + cell.getStringCellValue();
				break;
			case BLANK:
				ret = "" + cell.getStringCellValue();
				break;
			case ERROR:
				break;
			// CELL_TYPE_FORMULA will never happen
			case FORMULA:
				break;
			default:
				break;
			}
		}
		return ret;
	}

	public void updateCell(Cell cell, String value) {

		if (cell != null) {

			switch (cell.getCellType()) {
			case BOOLEAN:
				cell.setCellValue(Boolean.getBoolean(value));
				break;
			case NUMERIC:

				if (DateUtil.isCellDateFormatted(cell)) {
					SimpleDateFormat sdf = new SimpleDateFormat(ExcelConstants.DATE_FORMAT);
					try {
						cell.setCellValue(sdf.parse(value));
					} catch (ParseException ex) {
						LOG.error("", ex);

					}
				} else {
					cell.setCellValue(Double.parseDouble(value));
				}

				break;
			case STRING:
				cell.setCellValue(value);
				break;
			case BLANK:
				cell.setCellValue(value);
				break;
			case ERROR:
				break;
			case FORMULA:
				LOG.warn("Cell at '{}' is a '{}'", cell.getAddress(), CellType.FORMULA);
				break;
			default:
				break;
			}
		}
	}

	/**
	 * Return Cell from Excel position
	 * 
	 * D1 => Cell
	 * 
	 * @param a
	 * @return
	 */
	public Cell getCell(Sheet sheet, String excelPosition) {

		Cell cell = null;

		int[] pos = ExcelUtils.getPosition(excelPosition);

		Row row = sheet.getRow(pos[1]); // 0-based.

		cell = row.getCell(pos[0]); // Also 0-based;

		return cell;
	}

	@PostConstruct
	public void loadFile() throws Exception {

		InputStream inputStream = ExcelEngine.class.getResourceAsStream(conf.getResouceUri());

		if (inputStream != null) {
			LOG.info("Load file from classpth://{}", conf.getResouceUri());
			addFile(conf.getName(), inputStream);
		} else {
			Path file = Paths.get(conf.getResouceUri());
			if (Files.isRegularFile(file)) {
				addFile(file);
			} else {

				Files.walk(file, 1).filter(f -> !f.getFileName().toString().startsWith("~")
						&& (f.getFileName().toString().endsWith("xls") || f.getFileName().toString().endsWith("xlsx")))
						.forEach(f -> addFile(f));
			}
		}
	}

	private void addFile(Path file) {
		try {
			LOG.info("Load file from {}", file.getFileName());
			addFile(file.getFileName().toString().split("\\.")[0], file.toUri().toURL().openStream());
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
