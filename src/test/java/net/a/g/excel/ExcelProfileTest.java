package net.a.g.excel;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class ExcelProfileTest implements QuarkusTestProfile {
	
	public ExcelProfileTest() {}

	public Map<String, String> getConfigOverrides() {
		Map<String, String> map = new HashMap();
		map.put("excel.static.resouces.uri", "KYC.xlsx");
		return map;
	}

}
