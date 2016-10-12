package org.sharedhealth.mci.web.model;

import java.nio.file.AccessDeniedException;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class Requester {

    private RequesterDetails facility;
    private RequesterDetails provider;
    private RequesterDetails admin;

    public Requester() {
    }

    public Requester(String facilityId, String providerId, String adminId, String name) throws AccessDeniedException {
        if (facilityId == null && providerId == null && adminId == null) {
            throw new AccessDeniedException("All of facility, provider and admin cannot be empty");
        }

        if (isNotBlank(facilityId)) {
            this.facility = new RequesterDetails(facilityId);
        }

        if (isNotBlank(providerId)) {
            this.provider = new RequesterDetails(providerId);
        }

        if (isNotBlank(adminId)) {
            this.admin = new RequesterDetails(adminId, name);
        }
    }

    public RequesterDetails getFacility() {
        return facility;
    }

    public void setFacility(RequesterDetails facility) {
        this.facility = facility;
    }

    public RequesterDetails getProvider() {
        return provider;
    }

    public void setProvider(RequesterDetails provider) {
        this.provider = provider;
    }

    public RequesterDetails getAdmin() {
        return admin;
    }

    public void setAdmin(RequesterDetails admin) {
        this.admin = admin;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Requester)) return false;

        Requester requester = (Requester) o;

        if (admin != null ? !admin.equals(requester.admin) : requester.admin != null) return false;
        if (facility != null ? !facility.equals(requester.facility) : requester.facility != null) return false;
        if (provider != null ? !provider.equals(requester.provider) : requester.provider != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = facility != null ? facility.hashCode() : 0;
        result = 31 * result + (provider != null ? provider.hashCode() : 0);
        result = 31 * result + (admin != null ? admin.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Requester{");
        sb.append("facility=").append(facility);
        sb.append(", provider=").append(provider);
        sb.append(", admin=").append(admin);
        sb.append('}');
        return sb.toString();
    }
}
