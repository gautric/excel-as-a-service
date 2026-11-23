package net.a.g.excel.kafka;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.BiFunction;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import net.a.g.excel.engine.ExcelEngineImpl;
import net.a.g.excel.model.ExcelResource;
import net.a.g.excel.param.ExcelParameterImpl;
import net.a.g.excel.repository.ExcelRepositoryImpl;

/** Hand-built beans shared by the module's unit tests (no Quarkus, no broker). */
public final class KafkaTestSupport {

	private KafkaTestSupport() {
	}

	/** Engine over ../sample/KYCAPI.xlsx (API names) and ../sample/KYC.xlsx (cell references only). */
	public static ExcelEngineImpl engine() {
		ExcelRepositoryImpl repo = new ExcelRepositoryImpl();
		for (String name : List.of("KYCAPI", "KYC")) {
			ExcelResource r = new ExcelResource();
			r.setName(name);
			r.setFile(name + ".xlsx");
			try {
				r.setDoc(Files.readAllBytes(Path.of("../sample/" + name + ".xlsx")));
			} catch (java.io.IOException e) {
				throw new IllegalStateException(e);
			}
			repo.add(r);
		}
		ExcelEngineImpl engine = new ExcelEngineImpl();
		engine.setRepository(repo);
		engine.setParameter(new ExcelParameterImpl());
		return engine;
	}

	/** Same setting as the ObjectMapper Quarkus injects: unknown properties are ignored. */
	public static ObjectMapper om() {
		return JsonMapper.builder().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();
	}

	/** Minimal fake of an interface: every call goes to {@code handler(methodName, args)}. */
	@SuppressWarnings("unchecked")
	public static <T> T fake(Class<T> type, BiFunction<String, Object[], Object> handler) {
		return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, (proxy, m, args) -> {
			switch (m.getName()) {
			case "equals":
				return proxy == args[0];
			case "hashCode":
				return System.identityHashCode(proxy);
			case "toString":
				return "fake " + type.getSimpleName();
			default:
				return handler.apply(m.getName(), args == null ? new Object[0] : args);
			}
		});
	}
}
