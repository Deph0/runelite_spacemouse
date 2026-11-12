/*
 * Copyright (c) 2026, NeusAap
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.spacemouse;

import java.awt.event.KeyEvent;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.config.ModifierlessKeybind;
import net.runelite.client.input.KeyListener;

/**
 * Tracks which of the plugin's bound keys are currently held down.
 * <p>
 * Only key state is tracked here; the camera is driven from
 * {@link SpaceMousePlugin#onBeforeRender}, so a key that is held keeps the
 * camera moving at a constant rate rather than relying on OS key repeat.
 */
@Singleton
class SpaceMouseKeyListener implements KeyListener
{
	/**
	 * Written from the AWT event thread and read from the client thread, so it
	 * is a concurrent set. Keys are only added and removed from AWT events.
	 */
	private final Set<Integer> heldKeys = ConcurrentHashMap.newKeySet();

	@Inject
	private SpaceMousePlugin plugin;

	@Inject
	private SpaceMouseConfig config;

	@Override
	public void keyTyped(KeyEvent e)
	{
	}

	@Override
	public void keyPressed(KeyEvent e)
	{
		if (!plugin.isActive() || !isBoundKey(e.getKeyCode()))
		{
			return;
		}

		heldKeys.add(e.getKeyCode());
		// The keys are owned by this plugin while the game is in focus, so the
		// client does not also act on them (e.g. the built in arrow key camera).
		e.consume();
	}

	@Override
	public void keyReleased(KeyEvent e)
	{
		if (heldKeys.remove(e.getKeyCode()))
		{
			e.consume();
		}
	}

	boolean isHeld(ModifierlessKeybind keybind)
	{
		int keyCode = keybind.getKeyCode();
		return keyCode != KeyEvent.VK_UNDEFINED && heldKeys.contains(keyCode);
	}

	void reset()
	{
		heldKeys.clear();
	}

	private boolean isBoundKey(int keyCode)
	{
		return isBound(config.rotateLeft(), keyCode)
			|| isBound(config.rotateRight(), keyCode)
			|| isBound(config.rotateUp(), keyCode)
			|| isBound(config.rotateDown(), keyCode)
			|| isBound(config.zoomIn(), keyCode)
			|| isBound(config.zoomOut(), keyCode);
	}

	private static boolean isBound(ModifierlessKeybind keybind, int keyCode)
	{
		return keybind.getKeyCode() != KeyEvent.VK_UNDEFINED && keybind.getKeyCode() == keyCode;
	}
}
