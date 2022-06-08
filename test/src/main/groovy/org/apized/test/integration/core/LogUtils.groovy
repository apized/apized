/*
 * Copyright 2022 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apized.test.integration.core

import org.slf4j.Logger

class LogUtils {

  private static final int LOG_HEADER_WIDTH = 60

  static void logHeaderCenter(Logger log, String line) {
    logHeaderCenter(log, line, LOG_HEADER_WIDTH)
  }

  static void logHeaderCenter(Logger log, String line, int maxWidth) {
    if (line.length() > maxWidth - 4) {
      // If a line is too long, split it recursively
      logHeaderCenter(log, line.substring(0, maxWidth - 10), maxWidth)
      logHeaderCenter(log, line.substring(maxWidth - 10), maxWidth)
      return
    }
    log.debug(formatHeaderCenter(line, maxWidth))
  }

  static String formatHeaderCenter(String line) {
    formatHeaderCenter(line, LOG_HEADER_WIDTH)
  }

  static String formatHeaderCenter(String line, int maxWidth) {
    int length = line.length()
    // We need 2 spaces for the border around the header
    int padding = maxWidth - (length + 2)
    int leftPadding = Math.max((int) (padding / 2), 2)
    // add a potential uneven padding to the right
    int rightPadding = Math.max(leftPadding + (padding % 2), 2)

    "#".repeat(leftPadding) + " " + line + " " + "#".repeat(rightPadding)
  }

  static void logHeader(Logger log, String... content) {
    log.debug("#".repeat(LOG_HEADER_WIDTH))
    for (line in content) {
      logHeaderCenter(log, line)
    }
    log.debug("#".repeat(LOG_HEADER_WIDTH))
  }

  static void logLongHeader(Logger log, String... content) {
    int maxLength = content.toList().stream().mapToInt(s -> s.length() + 8).max().orElse(LOG_HEADER_WIDTH)
    String header = "\n" + "#".repeat(maxLength) + "\n"
    for (line in content) {
      header += formatHeaderCenter(line, maxLength) + "\n"
    }
    header += "#".repeat(maxLength) + "\n"

    log.debug(header)
  }
}
