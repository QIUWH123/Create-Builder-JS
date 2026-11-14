package net.quiwh.cbjs.kubejs;



/** Storage geometry is generated for its multiblock/CT renderer; item models remain customizable. */
final class StorageAssetOverrides {
    static void validate(CBJSBlockBuilder builder) {
        if (builder.model != null && !builder.model.isEmpty()) throw unsupported(builder, "model");
        if (builder.modelJson != null) throw unsupported(builder, "modelJson");
        if (builder.modelGenerator != null) throw unsupported(builder, "modelGenerator");
        if (builder.blockstateJson != null) throw unsupported(builder, "blockstateJson");
    }

    static IllegalArgumentException unsupported(CBJSBlockBuilder builder, String setting) {
        return new IllegalArgumentException("Storage block " + builder.id + " does not support world " + setting
                + " overrides; use .replaceTexture(role, path) for its generated multiblock, or .item(...) for an item model");
    }

    private StorageAssetOverrides() { }
}
