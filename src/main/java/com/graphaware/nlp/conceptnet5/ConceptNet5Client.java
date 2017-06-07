/*
 * Copyright (c) 2013-2016 GraphAware
 *
 * This file is part of the GraphAware Framework.
 *
 * GraphAware Framework is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details. You should have received a copy of
 * the GNU General Public License along with this program.  If not, see
 * <http://www.gnu.org/licenses/>.
 */
package com.graphaware.nlp.conceptnet5;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.core.MediaType;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConceptNet5Client {
    private static final Logger LOG = LoggerFactory.getLogger(ConceptNet5Client.class);

    private final String conceptNet5EndPoint;
    private final ClientConfig cfg;

    private final Cache<String, ConceptNet5EdgeResult> cache = CacheBuilder
            .newBuilder()
            .maximumSize(10000)
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .build();

    public ConceptNet5Client(String conceptNet5EndPoint) {
        this.conceptNet5EndPoint = conceptNet5EndPoint;
        this.cfg = new DefaultClientConfig();
        cfg.getClasses().add(JacksonJsonProvider.class);
    }

    public ConceptNet5EdgeResult getValues(String concept, String lang) {
        String url = conceptNet5EndPoint + "/c/" + lang + "/" + concept;
        ConceptNet5EdgeResult value;
        try {
            value = cache.get(url, () -> getValues(url));
        } catch (ExecutionException ex) {
            LOG.error("Error while getting value for concept " + concept + " lang " + lang, ex);
            throw new RuntimeException("Error while getting value for concept " + concept + " lang " + lang);
        }
        return value;
    }

    public ConceptNet5EdgeResult getValues(String url) {
        WebResource resource = Client.create(cfg).resource(url);
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .get(ClientResponse.class);
        ConceptNet5EdgeResult result = response.getEntity(ConceptNet5EdgeResult.class);
        return result;
    }

    public ConceptNet5EdgeResult searchByStart(String concept, String lang) {
        String url = conceptNet5EndPoint + "/search?rel=/r/IsA&start=/c/" + lang + "/" + concept + "/&limit=20";
        WebResource resource = Client.create(cfg).resource(url);
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .get(ClientResponse.class);
        ConceptNet5EdgeResult result = response.getEntity(ConceptNet5EdgeResult.class);
        return result;
    }

}
