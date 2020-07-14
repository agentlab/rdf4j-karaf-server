package ru.agentlab.rdf4j.jaxrs;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

import org.apache.cxf.rs.security.cors.CrossOriginResourceSharingFilter;
//import org.apache.cxf.jaxrs.provider.MultipartProvider;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsExtension;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsName;

/**
 * With Feature Component you can register any Jax-RS (non-OSGi) class as provider
 * 
 */
@Component
@JaxrsName("myFeature")
@JaxrsExtension
public class MyFeature implements Feature {
	
	//public MyFeature() {
	//	System.out.println(this.getClass().getSimpleName() + "started");
	//}

    @Override
    public boolean configure(FeatureContext fc) {
        fc.register(CrossOriginResourceSharingFilter.class);
        //fc.register(MultipartProvider.class);
		return true;
    }
} 
