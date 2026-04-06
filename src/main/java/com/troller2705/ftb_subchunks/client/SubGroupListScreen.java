package com.troller2705.ftb_subchunks.client;

import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.SimpleTextButton;
import dev.ftb.mods.ftblibrary.ui.misc.ButtonListBaseScreen;
import dev.ftb.mods.ftblibrary.ui.input.Key;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import com.troller2705.ftb_subchunks.data.SubZone;

public class SubGroupListScreen extends ButtonListBaseScreen {
    private final Runnable returnToMap;

    public SubGroupListScreen(Runnable returnToMap) {
        // Empty constructor as per your .class file
        super();
        this.returnToMap = returnToMap;
        this.setTitle(Component.literal("Sub-Group Brushes"));
        this.setHasSearchBox(true);
    }

    @Override
    public void addButtons(Panel panel) {
        panel.add(new SimpleTextButton(panel, Component.literal("Create New Sub-Group..."), Icons.ADD) {
            @Override
            public void onClicked(dev.ftb.mods.ftblibrary.ui.input.MouseButton button) {
                SubZone newZone = new SubZone("New Sub-Group", BlockPos.ZERO, BlockPos.ZERO);
                SubGroupClientUI.openBrushEditor(newZone, SubGroupListScreen.this, returnToMap);
            }
        });

        for (SubZone zone : SubGroupClientUI.BRUSH_PALETTE.values()) {
            panel.add(new SimpleTextButton(panel, Component.literal(zone.getName()), Icons.SETTINGS) {
                @Override
                public void onClicked(dev.ftb.mods.ftblibrary.ui.input.MouseButton button) {
                    SubGroupClientUI.openBrushEditor(zone, SubGroupListScreen.this, returnToMap);
                }
            });
        }
    }

    @Override
    public boolean keyPressed(Key key) {
        // Catching the Escape key using FTB's internal Key object
        if (key.esc()) {
            this.closeGui();
            returnToMap.run();
            return true;
        }
        return super.keyPressed(key);
    }
}