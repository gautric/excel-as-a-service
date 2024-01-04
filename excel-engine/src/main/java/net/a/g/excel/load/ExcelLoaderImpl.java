package net.a.g.excel.load;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Predicate;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.a.g.excel.model.ExcelResource;
import net.a.g.excel.repository.ExcelRepository;
import net.a.g.excel.util.ExcelConstants;

public class ExcelLoaderImpl implements ExcelLoader {

	public final static Logger LOG = LoggerFactory.getLogger(ExcelLoaderImpl.class);

	ExcelRepository repository;

	private String resouceUri;

	
	public void setRepo(ExcelRepository repo) {
		this.repository = repo;
	}


	public ExcelRepository getRepo() {
		return repository;
	}

	public void setResouceUri(String resouceUri) {
		this.resouceUri = resouceUri;
	}

	public String getResouceUri() {
		return resouceUri;
	}

	public ExcelLoaderImpl() {
	}

	private void addFile(Path file) {
		try {
			LOG.info("Load file from {} ({})", file.getFileName(), file.toFile().toURI());
			injectResource(FilenameUtils.removeExtension(file.getFileName().toString()),
					FilenameUtils.getName(file.getFileName().toString()), file.toUri().toURL().openStream());
		} catch (MalformedURLException e) {
			LOG.error("Error while loading file", e);
		} catch (IOException e) {
			LOG.error("Error while loading file", e);
		}
	}

	public void init() {
		LOG.info("Load / Init");
		try {
			Predicate<Path> excelFilter = f -> !(f).getFileName().toString().startsWith("~")
					&& (f.getFileName().toString().endsWith("xls") || f.getFileName().toString().endsWith("xlsx"));

			InputStream inputStream = null;
			if (getResouceUri() != null) {
				inputStream = ExcelLoaderImpl.class.getResourceAsStream(getResouceUri());

				if (inputStream != null) {
					LOG.info("Load file from classpath:/{}", getResouceUri());
					injectResource(FilenameUtils.getBaseName(getResouceUri()), FilenameUtils.getName(getResouceUri()),
							inputStream);
				} else {
					Path file = Paths.get(getResouceUri());
					if (Files.isRegularFile(file)) {
						addFile(file);
					} else if (Files.isDirectory(file)) {
						Files.walk(file, 1).filter(excelFilter).forEach(this::addFile);
					} else {
						LOG.warn("Cannot read file or directory : {}", file.getFileName().toAbsolutePath());
					}
				}
			} else {
				LOG.warn("Param '{}' is {} or not set ", ExcelConstants.EXCEL_STATIC_RESOURCE_URI, getResouceUri());
			}
		} catch (Exception ex) {
			LOG.debug("Error while loading file", ex);
		}
	}

	public boolean injectResource(String resourceName, String resourceFileName, InputStream resourceStream) {
		LOG.info("Inject Resource");
		byte[] targetArray;
		try {
			targetArray = IOUtils.toByteArray(resourceStream);
		} catch (IOException e) {
			LOG.error("Workbook " + resourceName + " is not readable", e);
			return false;
		}

		ExcelResource excelResource = new ExcelResource();
		excelResource.setName(resourceName);
		excelResource.setFile(resourceFileName);
		excelResource.setDoc(targetArray);

		repository.add(excelResource);

		return true;
	}
}
