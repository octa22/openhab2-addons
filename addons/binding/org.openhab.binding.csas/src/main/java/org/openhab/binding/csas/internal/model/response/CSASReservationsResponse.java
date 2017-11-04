package org.openhab.binding.csas.internal.model.response;

import org.openhab.binding.csas.internal.model.CSASReservation;

import java.util.ArrayList;

public class CSASReservationsResponse {
    private ArrayList<CSASReservation> reservations;

    public ArrayList<CSASReservation> getReservations() {
        return reservations;
    }
}
