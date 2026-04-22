package com.smartcampus.resource;

import com.smartcampus.data.DataStore;
import com.smartcampus.exception.RoomNotEmptyException;
import com.smartcampus.model.ApiError;
import com.smartcampus.model.Room;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Room Resource — manages the /api/v1/rooms collection.
 *
 * Endpoints:
 * GET /api/v1/rooms - list all rooms
 * POST /api/v1/rooms - create a new room
 * GET /api/v1/rooms/{roomId} - get a specific room
 * DELETE /api/v1/rooms/{roomId} - delete a room (blocked if sensors are
 * assigned)
 */
@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    // GET /api/v1/rooms
    /**
     * Returns a list of all rooms currently registered in the system.
     */
    @GET
    public Response getAllRooms() {
        List<Room> roomList = new ArrayList<>(DataStore.getRooms().values());
        return Response.ok(roomList).build();
    }

    // POST /api/v1/rooms
    /**
     * Creates a new room.
     * - If no id is provided in the body, one is auto-generated (UUID).
     * - If id is provided and already exists, returns 409 Conflict.
     * - name and capacity are required fields.
     */
    @POST
    public Response createRoom(Room room) {
        // Validate required fields
        if (room == null) {
            ApiError err = new ApiError(400, "Bad Request", "Request body is missing or malformed.");
            return Response.status(Response.Status.BAD_REQUEST).entity(err).build();
        }
        if (room.getName() == null || room.getName().trim().isEmpty()) {
            ApiError err = new ApiError(400, "Bad Request", "Field 'name' is required.");
            return Response.status(Response.Status.BAD_REQUEST).entity(err).build();
        }
        if (room.getCapacity() <= 0) {
            ApiError err = new ApiError(400, "Bad Request", "Field 'capacity' must be a positive integer.");
            return Response.status(Response.Status.BAD_REQUEST).entity(err).build();
        }

        // Auto-generate ID if not provided
        if (room.getId() == null || room.getId().trim().isEmpty()) {
            room.setId(UUID.randomUUID().toString());
        }

        // Reject duplicate IDs
        if (DataStore.roomExists(room.getId())) {
            ApiError err = new ApiError(409, "Conflict",
                    "A room with id '" + room.getId() + "' already exists.");
            return Response.status(Response.Status.CONFLICT).entity(err).build();
        }

        // Ensure sensorIds list is always initialized
        if (room.getSensorIds() == null) {
            room.setSensorIds(new ArrayList<>());
        }

        DataStore.putRoom(room);

        // 201 Created with the newly created room in the body
        return Response.status(Response.Status.CREATED).entity(room).build();
    }

    // GET /api/v1/rooms/{roomId}
    /**
     * Returns the full details of a single room by its ID.
     * Returns 404 if no room with that ID exists.
     */
    @GET
    @Path("/{roomId}")
    public Response getRoomById(@PathParam("roomId") String roomId) {
        Room room = DataStore.getRoom(roomId);

        if (room == null) {
            ApiError err = new ApiError(404, "Not Found",
                    "No room found with id '" + roomId + "'.");
            return Response.status(Response.Status.NOT_FOUND).entity(err).build();
        }

        return Response.ok(room).build();
    }

    // DELETE /api/v1/rooms/{roomId}
    /**
     * Deletes a room from the system.
     *
     * Business Logic Constraint:
     * A room cannot be deleted if it still has sensors assigned to it.
     * Throws RoomNotEmptyException (→ 409 Conflict) if sensors are present.
     *
     * Idempotency note:
     * The first DELETE succeeds (200 OK).
     * Subsequent DELETE calls on the same ID return 404 Not Found,
     * meaning this operation is NOT strictly idempotent (per REST spec it ideally
     * would be),
     * but in this implementation we deliberately retuern 404 on repeat calls to
     * give
     * the client clear feedback that the resource no longer exists.
     */
    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = DataStore.getRoom(roomId);

        // 404 if room doesn't exist
        if (room == null) {
            ApiError err = new ApiError(404, "Not Found",
                    "No room found with id '" + roomId + "'. It may have already been deleted.");
            return Response.status(Response.Status.NOT_FOUND).entity(err).build();
        }

        // 409 if room still has sensors — throw custom exception (mapped by
        // RoomNotEmptyExceptionMapper)
        if (room.getSensorIds() != null && !room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException(roomId);
        }

        // All clear — remove the room
        DataStore.removeRoom(roomId);

        // 200 OK with confirmation message
        return Response.ok(new ApiError(200, "OK",
                "Room '" + roomId + "' has been successfully deleted.")).build();
    }
}
