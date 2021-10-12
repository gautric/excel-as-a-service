package net.a.g.excel.camel;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;


public class ExcelCamelRoute extends RouteBuilder {
	@Override
    public void configure() throws Exception {

        restConfiguration().bindingMode(RestBindingMode.json);

        rest("/fruits")
                .get()
                .route()
                .setBody().constant("")
                //.to("log:foo")
                .endRest()

                .post()
                //.type(Fruit.class)
                .route()
                .setBody().constant("")
               // .process().body(Fruit.class, fruits::add)
               // .setBody().constant(fruits)
                .endRest();

//        rest("/legumes")
//                .get()
//                .route()
//               // .setBody().constant(legumes)
//                .endRest();
    }
}
