package net.a.g.excel.rest.test;

import static io.restassured.RestAssured.given;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Shared helpers for the HTTP tests: sample workbooks and resources with
 * unique names, so that tests which upload, replace or delete never collide.
 */
final class ApiFixtures {

	static final String API = "/eaas/api";
	static final String BASE = "http://localhost:8081" + API;
	static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

	private ApiFixtures() {
	}

	static byte[] sample(String file) {
		try {
			return Files.readAllBytes(Path.of("../sample", file));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/** A resource name no other test uses. */
	static String unique(String prefix) {
		return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
	}

	/**
	 * Uploads a sample under {@code name}; the multipart file name is unique too
	 * (cross-workbook lookup keys resources by file name).
	 */
	static String upload(String name, String sampleFile) {
		return upload(name, sampleFile, name + ".xlsx");
	}

	static String upload(String name, String sampleFile, String fileName) {
		given().multiPart("file", fileName, sample(sampleFile)).when().post(API + "/" + name).then()
				.statusCode(201);
		return name;
	}

	/** Best-effort cleanup, used in finally blocks and @AfterAll. */
	static void delete(String name) {
		given().when().delete(API + "/" + name);
	}
}
