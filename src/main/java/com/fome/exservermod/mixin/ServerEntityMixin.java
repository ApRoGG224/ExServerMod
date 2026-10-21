package com.fome.exservermod.mixin;

import com.fome.exservermod.ExServerMod;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.Consumer;

@Mixin(ServerEntity.class)
public abstract class ServerEntityMixin {

    @Shadow
    @Final
    private Entity entity;

    @Shadow
    private List<SynchedEntityData.DataValue<?>> trackedDataValues;

    @Shadow
    @Final
    private ServerEntity.Synchronizer synchronizer;

    @ModifyVariable(
        method = "sendPairingData",
        at = @At("HEAD"),
        argsOnly = true
    )
    private Consumer<Packet<ClientGamePacketListener>> exservermod$wrapPairingDataConsumer(
            Consumer<Packet<ClientGamePacketListener>> originalConsumer,
            ServerPlayer trackingPlayer) {

        if (this.entity instanceof ServerPlayer player && ExServerMod.INSTANCE.getDisguiseManager().isDisguised(player.getUUID())) {
            EntityType<?> mobType = ExServerMod.INSTANCE.getDisguiseManager().getDisguise(player.getUUID());
            if (mobType != null) {
                return packet -> {
                    if (packet instanceof ClientboundAddEntityPacket addPacket && addPacket.getId() == player.getId()) {
                        originalConsumer.accept(new ClientboundAddEntityPacket(
                                player.getId(),
                                player.getUUID(),
                                addPacket.getX(),
                                addPacket.getY(),
                                addPacket.getZ(),
                                addPacket.getXRot(),
                                addPacket.getYRot(),
                                mobType,
                                addPacket.getData(),
                                addPacket.getMovement(),
                                (double) addPacket.getYHeadRot()
                        ));
                    } else if (packet instanceof ClientboundSetEntityDataPacket dataPacket && dataPacket.id() == player.getId()) {
                        List<SynchedEntityData.DataValue<?>> filtered = dataPacket.packedItems().stream()
                                .filter(item -> item.id() <= 13)
                                .toList();
                        if (!filtered.isEmpty()) {
                            originalConsumer.accept(new ClientboundSetEntityDataPacket(dataPacket.id(), filtered));
                        }
                    } else {
                        originalConsumer.accept(packet);
                    }
                };
            }
        }

        return originalConsumer;
    }

    @Inject(method = "sendDirtyEntityData", at = @At("HEAD"), cancellable = true)
    private void exservermod$filterDirtyEntityData(CallbackInfo ci) {
        if (this.entity instanceof ServerPlayer player && ExServerMod.INSTANCE.getDisguiseManager().isDisguised(player.getUUID())) {
            ci.cancel();
            List<SynchedEntityData.DataValue<?>> list = this.entity.getEntityData().packDirty();
            if (list != null) {
                this.trackedDataValues = this.entity.getEntityData().getNonDefaultValues();
                List<SynchedEntityData.DataValue<?>> filtered = list.stream()
                        .filter(item -> item.id() <= 13)
                        .toList();
                if (!filtered.isEmpty()) {
                    this.synchronizer.sendToTrackingPlayersAndSelf(new ClientboundSetEntityDataPacket(this.entity.getId(), filtered));
                }
            }
        }
    }
}
