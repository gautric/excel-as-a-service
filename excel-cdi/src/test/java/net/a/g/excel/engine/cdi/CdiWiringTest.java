package net.a.g.excel.engine.cdi;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.inject.Inject;
import net.a.g.excel.engine.ExcelEngine;
import net.a.g.excel.load.ExcelLoader;
import net.a.g.excel.param.ExcelParameter;
import net.a.g.excel.repository.ExcelRepository;
import net.a.g.excel.util.ExcelConstants;

@DisplayName("CDI wiring of the excel-cdi beans")
public class CdiWiringTest extends CdiTestSupport {

	@Inject
	net.a.g.excel.load.cdi.ExcelLoaderImpl loaderImpl;

	static Stream<Arguments> beanTypes() {
		return Stream.of(
				Arguments.of(ExcelEngine.class, ExcelEngineImpl.class),
				Arguments.of(ExcelLoader.class, net.a.g.excel.load.cdi.ExcelLoaderImpl.class),
				Arguments.of(ExcelParameter.class, net.a.g.excel.param.cdi.ExcelParameterImpl.class),
				Arguments.of(ExcelRepository.class, net.a.g.excel.repository.cdi.ExcelRepositoryImpl.class));
	}

	@ParameterizedTest(name = "{0} resolves to one @ApplicationScoped {1}")
	@MethodSource("beanTypes")
	void eachInterfaceHasExactlyOneApplicationScopedBean(Class<?> type, Class<?> impl) {
		Set<Bean<?>> beans = weld.getBeanManager().getBeans(type);
		assertThat(beans, hasSize(1));
		Bean<?> bean = beans.iterator().next();
		assertEquals(ApplicationScoped.class, bean.getScope());
		assertEquals(impl, bean.getBeanClass());
	}

	@ParameterizedTest(name = "@Named ''{0}''")
	@ValueSource(strings = { "excelEngineImpl", "excelLoaderImpl", "excelParameterImpl", "excelRepositoryImpl" })
	void beansAreResolvableByDefaultName(String name) {
		assertThat(weld.getBeanManager().getBeans(name), hasSize(1));
	}

	@Test
	void applicationScopeSharesOneRepositoryBetweenLoaderAndEngine() {
		load("KYC", "KYCAPI.xlsx");
		ExcelRepository looked = weld.select(ExcelRepository.class).get();
		assertTrue(looked.contains("KYC"));
		assertThat(engine.listOfSheet("KYC"), hasSize(3));
	}

	@Test
	void loaderObserverReadsResourceUriFromConfig() {
		String configured = ConfigProvider.getConfig().getValue(ExcelConstants.EXCEL_STATIC_RESOURCE_URI, String.class);
		assertThat(loaderImpl.getResouceUri(), is(configured));
		assertThat(loaderImpl.getRepository(), instanceOf(ExcelRepository.class));
	}
}
