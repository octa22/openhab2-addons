package org.openhab.binding.csas.internal.model.response;

import org.openhab.binding.csas.internal.model.CSASCard;

import java.util.ArrayList;

public class CSASCardsResponse {
    private ArrayList<CSASCard> cards;

    public ArrayList<CSASCard> getCards() {
        return cards;
    }
}
