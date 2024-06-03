package org.apized.core.mvc;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class ManyToManyTuple {
  private final UUID self;
  private final UUID other;
}
