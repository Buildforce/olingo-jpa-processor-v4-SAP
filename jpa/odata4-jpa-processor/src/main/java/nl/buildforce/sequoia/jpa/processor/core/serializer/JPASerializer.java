package nl.buildforce.sequoia.jpa.processor.core.serializer;

import nl.buildforce.sequoia.jpa.processor.core.api.JPAODataCRUDContextAccess;
import nl.buildforce.sequoia.jpa.processor.core.exception.ODataJPASerializerException;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.apache.olingo.server.api.serializer.SerializerResult;

import java.net.URI;
import java.net.URISyntaxException;

public interface JPASerializer {

  SerializerResult serialize(final ODataRequest request, final EntityCollection result)
      throws SerializerException, ODataJPASerializerException;

  ContentType getContentType();

  default URI buildServiceRoot(final ODataRequest request, final JPAODataCRUDContextAccess serviceContext)
          throws URISyntaxException {
    if (serviceContext.useAbsoluteContextURL()) {
      final String serviceRoot = request.getRawBaseUri();
      if (serviceRoot == null)
        return null;
      return new URI(serviceRoot.endsWith("/") ? serviceRoot : (serviceRoot + "/"));
    }
    return null;
  }
}