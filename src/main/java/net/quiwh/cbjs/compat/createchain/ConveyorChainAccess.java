package net.quiwh.cbjs.compat.createchain;

/** Implemented on native Create conveyors by the mixin, without registering extra blocks. */
public interface ConveyorChainAccess {
    ConveyorChainData cbjs$chains();
    ConveyorChainData cbjs$existingChains();
}
