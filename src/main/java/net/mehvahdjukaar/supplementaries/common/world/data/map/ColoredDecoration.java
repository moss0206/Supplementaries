package net.mehvahdjukaar.supplementaries.common.world.data.map;

import net.mehvahdjukaar.selene.map.CustomDecoration;
import net.mehvahdjukaar.selene.map.CustomDecorationType;
import net.mehvahdjukaar.supplementaries.client.renderers.color.ColorHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;

public class ColoredDecoration extends CustomDecoration {
    private final DyeColor color;
    private final int value;

    public ColoredDecoration(CustomDecorationType<?, ?> type, byte x, byte y, byte rot, @Nullable Component displayName, @Nonnull DyeColor color) {
        super(type, x, y, rot, displayName);
        this.color = color;
        this.value = ColorHelper.pack(color.getTextureDiffuseColors());
    }
    public ColoredDecoration(CustomDecorationType<?,?> type, FriendlyByteBuf buffer){
        this(type, buffer.readByte(), buffer.readByte(), (byte)(buffer.readByte() & 15), buffer.readBoolean() ? buffer.readComponent() : null,
                DyeColor.byId(buffer.readByte()));
    }
    @Override
    public void saveToBuffer(FriendlyByteBuf buffer){
        super.saveToBuffer(buffer);
        buffer.writeByte(color.getId());
    }

    public DyeColor getColor() {
        return color;
    }

    public int getColorValue(){
        return value;
    }

}
