package viaprenetty.storage;

import com.viaversion.viaversion.api.connection.StorableObject;

public final class ClientWorld12 implements StorableObject {

    private int dimension;

    public int dimension() {
        return this.dimension;
    }

    public void dimension(final int dimension) {
        this.dimension = dimension;
    }

}
