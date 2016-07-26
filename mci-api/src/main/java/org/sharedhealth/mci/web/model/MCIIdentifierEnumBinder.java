package org.sharedhealth.mci.web.model;

import ca.uhn.fhir.model.api.IValueSetEnumBinder;

public class MCIIdentifierEnumBinder implements IValueSetEnumBinder{
    @Override
    public Enum<?> fromCodeString(String theCodeString) {
        return null;
    }

    @Override
    public Enum<?> fromCodeString(String theCodeString, String theSystemString) {
        return null;
    }

    @Override
    public String toSystemString(Enum theEnum) {
        return null;
    }

    @Override
    public String toCodeString(Enum theEnum) {
        return null;
    }
}
