package org.elmo.robella.service.transform.provider;

import org.elmo.robella.common.EndpointType;
import org.elmo.robella.common.ProviderType;

public interface VendorTransform<V, R>{

    ProviderType providerType();

    default EndpointType type() {
        return providerType().getEndpointType();
    }

    default V processRequest(V vendorRequest) {
        return vendorRequest;
    }
    
}
