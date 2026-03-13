package org.apized.core;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Properties to configure a federated access.
 */
@Data
public class FederationConfig {
  /**
   * The baseUrl for the federated service
   */
  protected String baseUrl;

  /**
   * The Map of query params to be sent
   */
  protected List<String> queryParams = new ArrayList<>();

  /**
   * The Map of headers to be sent
   */
  protected Map<String, String> headers = new HashMap<>();
}
