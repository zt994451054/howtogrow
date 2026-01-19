package com.howtogrow.backend.service.miniprogram;

import com.howtogrow.backend.controller.miniprogram.dto.TroubleSceneView;
import com.howtogrow.backend.infrastructure.trouble.TroubleSceneRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MiniprogramTroubleSceneService {
  private final TroubleSceneRepository sceneRepo;

  public MiniprogramTroubleSceneService(TroubleSceneRepository sceneRepo) {
    this.sceneRepo = sceneRepo;
  }

  public List<TroubleSceneView> list() {
    return sceneRepo.listActive().stream()
        .map(r -> new TroubleSceneView(r.id(), r.name(), r.logoUrl(), r.minAge(), r.maxAge()))
        .toList();
  }
}
