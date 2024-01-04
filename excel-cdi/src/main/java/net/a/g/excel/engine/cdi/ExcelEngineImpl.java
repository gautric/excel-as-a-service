
package net.a.g.excel.engine.cdi;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import net.a.g.excel.param.ExcelParameter;
import net.a.g.excel.repository.ExcelRepository;

@ApplicationScoped
@Named
public class ExcelEngineImpl extends net.a.g.excel.engine.ExcelEngineImpl {

	@Inject
	public void setParameter(ExcelParameter param) {
		super.setParameter(param);
	}

	@Inject
	public void setRepository(ExcelRepository repo) {
		super.setRepository(repo);
	}

}
