package org.apized.spring.server.api.organization;

import org.apized.core.model.Apized;
import org.apized.core.model.Layer;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

@Apized.Extension(layer = Layer.REPOSITORY)
public interface OrganizationRepositoryExtension {
  Optional<Organization> findByName(String name);

  @Query(value = "select count(*) from organization", nativeQuery = true)
  Integer countOrganizations();
}
