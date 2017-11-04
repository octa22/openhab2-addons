package org.openhab.binding.csas.internal.model;

public class CSASInsurance {
    private String id;
    private String policyNumber;
    private String productI18N;
    private String status;

    public String getId() {
        return id;
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public String getProductI18N() {
        return productI18N;
    }

    public String getStatus() {
        return status;
    }
}
