package net.a.g.excel.load;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Predicate;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Destroyed;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.a.g.excel.engine.ExcelEngine;
import net.a.g.excel.model.ExcelResource;
import net.a.g.excel.util.ExcelConfiguration;

@ApplicationScoped
public class ExcelLoader {

	public final static Logger LOG = LoggerFactory.getLogger(ExcelLoader.class);

	@Inject
	ExcelEngine engine;

	@Inject
	ExcelConfiguration conf;

	public ExcelLoader() {
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

	public void init(@Observes @Initialized(ApplicationScoped.class) Object init) {
		try {
			Predicate<Path> excelFilter = f -> !(f).getFileName().toString().startsWith("~")
					&& (f.getFileName().toString().endsWith("xls") || f.getFileName().toString().endsWith("xlsx"));

			InputStream inputStream = ExcelEngine.class.getResourceAsStream(conf.getResouceUri());

			if (inputStream != null) {
				LOG.info("Load file from classpath:/{}", conf.getResouceUri());
				injectResource(FilenameUtils.getBaseName(conf.getResouceUri()),
						FilenameUtils.getName(conf.getResouceUri()), inputStream);
			} else {
				Path file = Paths.get(conf.getResouceUri());
				if (Files.isRegularFile(file)) {
					addFile(file);
				} else if (Files.isDirectory(file)) {
					Files.walk(file, 1).filter(excelFilter).forEach(this::addFile);
				} else {
					LOG.warn("Cannot read file or directory : {}", file.getFileName().toAbsolutePath());
				}
			}
		} catch (Exception ex) {
			LOG.debug("Error while loading file", ex);
		}
	}

	public void destroy(@Observes @Destroyed(ApplicationScoped.class) Object init) {
	}

	public boolean injectResource(String resourceName, String resourceFileName, InputStream resourceStream) {

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

		engine.addResource(excelResource);

		return true;
	}
}
