/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.hisensetv.internal;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.auth.Role;
import org.openhab.core.io.rest.RESTConstants;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingUID;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsApplicationSelect;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;

/**
 * Restricts TV pairing requests to authenticated openHAB administrators.
 *
 * @author Ondrej Pecta - Initial contribution
 */
@Component(service = HisenseTVPairingResource.class)
@JaxrsResource
@JaxrsApplicationSelect("(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=" + RESTConstants.JAX_RS_NAME + ")")
@Path("hisensetv/pairing")
@RolesAllowed(Role.ADMIN)
@Produces(MediaType.TEXT_PLAIN)
@NonNullByDefault
public class HisenseTVPairingResource {
    private final ThingRegistry thingRegistry;

    @Activate
    public HisenseTVPairingResource(@Reference ThingRegistry thingRegistry) {
        this.thingRegistry = thingRegistry;
    }

    @POST
    @Path("{id}/request")
    public void requestPin(@PathParam("id") String id, @Suspended AsyncResponse response) {
        HisenseTVHandler handler = handler(id);
        if (handler == null) {
            response.resume(Response.status(Status.NOT_FOUND).build());
            return;
        }
        handler.requestPairing().whenComplete((sent, error) -> {
            response.resume(Boolean.TRUE.equals(sent) ? Response.noContent().build()
                    : Response.status(Status.CONFLICT)
                            .entity("The TV is disconnected or pairing is already in progress.").build());
        });
    }

    @POST
    @Path("{id}/pin")
    @Consumes(MediaType.TEXT_PLAIN)
    public void submitPin(@PathParam("id") String id, @Nullable String pin, @Suspended AsyncResponse response) {
        HisenseTVHandler handler = handler(id);
        if (handler == null) {
            response.resume(Response.status(Status.NOT_FOUND).build());
            return;
        }
        handler.submitPairingPin(pin == null ? "" : pin).whenComplete((result, error) -> {
            Response reply = error != null ? Response.serverError().build() : switch (result) {
                case ACCEPTED -> Response.noContent().build();
                case INVALID_PIN ->
                    Response.status(Status.BAD_REQUEST).entity("Enter the four digits shown on the TV.").build();
                case REJECTED ->
                    Response.status(Status.BAD_REQUEST).entity("The TV rejected the PIN. Request a new PIN.").build();
                case REQUEST_REQUIRED ->
                    Response.status(Status.CONFLICT).entity("Request a PIN before submitting it.").build();
                case NO_RESPONSE ->
                    Response.status(Status.GATEWAY_TIMEOUT).entity("The TV did not confirm the PIN.").build();
                case DISCONNECTED -> Response.status(Status.SERVICE_UNAVAILABLE).entity("The TV disconnected.").build();
            };
            response.resume(reply);
        });
    }

    private @Nullable HisenseTVHandler handler(String id) {
        if (!id.matches("[a-zA-Z0-9_-]+")) {
            return null;
        }
        Thing thing = thingRegistry.get(new ThingUID("hisensetv:tv:" + id));
        return thing != null && thing.getHandler() instanceof HisenseTVHandler handler ? handler : null;
    }
}
