package net.a.g.excel.load;

import java.io.InputStream;

import net.a.g.excel.repository.ExcelRepository;

public interface ExcelLoader {

	void setRepo(ExcelRepository repo);

	boolean injectResource(String resourceName, String resourceFileName, InputStream resourceStream);

}