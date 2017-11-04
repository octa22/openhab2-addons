package org.openhab.binding.csas.internal.discovery;

import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.config.discovery.DiscoveryServiceCallback;
import org.eclipse.smarthome.config.discovery.ExtendedDiscoveryService;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.csas.handler.CSASBridgeHandler;
import org.openhab.binding.csas.internal.model.CSASAccountNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.openhab.binding.csas.CSASBindingConstants.*;

/**
 * Discovery service for accounts/financial products.
 *
 * @author Ondrej Pecta - Initial contribution
 */
public class CSASDiscoveryService extends AbstractDiscoveryService
        implements ExtendedDiscoveryService {

    private final Logger logger = LoggerFactory.getLogger(CSASDiscoveryService.class);

    private CSASBridgeHandler bridge = null;
    private DiscoveryServiceCallback discoveryServiceCallback;

    public CSASDiscoveryService(CSASBridgeHandler bridgeHandler) {
        super(DISCOVERY_TIMEOUT_SEC);
        logger.debug("Creating discovery service");
        this.bridge = bridgeHandler;
        bridgeHandler.setDiscoveryService(this);
    }

    @Override
    public Set<ThingTypeUID> getSupportedThingTypes() {
        return new HashSet<>(Arrays.asList(
                THING_TYPE_ACCOUNT,
                THING_TYPE_BS_ACCOUNT,
                THING_TYPE_CARD_ACCOUNT,
                THING_TYPE_SECURITIES_ACCOUNT,
                THING_TYPE_INSURANCE_CONTRACT,
                THING_TYPE_LOYALTY_CONTRACT,
                THING_TYPE_PENSION_CONTRACT
        ));
    }


    @Override
    protected void startScan() {
        logger.info("Starting discovery...");
        bridge.startDiscovery();
    }

    @Override
    public void setDiscoveryServiceCallback(DiscoveryServiceCallback discoveryServiceCallback) {
        this.discoveryServiceCallback = discoveryServiceCallback;
    }

    private void deviceDiscovered(String id, String label, ThingTypeUID thingTypeUID) {
        Map<String, Object> properties = new HashMap<>(1);
        properties.put("id", id);

        ThingUID thingUID = new ThingUID(thingTypeUID, bridge.getThing().getUID(), id);

        if (discoveryServiceCallback.getExistingThing(thingUID) == null) {
            logger.debug("Detected a/an {} - label: {} id: {}", thingTypeUID.getId(), label, id);
            thingDiscovered(
                    DiscoveryResultBuilder.create(thingUID).withThingType(thingTypeUID).withProperties(properties)
                            .withRepresentationProperty("id").withLabel(label)
                            .withBridge(bridge.getThing().getUID()).build());
        }
    }

    public void accountDiscovered(String id, CSASAccountNumber accountNr) {
        deviceDiscovered(id, accountNr.getFullAccount(), THING_TYPE_ACCOUNT);
    }

    public void cardAccountDiscovered(String id, CSASAccountNumber accountNr) {
        deviceDiscovered(id, accountNr.getFullAccount(), THING_TYPE_CARD_ACCOUNT);
    }

    public void buildingSavingsAccountDiscovered(String id, CSASAccountNumber accountNr) {
        deviceDiscovered(id, accountNr.getFullAccount(), THING_TYPE_BS_ACCOUNT);
    }

    public void pensionContractDiscovered(String id, String agreement) {
        deviceDiscovered(id, agreement, THING_TYPE_PENSION_CONTRACT);
    }

    public void insuranceContractDiscovered(String id, String description) {
        deviceDiscovered(id, description, THING_TYPE_INSURANCE_CONTRACT);
    }

    public void securitiesAccountDiscovered(String id, String accountNr) {
        deviceDiscovered(id, accountNr, THING_TYPE_SECURITIES_ACCOUNT);
    }

    public void loyaltyContractDiscovered() {
        deviceDiscovered(IBOD, IBOD, THING_TYPE_LOYALTY_CONTRACT);
    }
}
