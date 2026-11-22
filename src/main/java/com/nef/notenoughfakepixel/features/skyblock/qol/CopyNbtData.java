/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.qol;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.envcheck.registers.RegisterEvents;
import com.nef.notenoughfakepixel.utils.Logger;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByteArray;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;

import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;

/** Safe, opt-in NBT exporter. It is inert unless both debug switches are enabled. */
@RegisterEvents
public class CopyNbtData {
    private static final String INDENT = "    ";

    @SubscribeEvent
    public void onKeyInput(GuiScreenEvent.KeyboardInputEvent.Pre event) {
        if (!Config.feature.debug.debug || !Config.feature.debug.debugCopyNbt) return;
        if (!(event.gui instanceof GuiContainer) || Keyboard.getEventKey() != Keyboard.KEY_RCONTROL || !Keyboard.getEventKeyState()) return;
        Slot slot = ((GuiContainer) event.gui).getSlotUnderMouse();
        if (slot == null || !slot.getHasStack()) return;
        copy(slot.getStack().serializeNBT());
    }

    private void copy(NBTBase root) {
        if (root == null) return;
        StringBuilder out = new StringBuilder(4096);
        append(root, out, 0);
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            StringSelection value = new StringSelection(out.toString());
            clipboard.setContents(value, value);
            Logger.log("Copied item NBT to clipboard.");
        } catch (IllegalStateException | HeadlessException ex) {
            Logger.logError("Clipboard unavailable; item NBT was not copied.");
        }
    }

    private void append(NBTBase tag, StringBuilder out, int depth) {
        switch (tag.getId()) {
            case Constants.NBT.TAG_COMPOUND: appendCompound((NBTTagCompound) tag, out, depth); break;
            case Constants.NBT.TAG_LIST: appendList((NBTTagList) tag, out, depth); break;
            case Constants.NBT.TAG_BYTE_ARRAY: appendBytes((NBTTagByteArray) tag, out); break;
            case Constants.NBT.TAG_INT_ARRAY: appendInts((NBTTagIntArray) tag, out); break;
            default: out.append(tag.toString());
        }
    }

    private void appendCompound(NBTTagCompound compound, StringBuilder out, int depth) {
        out.append('{');
        if (compound.hasNoTags()) { out.append('}'); return; }
        out.append(System.lineSeparator());
        Iterator<String> it = compound.getKeySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            NBTBase child = compound.getTag(key);
            indent(out, depth + 1); out.append(key).append(": "); append(child, out, depth + 1);
            if (key.contains("backpack_data") && child instanceof NBTTagByteArray) decodeBackpack((NBTTagByteArray) child, out, depth + 1);
            if (it.hasNext()) out.append(',');
            out.append(System.lineSeparator());
        }
        indent(out, depth); out.append('}');
    }

    private void appendList(NBTTagList list, StringBuilder out, int depth) {
        out.append('[');
        for (int i = 0; i < list.tagCount(); i++) {
            if (i > 0) out.append(", ");
            append(list.get(i), out, depth);
        }
        out.append(']');
    }

    private void appendBytes(NBTTagByteArray tag, StringBuilder out) {
        byte[] values = tag.getByteArray(); out.append('[');
        for (int i = 0; i < values.length; i++) { if (i > 0) out.append(", "); out.append(values[i]); }
        out.append(']');
    }

    private void appendInts(NBTTagIntArray tag, StringBuilder out) {
        int[] values = tag.getIntArray(); out.append('[');
        for (int i = 0; i < values.length; i++) { if (i > 0) out.append(", "); out.append(values[i]); }
        out.append(']');
    }

    private void decodeBackpack(NBTTagByteArray tag, StringBuilder out, int depth) {
        try {
            NBTTagCompound decoded = CompressedStreamTools.readCompressed(new ByteArrayInputStream(tag.getByteArray()));
            out.append(',').append(System.lineSeparator()); indent(out, depth); out.append("(decoded): "); append(decoded, out, depth);
        } catch (IOException ignored) {
            out.append(" /* backpack payload could not be decompressed */");
        }
    }

    private void indent(StringBuilder out, int depth) { for (int i = 0; i < depth; i++) out.append(INDENT); }
}
