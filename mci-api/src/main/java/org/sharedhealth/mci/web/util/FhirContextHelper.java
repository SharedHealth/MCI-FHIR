package org.sharedhealth.mci.web.util;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.StrictErrorHandler;
import org.hl7.fhir.instance.model.api.IBaseResource;

public class FhirContextHelper {
    public static FhirContext fhirContext = getFhirContext();

    private static FhirContext getFhirContext() {
        FhirContext context = FhirContext.forDstu2();
        context.setParserErrorHandler(new StrictErrorHandler());
        return context;
    }

    public static String encodeResource(IBaseResource resource) {
        return fhirContext.newXmlParser().encodeResourceToString(resource);
    }

    public static IBaseResource parseResource(String content) {
        return fhirContext.newXmlParser().parseResource(content);
    }
}
