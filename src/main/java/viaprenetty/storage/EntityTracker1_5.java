package viaprenetty.storage;

import com.viaversion.viaversion.api.connection.StorableObject;

import java.util.HashMap;
import java.util.Map;

public final class EntityTracker1_5 implements StorableObject {

    public static final int PLAYER = -1;
    public static final int OBJECT_OFFSET = 1000;

    private final Map<Integer, Integer> types = new HashMap<>();
    private int playerId = -1;
    private int vehicleId = -1;

    public void track(final int entityId, final int type) {
        this.types.put(entityId, type);
    }

    public Integer type(final int entityId) {
        if (entityId == this.playerId) {
            return PLAYER;
        }
        return this.types.get(entityId);
    }

    public void remove(final int entityId) {
        this.types.remove(entityId);
        if (entityId == this.vehicleId) {
            this.vehicleId = -1;
        }
    }

    public void clear() {
        this.types.clear();
        this.vehicleId = -1;
    }

    public int playerId() {
        return this.playerId;
    }

    public void setPlayerId(final int playerId) {
        this.playerId = playerId;
    }

    public int vehicleId() {
        return this.vehicleId;
    }

    public void setVehicleId(final int vehicleId) {
        this.vehicleId = vehicleId;
    }

}
