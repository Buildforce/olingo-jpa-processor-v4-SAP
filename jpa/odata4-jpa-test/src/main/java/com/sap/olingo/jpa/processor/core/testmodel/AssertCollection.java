package com.sap.olingo.jpa.processor.core.testmodel;

import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class AssertCollection {

  public static <T> void assertListEquals(final List<T> exp, final List<T> act, Class<T> reflection) {
    assertEquals(exp.size(), act.size());
    boolean found;
    for (final T expItem : exp) {
      for (T actItem : act) {
        found = EqualsBuilder.reflectionEquals(expItem, actItem, true, reflection);
        if (found) {
          break;
        }
        assertFalse(found, "Cloud not find" + expItem.toString());
      }
    }
  }

  private AssertCollection() {
    super();
  }
}