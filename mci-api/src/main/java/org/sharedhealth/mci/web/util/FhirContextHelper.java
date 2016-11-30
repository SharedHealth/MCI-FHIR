package org.sharedhealth.mci.web.util;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.parser.StrictErrorHandler;
import org.hl7.fhir.instance.model.api.IBaseResource;

public class FhirContextHelper {
    public static FhirContext fhirContext = getFhirContext();
    private static final IParser xmlParser = fhirContext.newXmlParser();
    public static FhirContext fhirHL7Context = FhirContext.forDstu2Hl7Org();


    private static FhirContext getFhirContext() {
        FhirContext context = FhirContext.forDstu2();
        context.setParserErrorHandler(new StrictErrorHandler());
        return context;
    }

    public static String encodeResource(IBaseResource resource) {
        return xmlParser.encodeResourceToString(resource);
    }

    public static IBaseResource parseResource(String content) {
        return xmlParser.parseResource(content);
    }
}
