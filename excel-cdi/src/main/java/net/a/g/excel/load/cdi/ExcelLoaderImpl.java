package net.a.g.excel.load.cdi;

import org.eclipse.microprofile.config.ConfigProvider;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Destroyed;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import net.a.g.excel.repository.ExcelRepository;
import net.a.g.excel.util.ExcelConstants;

@ApplicationScoped
@Named
public class ExcelLoaderImpl extends net.a.g.excel.load.ExcelLoaderImpl {

	@Inject
	public void setRepository(ExcelRepository repo) {
		super.setRepository(repo);
	}

	public void init(@Observes @Initialized(ApplicationScoped.class) Object init) {
		String repo = ConfigProvider.getConfig().getValue(ExcelConstants.EXCEL_STATIC_RESOURCE_URI, String.class);

		super.setResouceUri(repo);

		super.init();
	}

	public void destroy(@Observes @Destroyed(ApplicationScoped.class) Object init) {
	}
}
