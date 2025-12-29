package com.howtogrow.backend.service.admin;

import com.howtogrow.backend.controller.admin.dto.DimensionView;
import com.howtogrow.backend.domain.capability.CapabilityDimension;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AdminDimensionService {
  public List<DimensionView> list() {
    return CapabilityDimension.ordered().stream()
        .map(d -> new DimensionView(d.code(), d.displayName(), d.sortNo()))
        .toList();
  }
}

