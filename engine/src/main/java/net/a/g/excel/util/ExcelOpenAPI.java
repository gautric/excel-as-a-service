package net.a.g.excel.util;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.OASModelReader;
import org.eclipse.microprofile.openapi.models.OpenAPI;

@ApplicationScoped
public class ExcelOpenAPI implements OASModelReader {

    @Override
    public OpenAPI buildModel() {
        return OASFactory.createObject(OpenAPI.class)
                .info(OASFactory.createInfo()
                    .title("Sample App defined with Model Reader Implementation")
                    .version("1.0")
                    .termsOfService("http://termsofservice.com/terms")
                    .contact(OASFactory.createContact()
                        .name("Sample API Support")
                        .url("http://sampleappone.com/contact")
                        .email("techsupport@sampleappone.com"))
                    .license(OASFactory.createLicense()
                        .name("Apache 2.0")
                        .url("http://www.apache.org/licenses/LICENSE-2.0.html")));


}}
