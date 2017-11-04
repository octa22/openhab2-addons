package org.openhab.binding.csas.internal.discovery;

import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryServiceCallback;
import org.eclipse.smarthome.config.discovery.ExtendedDiscoveryService;

/**
 * Discovery service for accounts/financial products.
 *
 * @author Ondrej Pecta - Initial contribution
 */
public class CSASDiscoveryService extends AbstractDiscoveryService
        implements ExtendedDiscoveryService {

    public CSASDiscoveryService(int timeout) throws IllegalArgumentException {
        super(timeout);
    }

    @Override
    protected void startScan() {

    }

    @Override
    public void setDiscoveryServiceCallback(DiscoveryServiceCallback discoveryServiceCallback) {

    }
}
