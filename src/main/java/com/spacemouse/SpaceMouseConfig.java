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
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.ModifierlessKeybind;
import net.runelite.client.config.Range;

@ConfigGroup(SpaceMouseConfig.GROUP)
public interface SpaceMouseConfig extends Config
{
	String GROUP = "spacemouse";

	@ConfigSection(
		name = "Rotation",
		description = "Camera rotation keys and speeds",
		position = 0
	)
	String rotationSection = "rotation";

	@ConfigSection(
		name = "Zoom",
		description = "Camera zoom keys and speed",
		position = 1
	)
	String zoomSection = "zoom";

	@ConfigItem(
		keyName = "rotateLeft",
		name = "Rotate left",
		description = "Hold to rotate the camera left",
		section = rotationSection,
		position = 0
	)
	default ModifierlessKeybind rotateLeft()
	{
		return new ModifierlessKeybind(KeyEvent.VK_NUMPAD4, 0);
	}

	@ConfigItem(
		keyName = "rotateRight",
		name = "Rotate right",
		description = "Hold to rotate the camera right",
		section = rotationSection,
		position = 1
	)
	default ModifierlessKeybind rotateRight()
	{
		return new ModifierlessKeybind(KeyEvent.VK_NUMPAD6, 0);
	}

	@ConfigItem(
		keyName = "rotateUp",
		name = "Rotate up",
		description = "Hold to rotate the camera up",
		section = rotationSection,
		position = 2
	)
	default ModifierlessKeybind rotateUp()
	{
		return new ModifierlessKeybind(KeyEvent.VK_NUMPAD8, 0);
	}

	@ConfigItem(
		keyName = "rotateDown",
		name = "Rotate down",
		description = "Hold to rotate the camera down",
		section = rotationSection,
		position = 3
	)
	default ModifierlessKeybind rotateDown()
	{
		return new ModifierlessKeybind(KeyEvent.VK_NUMPAD2, 0);
	}

	@Range(min = 10, max = 720)
	@ConfigItem(
		keyName = "yawSpeed",
		name = "Yaw speed",
		description = "Degrees per second the camera turns while rotate left/right is held",
		section = rotationSection,
		position = 4
	)
	default int yawSpeed()
	{
		return 180;
	}

	@Range(min = 10, max = 360)
	@ConfigItem(
		keyName = "pitchSpeed",
		name = "Pitch speed",
		description = "Degrees per second the camera tilts while rotate up/down is held",
		section = rotationSection,
		position = 5
	)
	default int pitchSpeed()
	{
		return 90;
	}

	@ConfigItem(
		keyName = "invertYaw",
		name = "Invert yaw",
		description = "Swap the rotate left/right keys",
		section = rotationSection,
		position = 6
	)
	default boolean invertYaw()
	{
		return false;
	}

	@ConfigItem(
		keyName = "invertPitch",
		name = "Invert pitch",
		description = "Swap the rotate up/down keys",
		section = rotationSection,
		position = 7
	)
	default boolean invertPitch()
	{
		return false;
	}

	@ConfigItem(
		keyName = "zoomIn",
		name = "Zoom in",
		description = "Hold to zoom the camera in",
		section = zoomSection,
		position = 0
	)
	default ModifierlessKeybind zoomIn()
	{
		return new ModifierlessKeybind(KeyEvent.VK_ADD, 0);
	}

	@ConfigItem(
		keyName = "zoomOut",
		name = "Zoom out",
		description = "Hold to zoom the camera out",
		section = zoomSection,
		position = 1
	)
	default ModifierlessKeybind zoomOut()
	{
		return new ModifierlessKeybind(KeyEvent.VK_SUBTRACT, 0);
	}

	@Range(min = 25, max = 2000)
	@ConfigItem(
		keyName = "zoomSpeed",
		name = "Zoom speed",
		description = "Zoom units per second while a zoom key is held (full range is about 1500 units)",
		section = zoomSection,
		position = 2
	)
	default int zoomSpeed()
	{
		return 300;
	}

	@ConfigItem(
		keyName = "invertZoom",
		name = "Invert zoom",
		description = "Swap the zoom in/out keys",
		section = zoomSection,
		position = 3
	)
	default boolean invertZoom()
	{
		return false;
	}
}
