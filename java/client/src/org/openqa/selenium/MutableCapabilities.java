// Licensed to the Software Freedom Conservancy (SFC) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The SFC licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.openqa.selenium;


import org.openqa.selenium.logging.LogLevelMapping;
import org.openqa.selenium.logging.LoggingPreferences;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class MutableCapabilities implements Capabilities, Serializable {

  private static final long serialVersionUID = -112816287184979465L;

  private static final Set<String> OPTION_KEYS;
  static {
    HashSet<String> keys = new HashSet<>();
    keys.add("chromeOptions");
    keys.add("edgeOptions");
    keys.add("goog:chromeOptions");
    keys.add("moz:firefoxOptions");
    keys.add("operaOptions");
    keys.add("se:ieOptions");
    keys.add("safari.options");
    OPTION_KEYS = Collections.unmodifiableSet(keys);
  }

  private final Map<String, Object> caps = new HashMap<>();


  public MutableCapabilities() {
    // no-arg constructor
  }

  public MutableCapabilities(Capabilities other) {
    this(other.asMap());
  }

  public MutableCapabilities(Map<String, ?> capabilities) {
    capabilities.forEach((key, value) -> {
      if (value != null) {
        setCapability(key, value);
      }
    });
  }

  @Override
  public Object getCapability(String capabilityName) {
    return caps.get(capabilityName);
  }

  @Override
  public Map<String, ?> asMap() {
    return Collections.unmodifiableMap(caps);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Capabilities)) {
      return false;
    }

    Capabilities that = (Capabilities) o;

    return asMap().equals(that.asMap());
  }

  @Override
  public int hashCode() {
    return Objects.hash(amendHashCode(), caps);
  }

  /**
   * Subclasses can use this to add information that isn't always in the capabilities map.
   * @return
   */
  protected int amendHashCode() {
    return 0;
  }

  /**
   * Merges the extra capabilities provided into this DesiredCapabilities instance. If capabilities
   * with the same name exist in this instance, they will be overridden by the values from the
   * extraCapabilities object.
   *
   * @param extraCapabilities Additional capabilities to be added.
   * @return DesiredCapabilities after the merge
   */
  @Override
  public MutableCapabilities merge(Capabilities extraCapabilities) {
    if (extraCapabilities == null) {
      return this;
    }

    extraCapabilities.asMap().forEach(this::setCapability);

    return this;
  }

  public void setCapability(String capabilityName, boolean value) {
    setCapability(capabilityName, (Object) value);
  }

  public void setCapability(String capabilityName, String value) {
    setCapability(capabilityName, (Object) value);
  }

  public void setCapability(String capabilityName, Platform value) {
    setCapability(capabilityName, (Object) value);
  }

  public void setCapability(String key, Object value) {
    // We have to special-case some keys and values because of the popular idiom of calling
    // something like "capabilities.setCapability(SafariOptions.CAPABILITY, new SafariOptions());
    // and this is no longer needed as options are capabilities. There will be a large amount of
    // legacy code that will always try and follow this pattern, however.
    if (OPTION_KEYS.contains(key) && value instanceof Capabilities) {
      merge((Capabilities) value);
      return;
    }

    if ("loggingPrefs".equals(key) && value instanceof Map) {
      LoggingPreferences prefs = new LoggingPreferences();
      @SuppressWarnings("unchecked") Map<String, String> prefsMap = (Map<String, String>) value;

      for (String logType : prefsMap.keySet()) {
        prefs.enable(logType, LogLevelMapping.toLevel(prefsMap.get(logType)));
      }
      caps.put(key, prefs);
      return;
    }

    if ("platform".equals(key) && value instanceof String) {
      try {
        caps.put(key, Platform.fromString((String) value));
      } catch (WebDriverException e) {
        caps.put(key, value);
      }
      return;
    }

    if ("unexpectedAlertBehaviour".equals(key)) {
      caps.put("unexpectedAlertBehaviour", value);
      caps.put("unhandledPromptBehavior", value);
      return;
    }

    if (value == null) {
      caps.remove(key);
    } else {
      caps.put(key, value);
    }
  }

  @Override
  public String toString() {
    return String.format("Capabilities [%s]", shortenMapValues(asMap()));
  }

  private Map<String, ?> shortenMapValues(Map<String, ?> map) {
    Map<String, Object> newMap = new HashMap<>();

    for (Map.Entry<String, ?> entry : map.entrySet()) {
      if (entry.getValue() instanceof Map) {
        @SuppressWarnings("unchecked") Map<String, ?> value = (Map<String, ?>) entry.getValue();
        newMap.put(entry.getKey(), shortenMapValues(value));

      } else {
        String value = String.valueOf(entry.getValue());
        if (value.length() > 1024) {
          value = value.substring(0, 29) + "...";
        }
        newMap.put(entry.getKey(), value);
      }
    }

    return newMap;
  }
}
