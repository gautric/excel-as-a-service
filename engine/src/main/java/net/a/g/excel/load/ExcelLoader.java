package net.a.g.excel.load;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Predicate;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.a.g.excel.engine.ExcelEngine;
import net.a.g.excel.util.ExcelConfiguration;

@ApplicationScoped
public class ExcelLoader {

	public final static Logger LOG = LoggerFactory.getLogger(ExcelLoader.class);

	@Inject
	ExcelConfiguration conf;

	@Inject
	ExcelEngine engine;

	@PostConstruct
	public void loadFile() {
		try {
			Predicate<Path> excelFilter = f -> !(f).getFileName().toString().startsWith("~")
					&& (f.getFileName().toString().endsWith("xls") || f.getFileName().toString().endsWith("xlsx"));

			InputStream inputStream = ExcelEngine.class.getResourceAsStream(conf.getResouceUri());

			if (inputStream != null) {
				LOG.info("Load file from classpath://{}", conf.getResouceUri());
				engine.addNewResource(FilenameUtils.getBaseName(conf.getResouceUri()), inputStream);
			} else {
				Path file = Paths.get(conf.getResouceUri());
				if (Files.isRegularFile(file)) {
					addFile(file);
				} else if (Files.isDirectory(file)) {
					Files.walk(file, 1).filter(excelFilter).forEach(this::addFile);
				} else {
					LOG.warn("Cannot read file or directory : {}", conf.getResouceUri());
				}
			}
		} catch (Exception ex) {
			LOG.error("Error while loading file", ex);
		}
	}

	private void addFile(Path file) {
		try {
			LOG.info("Load file from {}", file.getFileName());
			engine.addNewResource(FilenameUtils.removeExtension(file.getFileName().toString()),
					file.toUri().toURL().openStream());
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
