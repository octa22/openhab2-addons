package org.openhab.binding.csas.internal.model.response;

import org.openhab.binding.csas.internal.model.CSASAccount;

import java.util.ArrayList;

public class CSASBuildingsResponse {
    private ArrayList<CSASAccount> buildings;

    public ArrayList<CSASAccount> getBuildings() {
        return buildings;
    }
}
