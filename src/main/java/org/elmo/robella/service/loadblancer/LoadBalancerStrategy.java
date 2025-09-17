package org.elmo.robella.service.loadblancer;

import java.util.List;

import org.elmo.robella.model.entity.VendorModel;

public interface LoadBalancerStrategy {
    VendorModel select(List<VendorModel> candidates);
    

}
