package net.a.g.excel.engine;

import java.io.ByteArrayInputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.poi.hssf.usermodel.HSSFFormulaEvaluator;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
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
import org.apache.poi.xssf.usermodel.XSSFFormulaEvaluator;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
			workbook = extractedWorkbook(name, byteArray);
		}
		return workbook;
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

	public Map<String, ExcelCell> cellFormular(String excelName, String sheetName) {
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

	public Map<String, Object> computeCell(String excelName, String sheetName, String[] cellNames,
			Map<String, List<String>> names, boolean global) {

		Workbook workbook = retrieveWorkbook(excelName);

		if (names != null) {
			// Inject Value to the workbook
			names.forEach((address, value) -> injectValue(address, value, workbook, sheetName));
		}

		FormulaEvaluator exec = workbook.getCreationHelper().createFormulaEvaluator();

		if (global) {
			Map<String, FormulaEvaluator> workbooks = new HashMap<String, FormulaEvaluator>();

			workbooks.put("primary", exec);

			listOfResources.forEach((k, v) -> {
				if (excelName.compareTo(k) != 0) {
					workbooks.put(listOfResources.get(k).getFile(),
							this.retrieveWorkbook(k).getCreationHelper().createFormulaEvaluator());
				}
			});

			exec.setupReferencedWorkbooks(workbooks);
		}
		// Compute All cellNames
		Map<String, Object> map = Arrays.stream(cellNames).map(addr -> retrieveCellByAdress(addr, workbook, sheetName))
				.collect(Collectors.toMap(cell -> retrieveFullCellName(cell, sheetName),
						cell -> computeCell(cell, exec)));

		return map;
	}

	private Object computeCell(Cell cell, FormulaEvaluator exec) {
		return getRawCell(exec.evaluateInCell(cell));
	}

	private String retrieveFullCellName(Cell cell, String defaultSheetName) {
		return (defaultSheetName.compareTo(cell.getSheet().getSheetName()) != 0)
				? cell.getSheet().getSheetName() + "!" + cell.getAddress().formatAsString()
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
		Cell cell = row.getCell(cr.getCol(), MissingCellPolicy.CREATE_NULL_AS_BLANK);
		return cell;
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

	private ExcelCell celltoExcelCell(Cell cell) {
		ExcelCell ret = new ExcelCell();

		ret.setAddress(cell.getAddress().formatAsString());
		ret.setValue(getRawCell(cell));
		ret.setType(cell.getCellType().name());
		if (cell.getCellComment() != null) {
			ret.setMetadata(cell.getCellComment().getString().toString());
		}
		return ret;
	}
}
