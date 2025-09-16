package org.elmo.robella.service.transform.provider;

import org.elmo.robella.common.ProviderType;
import org.springframework.stereotype.Component;

@Component
public class NoOpVendorTransform implements VendorTransform<Object, Object> {

    @Override
    public ProviderType providerType() {
        return ProviderType.NONE;
    }
}