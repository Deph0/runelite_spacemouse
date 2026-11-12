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

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ScriptID;
import net.runelite.api.events.BeforeRender;
import net.runelite.api.events.FocusChanged;
import net.runelite.api.gameval.VarClientID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

/**
 * Camera control plugin for 3D mice.
 * <p>
 * The device itself is never opened: the vendor driver (3DxWare on
 * Windows/macOS, spacerunner/spacenavd on Linux) is configured to emit the
 * keys bound below for each axis, and this plugin turns "key is held" into a
 * constant, configurable camera rotation rate.
 * <p>
 * This keeps the plugin entirely inside the public RuneLite API: no native
 * libraries, no reflection, no subprocesses, no synthetic AWT events, and no
 * background threads.
 */
@PluginDescriptor(
	name = "Space Mouse",
	description = "Rotate and zoom the camera with a 3D mouse, using keys mapped by your device driver",
	tags = {"camera", "input", "accessibility"}
)
@Slf4j
public class SpaceMousePlugin extends Plugin
{
	/**
	 * Camera yaw/pitch are in JAU14: 2^14 units per full rotation.
	 */
	private static final int FULL_ROTATION = 16384;
	private static final double UNITS_PER_DEGREE = FULL_ROTATION / 360.0;

	/**
	 * Camera pitch is limited by the client to 1024..3064 (22.5..67.3 degrees).
	 */
	private static final int PITCH_MIN = 1024;
	private static final int PITCH_MAX = 3064;

	/**
	 * Upper bound on the frame delta, so a stalled frame cannot slam the camera.
	 */
	private static final double MAX_FRAME_SECONDS = 0.1;

	/**
	 * Zoom variable values are roughly -272..1300; used when the client's limit
	 * variables are not populated (they are only filled in by the Camera plugin).
	 */
	private static final int DEFAULT_ZOOM_MIN = -272;
	private static final int DEFAULT_ZOOM_MAX = 1300;

	/**
	 * Minimum gap between zoom scripts, so a high zoom speed does not call into
	 * the client every frame.
	 */
	private static final long ZOOM_SCRIPT_INTERVAL_NANOS = 33_000_000L;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private KeyManager keyManager;

	@Inject
	private SpaceMouseConfig config;

	@Inject
	private SpaceMouseKeyListener keyListener;

	private volatile boolean gameFocused;
	private long lastFrameNanos;

	// Sub-unit remainders, so slow speeds still add up to whole camera units.
	private double yawRemainder;
	private double pitchRemainder;
	private double zoomRemainder;

	// Zoom level the plugin is currently driving towards, or -1 when unknown.
	private int zoomTarget = -1;
	private long lastZoomScriptNanos;

	@Override
	protected void startUp()
	{
		// FocusChanged is only fired on a change, so pick up the current state.
		gameFocused = client.getCanvas().isFocusOwner();
		keyManager.registerKeyListener(keyListener);
	}

	@Provides
	SpaceMouseConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(SpaceMouseConfig.class);
	}

	@Override
	protected void shutDown()
	{
		keyListener.reset();
		keyManager.unregisterKeyListener(keyListener);
		resetState();
	}

	@Subscribe
	public void onFocusChanged(FocusChanged event)
	{
		gameFocused = event.isFocused();
		if (!gameFocused)
		{
			keyListener.reset();
			resetState();
		}
	}

	@Subscribe
	public void onBeforeRender(BeforeRender event)
	{
		if (!isActive())
		{
			resetState();
			return;
		}

		long now = System.nanoTime();
		if (lastFrameNanos == 0L)
		{
			lastFrameNanos = now;
			return;
		}

		double deltaSeconds = (now - lastFrameNanos) / 1e9;
		lastFrameNanos = now;

		// Clamp so a stalled or reversed frame cannot slam the camera.
		deltaSeconds = Math.max(0, Math.min(deltaSeconds, MAX_FRAME_SECONDS));

		rotate(deltaSeconds);
		zoom(deltaSeconds);
	}

	/**
	 * @return true when the player is in game, the client window is focused and
	 * no text input is capturing keys.
	 */
	boolean isActive()
	{
		if (!gameFocused || client.getGameState() != GameState.LOGGED_IN)
		{
			return false;
		}

		// Another widget is taking text input (bank search, world map search, name change, ...).
		if (client.getFocusedInputFieldWidget() != null)
		{
			return false;
		}

		// Chatbox input holds whatever the player has typed so far.
		String chatInput = client.getVarcStrValue(VarClientID.CHATINPUT);
		return chatInput == null || chatInput.isEmpty();
	}

	private void rotate(double deltaSeconds)
	{
		int yawDirection = 0;
		if (keyListener.isHeld(config.rotateLeft()))
		{
			yawDirection++;
		}
		if (keyListener.isHeld(config.rotateRight()))
		{
			yawDirection--;
		}
		if (config.invertYaw())
		{
			yawDirection = -yawDirection;
		}

		int pitchDirection = 0;
		if (keyListener.isHeld(config.rotateUp()))
		{
			pitchDirection++;
		}
		if (keyListener.isHeld(config.rotateDown()))
		{
			pitchDirection--;
		}
		if (config.invertPitch())
		{
			pitchDirection = -pitchDirection;
		}

		if (yawDirection == 0)
		{
			yawRemainder = 0;
		}
		else
		{
			yawRemainder += yawDirection * config.yawSpeed() * UNITS_PER_DEGREE * deltaSeconds;
			int yawDelta = (int) yawRemainder;
			if (yawDelta != 0)
			{
				yawRemainder -= yawDelta;
				client.setCameraYawTarget(Math.floorMod(client.getCameraYawTarget() + yawDelta, FULL_ROTATION));
			}
		}

		if (pitchDirection == 0)
		{
			pitchRemainder = 0;
		}
		else
		{
			pitchRemainder += pitchDirection * config.pitchSpeed() * UNITS_PER_DEGREE * deltaSeconds;
			int pitchDelta = (int) pitchRemainder;
			if (pitchDelta != 0)
			{
				pitchRemainder -= pitchDelta;
				int pitch = client.getCameraPitchTarget() + pitchDelta;
				client.setCameraPitchTarget(Math.max(PITCH_MIN, Math.min(PITCH_MAX, pitch)));
			}
		}
	}

	private void zoom(double deltaSeconds)
	{
		int direction = 0;
		if (keyListener.isHeld(config.zoomIn()))
		{
			direction++;
		}
		if (keyListener.isHeld(config.zoomOut()))
		{
			direction--;
		}
		if (config.invertZoom())
		{
			direction = -direction;
		}

		if (direction == 0)
		{
			zoomRemainder = 0;
			zoomTarget = -1;
			lastZoomScriptNanos = 0L;
			return;
		}

		// Re-sync with the live zoom level whenever a new zoom starts, so manual
		// scrolling in between does not make the plugin jump back to a stale value.
		if (zoomTarget < 0)
		{
			zoomTarget = currentZoom();
			lastZoomScriptNanos = 0L;
		}

		zoomRemainder += direction * config.zoomSpeed() * deltaSeconds;
		int steps = (int) zoomRemainder;
		if (steps == 0)
		{
			return;
		}

		long now = System.nanoTime();
		if (lastZoomScriptNanos != 0L && now - lastZoomScriptNanos < ZOOM_SCRIPT_INTERVAL_NANOS)
		{
			return;
		}
		zoomRemainder -= steps;
		lastZoomScriptNanos = now;

		int min = minZoom();
		int max = maxZoom();
		if (max <= min)
		{
			min = DEFAULT_ZOOM_MIN;
			max = DEFAULT_ZOOM_MAX;
		}

		int target = Math.max(min, Math.min(max, zoomTarget + steps));
		if (target == zoomTarget)
		{
			return;
		}

		int previous = zoomTarget;
		zoomTarget = target;
		clientThread.invoke(() -> client.runScript(ScriptID.CAMERA_DO_ZOOM, target, target));
		log.debug("Zoom {} -> {} (range {}..{})", previous, target, min, max);
	}

	private int currentZoom()
	{
		return client.getVarcIntValue(client.isResized()
			? VarClientID.CAMERA_ZOOM_BIG
			: VarClientID.CAMERA_ZOOM_SMALL);
	}

	private int minZoom()
	{
		return client.getVarcIntValue(client.isResized()
			? VarClientID.CAMERA_ZOOM_BIG_MIN
			: VarClientID.CAMERA_ZOOM_SMALL_MIN);
	}

	private int maxZoom()
	{
		return client.getVarcIntValue(client.isResized()
			? VarClientID.CAMERA_ZOOM_BIG_MAX
			: VarClientID.CAMERA_ZOOM_SMALL_MAX);
	}

	private void resetState()
	{
		lastFrameNanos = 0L;
		yawRemainder = 0;
		pitchRemainder = 0;
		zoomRemainder = 0;
		zoomTarget = -1;
		lastZoomScriptNanos = 0L;
	}
}
