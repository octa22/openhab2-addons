package org.openhab.binding.csas.internal.model.response;

import org.openhab.binding.csas.internal.model.CSASInsurance;

import java.util.ArrayList;

public class CSASInsurancesResponse {
    private ArrayList<CSASInsurance> insurances;

    public ArrayList<CSASInsurance> getInsurances() {
        return insurances;
    }
}
