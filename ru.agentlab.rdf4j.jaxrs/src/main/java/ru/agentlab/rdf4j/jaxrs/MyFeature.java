package ru.agentlab.rdf4j.jaxrs;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

//import org.apache.cxf.jaxrs.provider.MultipartProvider;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsExtension;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsName;

//@Component
//@JaxrsName("myFeature")
//@JaxrsExtension
public class MyFeature implements Feature {
	
	public MyFeature() {
		System.out.println(this.getClass().getSimpleName() + "started");
	}

    @Override
    public boolean configure(FeatureContext fc) {
    	//CrossOriginResourceSharingFilter.class
        //fc.register(MultipartProvider.class);
		return true;
    }
} 
