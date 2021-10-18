package net.a.g.excel.engine;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Row.MissingCellPolicy;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.a.g.excel.model.ExcelCell;
import net.a.g.excel.model.ExcelResource;
import net.a.g.excel.util.ExcelConfiguration;
import net.a.g.excel.util.ExcelUtils;

@ApplicationScoped
@Named
public class ExcelEngine {

	public final static Logger LOG = LoggerFactory.getLogger(ExcelEngine.class);

	@Inject
	ExcelConfiguration conf;

	private Map<String, ExcelResource> listOfResources = new HashMap<String, ExcelResource>();

	public boolean addResource(ExcelResource resource) {

		assert (resource != null);

		if (extractedWorkbook(resource.getName(), resource.getDoc()) == null) {
			LOG.error("Workbook {} is not readable", resource.getName());
			return false;
		}
		listOfResources.put(resource.getName(), resource);

		LOG.info("Add {} OK", resource);

		return true;
	}

	public void clearAllResource() {
		listOfResources.clear();
	}

	public int countListOfResource() {
		return listOfResources.keySet().size();
	}

	public Set<String> lisfOfResourceName() {
		return listOfResources.keySet();
	}

	public boolean isResourceExists(String name) {
		return listOfResources.containsKey(name);
	}

	private Workbook retrieveWorkbook(String name) {
		Workbook workbook = null;
		if (listOfResources.containsKey(name)) {
			byte[] byteArray = listOfResources.get(name).getDoc();
			workbook = convertByteToWorkbook(byteArray);
		}
		return workbook;
	}

	private Workbook convertByteToWorkbook(byte[] byteArray) {
		try {
			return WorkbookFactory.create(new ByteArrayInputStream(byteArray));
		} catch (EncryptedDocumentException ex) {
			LOG.error("Workbook  is not a XSSF file", ex);
		} catch (IOException ex) {
			LOG.error("Workbook  is not a XSSF file", ex);
		}
		return null;
	}

	private Workbook extractedWorkbook(String name, byte[] byteArray) {
		Workbook workbook = null;

		try {
			workbook = WorkbookFactory.create(new ByteArrayInputStream(byteArray));
			return workbook;
		} catch (Exception ex) {
			LOG.error("Workbook '" + name + "' is not a XSSF file", ex);
		}

		return workbook;
	}

	private FormulaEvaluator formula(Workbook wb) {
		return wb.getCreationHelper().createFormulaEvaluator();
	}

	public List<String> listOfSheet(String name) {
		List<String> list = null;

		if (listOfResources.containsKey(name)) {

			Workbook workbook = retrieveWorkbook(name);

			int numberOfSheet = workbook.getNumberOfSheets();

			list = new ArrayList<String>(numberOfSheet);

			for (int i = 0; i < numberOfSheet; i++) {
				list.add(workbook.getSheetAt(i).getSheetName());
			}
		}
		return list;
	}

	public boolean isSheetExists(String excelName, String sheetName) {
		Workbook workbook = retrieveWorkbook(excelName);
		Sheet sheet = workbook.getSheet(sheetName);
		return sheet != null;
	}

	private Sheet sheet(String excelName, String sheetName) {
		Workbook workbook = retrieveWorkbook(excelName);
		return workbook.getSheet(sheetName);
	}

	public Map<String, ExcelCell> mapOfFormularCell(String excelName, String sheetName) {
		return retrieveCell(excelName, sheetName, "FORMULA");
	}

	private Map<String, ExcelCell> retrieveCell(String excelName, String sheetName, String cellType) {
		return mapOfCell(excelName, sheetName, cell -> CellType.valueOf(cellType) == cell.getCellType());
	}

	public Map<String, ExcelCell> mapOfCell(String excelName, String sheetName, Predicate<Cell> predicate) {
		Workbook workbook = retrieveWorkbook(excelName);
		Sheet sheet = workbook.getSheet(sheetName);

		return streamCell(sheet).filter(predicate)
				.collect(Collectors.toMap(cell -> cell.getAddress().formatAsString(), this::celltoExcelCell));
	}
	
	
	public List<ExcelCell> listOfCell(String excelName, String sheetName, Predicate<Cell> predicate) {
		Workbook workbook = retrieveWorkbook(excelName);
		Sheet sheet = workbook.getSheet(sheetName);

		return streamCell(sheet).filter(predicate).map(this::celltoExcelCell)
				.collect(Collectors.toList());
	}

	public Map<String, ExcelCell> mapOfCellCalculated(String excelName, String sheetName, String[] cellNames,
			Map<String, List<String>> names, boolean global) {

		// Compute All cellNames
		return cellCalculation(excelName, sheetName, cellNames, names, global).stream()
				.collect(Collectors.toMap(ExcelCell::getAddress, Function.identity()));

	}

	public List<ExcelCell> cellCalculation(String excelName, String sheetName, String[] cellNames,
			Map<String, List<String>> names, boolean global) {

		Workbook workbook = retrieveWorkbook(excelName);

		FormulaEvaluator exec = formula(workbook);

		if (global) {

			Map<String, FormulaEvaluator> workbooks = listOfResources.values().stream()
					.filter(r -> excelName.compareTo(r.getName()) != 0)
					.collect(Collectors.toMap(ExcelResource::getFile, r -> formula(convertByteToWorkbook(r.getDoc()))));

			workbooks.put("primary", exec);

			exec.setupReferencedWorkbooks(workbooks);
		}

		if (names != null) {
			// Inject Value to the workbook
			names.forEach((address, value) -> injectValue(address, value, workbook, sheetName));
		}

		// Compute All cellNames
		return Arrays.stream(cellNames).map(cn -> cn.contains("!") ? cn : sheetName + "!" + cn).map(CellReference::new)
				.map(cr -> retrieveCellByAdress(cr, workbook)).flatMap(Stream::ofNullable)
				.map(cell -> computeCell(cell, exec)).collect(Collectors.toList());

	}

	private String retrieveFullCellName(Cell cell, String defaultSheetName) {

		return (defaultSheetName.compareTo(cell.getSheet().getSheetName()) != 0)
				? cell.getSheet().getSheetName() + "!" + cell.getAddress().formatAsString()
				: cell.getAddress().formatAsString();
	}

	private void injectValue(String cellAdress, List<String> value, Workbook workbook, String defaultSheetName) {
		updateCell(retrieveCellByAdress(cellAdress, workbook, defaultSheetName), value.get(0));
	}

	private Cell retrieveCellByAdress(CellReference cr, Workbook workbook) {
		Cell ret = null;
		Sheet sheet = workbook.getSheet(cr.getSheetName());
		Row row = sheet.getRow(cr.getRow());
		if (row != null) {
			ret = row.getCell(cr.getCol(), MissingCellPolicy.CREATE_NULL_AS_BLANK);
		}
		return ret;
	}

	private Cell retrieveCellByAdress(String cellAddress, Workbook workbook, String defaultSheetName) {
		Cell ret = null;
		CellReference cr = new CellReference(cellAddress);
		String sheetOfCell = (cr.getSheetName() == null) ? defaultSheetName : cr.getSheetName();
		Sheet sheet = workbook.getSheet(sheetOfCell);
		Row row = sheet.getRow(cr.getRow());
		if (row != null) {
			ret = row.getCell(cr.getCol(), MissingCellPolicy.CREATE_NULL_AS_BLANK);
		}
		return ret;
	}

	private Object getRawCell(Cell cell) {

		Object ret = "";
		if (cell != null) {

			switch (cell.getCellType()) {
			case BOOLEAN:
				ret = Boolean.toString(cell.getBooleanCellValue());
				break;
			case NUMERIC:

				if (DateUtil.isCellDateFormatted(cell)) {
					SimpleDateFormat sdf = new SimpleDateFormat(conf.getFormatDate());
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
			case FORMULA:
				ret = cell.getCellFormula();
				break;
			default:
				break;
			}
		}
		return ret;
	}

	private String getCellType(Cell cell) {

		String ret = "";
		if (cell != null) {

			CellType type = cell.getCellType();
			switch (cell.getCellType()) {

			case NUMERIC:

				if (DateUtil.isCellDateFormatted(cell)) {
					return "DATE";
				} else {
					return type.name();
				}

			case BOOLEAN:
			case STRING:
			case BLANK:
			case ERROR:
			case FORMULA:
				return type.name();
			default:
				break;
			}
		}
		return ret;
	}

	private void updateCell(Cell cell, String value) {

		if (cell != null) {

			switch (cell.getCellType()) {
			case BOOLEAN:
				cell.setCellValue(Boolean.parseBoolean(value));
				break;
			case NUMERIC:

				if (DateUtil.isCellDateFormatted(cell)) {
					SimpleDateFormat sdf = new SimpleDateFormat(conf.getFormatDate());
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
	private Cell getCell(Sheet sheet, String excelPosition) {

		Cell cell = null;

		int[] pos = ExcelUtils.getPosition(excelPosition);

		Row row = sheet.getRow(pos[1]); // 0-based.

		cell = row.getCell(pos[0]); // Also 0-based;

		return cell;
	}

	private Stream<Cell> streamCell(Workbook workbook) {
		return StreamSupport.stream(workbook.spliterator(), false).flatMap(sheet -> StreamSupport
				.stream(sheet.spliterator(), false).flatMap(r -> StreamSupport.stream(r.spliterator(), false)));
	}

	private Stream<Cell> streamCell(Sheet sheet) {
		return StreamSupport.stream(sheet.spliterator(), false)
				.flatMap(r -> StreamSupport.stream(r.spliterator(), false));
	}

	private String getCellComment(Cell cell) {

		String ret = null;
		if (cell.getCellComment() != null) {
			ret = cell.getCellComment().getString().toString().replaceAll("\\n", "");
		}
		return ret;
	}

	private ExcelCell computeCell(Cell cell, FormulaEvaluator exec) {
		return celltoExcelCell(cell, c -> exec.evaluateInCell(c));
	}

	private ExcelCell celltoExcelCell(Cell cell) {
		return celltoExcelCell(cell, Function.identity());
	}

	private ExcelCell celltoExcelCell(Cell cell, Function<Cell, Cell> valueFunction) {
		ExcelCell ret = new ExcelCell();

		ret.setAddress(cell.getSheet().getSheetName() + "!" + cell.getAddress().formatAsString());
		ret.setValue(getRawCell(valueFunction.apply(cell)));
		ret.setType(getCellType(cell));
		ret.setMetadata(getCellComment(cell));
		return ret;
	}
}
