package net.a.g.excel.load;

import java.io.InputStream;

public interface ExcelLoader {

	boolean injectResource(String resourceName, String resourceFileName, InputStream resourceStream);

}