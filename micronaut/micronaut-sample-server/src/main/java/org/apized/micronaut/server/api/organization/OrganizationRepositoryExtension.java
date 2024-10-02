package org.apized.micronaut.server.api.organization;

import io.micronaut.data.annotation.Query;
import org.apized.core.model.Apized;
import org.apized.core.model.Layer;

import java.util.Optional;

@Apized.Extension(layer = Layer.REPOSITORY)
public interface OrganizationRepositoryExtension {
  Optional<Organization> findByName(String name);

  @Query(value = "select count(*) from organization", nativeQuery = true)
  Integer countOrganizations();
}
