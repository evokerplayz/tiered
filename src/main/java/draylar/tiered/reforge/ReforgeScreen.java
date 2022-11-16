package draylar.tiered.reforge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;

import draylar.tiered.api.TieredItemTags;
import draylar.tiered.config.ConfigInit;
import draylar.tiered.network.TieredClientPacket;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolItem;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerListener;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryEntry;

@Environment(EnvType.CLIENT)
public class ReforgeScreen extends HandledScreen<ReforgeScreenHandler> implements ScreenHandlerListener {

    public static final Identifier TEXTURE = new Identifier("tiered", "textures/gui/reforging_screen.png");
    public ReforgeScreen.ReforgeButton reforgeButton;

    public ReforgeScreen(ReforgeScreenHandler handler, PlayerInventory playerInventory, Text title) {
        super(handler, playerInventory, title);
        this.titleX = 60;
    }

    @Override
    protected void init() {
        super.init();
        ((ReforgeScreenHandler) this.handler).addListener(this);

        int i = (this.width - this.backgroundWidth) / 2;
        int j = (this.height - this.backgroundHeight) / 2;
        this.reforgeButton = (ReforgeScreen.ReforgeButton) this.addDrawableChild(new ReforgeScreen.ReforgeButton(i + 79, j + 56, (button) -> {
            if (button instanceof ReforgeScreen.ReforgeButton && !((ReforgeScreen.ReforgeButton) button).disabled)
                TieredClientPacket.writeC2SReforgePacket();
        }));
    }

    @Override
    public void removed() {
        super.removed();
        ((ReforgeScreenHandler) this.handler).removeListener(this);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        super.render(matrices, mouseX, mouseY, delta);
        RenderSystem.disableBlend();
        this.drawMouseoverTooltip(matrices, mouseX, mouseY);

        if (this.isPointWithinBounds(79, 56, 18, 18, (double) mouseX, (double) mouseY)) {
            ItemStack itemStack = this.getScreenHandler().getSlot(1).getStack();
            if (itemStack != null && !itemStack.isEmpty()) {
                List<ItemStack> list = new ArrayList<ItemStack>();
                if (itemStack.getItem() instanceof ToolItem)
                    list.addAll(Arrays.asList(((ToolItem) itemStack.getItem()).getMaterial().getRepairIngredient().getMatchingStacks()));
                else if (itemStack.getItem() instanceof ArmorItem)
                    list.addAll(Arrays.asList(((ArmorItem) itemStack.getItem()).getMaterial().getRepairIngredient().getMatchingStacks()));
                else {
                    Iterator<RegistryEntry<Item>> iterator = Registry.ITEM.getOrCreateEntryList(TieredItemTags.REFORGE_BASE_ITEM).iterator();
                    while (iterator.hasNext())
                        list.add(iterator.next().value().getDefaultStack());
                }
                ItemStack ingredient = this.getScreenHandler().getSlot(0).getStack();
                if (!list.isEmpty()) {
                    if (ingredient != null && !ingredient.isEmpty() && list.contains(ingredient)) {
                    } else {
                        List<Text> tooltip = new ArrayList<Text>();
                        tooltip.add(Text.translatable("screen.tiered.reforge_ingredient"));
                        for (int i = 0; i < list.size(); i++)
                            tooltip.add(list.get(i).getName());
                        this.renderTooltip(matrices, tooltip, mouseX, mouseY);
                    }
                }
            }
        }
    }

    @Override
    protected void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int i = (this.width - this.backgroundWidth) / 2;
        int j = (this.height - this.backgroundHeight) / 2;
        this.drawTexture(matrices, i, j, 0, 0, this.backgroundWidth, this.backgroundHeight);

        // anvil icon
        this.drawTexture(matrices, this.x + ConfigInit.CONFIG.xIconPosition, this.y - 21 + ConfigInit.CONFIG.yIconPosition, 24, 166, 24, 25);
        // reforge icon
        this.drawTexture(matrices, this.x + 25 + ConfigInit.CONFIG.xIconPosition, this.y - 23 + ConfigInit.CONFIG.yIconPosition, 72, 166, 24, 27);

        if (this.isPointWithinBounds(0 + ConfigInit.CONFIG.xIconPosition, -21 + ConfigInit.CONFIG.yIconPosition, 24, 21, (double) mouseX, (double) mouseY))
            this.renderTooltip(matrices, Text.translatable("container.repair"), mouseX, mouseY);
    }

    @Override
    public void onPropertyUpdate(ScreenHandler handler, int property, int value) {
    }

    @Override
    public void onSlotUpdate(ScreenHandler handler, int slotId, ItemStack stack) {
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.client != null && this.isPointWithinBounds(0 + ConfigInit.CONFIG.xIconPosition, -21 + ConfigInit.CONFIG.yIconPosition, 24, 21, (double) mouseX, (double) mouseY))
            TieredClientPacket.writeC2SScreenPacket((int) this.client.mouse.getX(), (int) this.client.mouse.getY(), false);

        return super.mouseClicked(mouseX, mouseY, button);
    }

    public class ReforgeButton extends ButtonWidget {
        private boolean disabled;

        public ReforgeButton(int x, int y, ButtonWidget.PressAction onPress) {
            super(x, y, 18, 18, ScreenTexts.EMPTY, onPress);
            this.disabled = true;
        }

        @Override
        public void renderButton(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, TEXTURE);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableDepthTest();
            int j = 176;
            if (this.disabled) {
                j += this.width * 2;
            } else if (this.isHovered()) {
                j += this.width;
            }
            this.drawTexture(matrices, this.x, this.y, j, 0, this.width, this.height);
        }

        public void setDisabled(boolean disable) {
            this.disabled = disable;
        }

    }

}
