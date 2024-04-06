package net.a.g.excel.rest.test;

import java.util.ArrayList;
import java.util.List;

import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import net.a.g.excel.model.ExcelRequest;
import net.a.g.excel.rest.ExcelRestResource;

@QuarkusTest
@Disabled
public class ExcelResourceLocalTest {

	@Inject
	ExcelRestResource err;


	@BeforeAll
	public static void getUriInfo() {
		
		 ResteasyProviderFactory.getInstance().createLinkBuilder().baseUri("dfdfdf").uri("test");
	}

	@Test
	public void test_ok_POST_KYC_ComputeKYC_SCORE_PEP_true_COUNTRY_CY_TOTOTO_1000000() throws JsonProcessingException {

		JSONObject body = new JSONObject();
		body.put("resource", "KYC");
		body.put("sheet", "ComputeKYC");
		body.put("force", true);
		List a = new ArrayList();
		a.add("SCORE");
		body.put("outputs", a);

		ExcelRequest er = new ExcelRequest();
		er.setForce(true);
		er.setResource("KYC");
		er.setSheet("ComputeKYC");
		er.getOutputs().add("SCORE");

		ObjectMapper mapper = new ObjectMapper();
		String json = mapper.writeValueAsString(er);
		System.out.println("ResultingJSONstring = " + json);

		err.computePOST(er);

	// @formatter:off
			
	// @formatter:on

	}

}