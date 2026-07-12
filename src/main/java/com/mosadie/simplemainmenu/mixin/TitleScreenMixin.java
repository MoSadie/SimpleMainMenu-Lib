package com.mosadie.simplemainmenu.mixin;

import com.mojang.realmsclient.gui.screens.RealmsNotificationsScreen;
import com.mosadie.simplemainmenu.client.SimpleMainMenuLibClient;
import com.mosadie.simplemainmenu.duck.MultilineSplashTextRenderer;
import com.terraformersmc.modmenu.ModMenu;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.SafetyScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
    @Shadow @Nullable private SplashRenderer splash;
    @Shadow @Nullable private RealmsNotificationsScreen realmsNotificationsScreen;

    protected TitleScreenMixin(Component title) {
        super(title);
    }

    // "Lnet/minecraft/client/gui/screen/TitleScreen;isRealmsNotificationsGuiDisplayed()Z"
    @Inject(method = "realmsNotificationsEnabled", at = @At("HEAD"), cancellable = true)
    private void injectRealmNotification(CallbackInfoReturnable<Boolean> info) {
        info.setReturnValue(false);
    }

//    @Inject(method = "onDisplayed", at = @At("HEAD"), cancellable = true)
//    private void injectOnDisplayed(CallbackInfo ci) {
//        super.onDisplayed();
//        ci.cancel();
//    }

    @Inject(method = "init()V", at = @At("HEAD"))
    private void injectSplashText(CallbackInfo info) {
        if (splash == null) {
            Component[] splashes = SimpleMainMenuLibClient.getSplashText();
            // Still provide the first line or empty to avoid anything else that tries to use it crashing
            this.splash = new SplashRenderer(splashes.length == 0 ? Component.literal("") : splashes[0]);
            ((MultilineSplashTextRenderer) this.splash).smm_lib$setMultilineText(splashes);
        }
    }

    @Redirect(method = "init()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/TitleScreen;createNormalMenuOptions(II)I"))
    private int redirectInitWidgetsNormal(TitleScreen self, int y, int spacingY) {
        int buttonYMulti = 0;

        int buttonCount = 0;

        if (SimpleMainMenuLibClient.isSingleplayerVisible()) buttonCount++;
        if (SimpleMainMenuLibClient.isMultiplayerVisible()) buttonCount++;
        if (SimpleMainMenuLibClient.isQuickJoinVisible()) buttonCount++;

        if (buttonCount == 1) {
            buttonYMulti = 1;
        }

        if (SimpleMainMenuLibClient.isSingleplayerVisible()) {
            Button.Builder singlePlayerButtonWidgetBuilder = Button.builder(Component.translatable("menu.singleplayer"), (button -> {
                        Minecraft.getInstance().setScreenAndShow(new SelectWorldScreen((self)));
                    }))
                    .size(200, 20)
                    .pos(self.width / 2 - 100, y);

            this.addRenderableWidget(singlePlayerButtonWidgetBuilder.build());

            buttonYMulti++;
        }

        final Component disabledText = ((TitleScreenInvoker) this).invokeGetMultiplayerDisabledReason();
        boolean isDisabled = disabledText != null;

        Tooltip tooltip = Tooltip.create(disabledText);

        if (SimpleMainMenuLibClient.isQuickJoinVisible()) {

            Button.Builder quickJoinButtonWidgetBuilder = Button.builder(SimpleMainMenuLibClient.getButtonComponent(), (button) -> {
                SimpleMainMenuLibClient.onQuickJoinClick();
            }).pos(self.width / 2 - 100, y + (spacingY * buttonYMulti++)).size(200, 20);

            // Technically as of v2.0.0, this button can connect to things outside of multiplayer,
            // but since it's possible, still going to disable it.

            if (isDisabled) {
                quickJoinButtonWidgetBuilder.tooltip(tooltip);
            }

            Button joinServerButtonWidget = quickJoinButtonWidgetBuilder.build();
            joinServerButtonWidget.active = !isDisabled;
            this.addRenderableWidget(quickJoinButtonWidgetBuilder.build());
        }

        if (SimpleMainMenuLibClient.isMultiplayerVisible()) {
            Button.Builder multiplayerButtonWidgetBuilder = Button.builder(Component.translatable("menu.multiplayer"), button -> {
                Screen screen = Minecraft.getInstance().options.skipMultiplayerWarning ? new JoinMultiplayerScreen(self) : new SafetyScreen(self);
                Minecraft.getInstance().setScreenAndShow(screen);
            }).pos(self.width / 2 - 100, y + (spacingY * buttonYMulti++)).size(200, 20);

            if (isDisabled)
                multiplayerButtonWidgetBuilder.tooltip(tooltip);

            Button multiplayerButtonWidget = multiplayerButtonWidgetBuilder.build();
            multiplayerButtonWidget.active = !isDisabled;

            this.addRenderableWidget(multiplayerButtonWidget);
        }

        if (SimpleMainMenuLibClient.isModsVisible()) {
            Button.Builder modsButtonWidgetBuilder = Button.builder(ModMenu.createModsButtonText(true), button -> {
                Screen modsScreen = ModMenuApi.createModsScreen(Minecraft.getInstance().gui.screen());
                Minecraft.getInstance().setScreenAndShow(modsScreen);
            }).pos(self.width / 2 + 104, y + spacingY).size(50, 20);

            Button modsButtonWidget = modsButtonWidgetBuilder.build();

            this.addRenderableWidget(modsButtonWidget);
        }

        return y + (--buttonYMulti * spacingY);
    }
}
