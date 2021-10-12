package net.a.g.excel;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class ExcelProfileTest implements QuarkusTestProfile {
	
	public ExcelProfileTest() {}

	public Map<String, String> getConfigOverrides() {
		Map<String, String> map = new HashMap<String, String>();
		map.put("excel.static.resouces.uri", "/KYC.xlsx");
		map.put("excel.return.list.or.map", "LIST");
		return map;
	}

}
