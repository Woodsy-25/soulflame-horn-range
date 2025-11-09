package com.soulflamerange;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

@Slf4j
class SoulflameRangeOverlay extends Overlay
{
	private final Client client;
	private final SoulflameRangePlugin plugin;
	private final SoulflameRangeConfig config;
	private final PanelComponent panelComponent = new PanelComponent();

	@Inject
	private SoulflameRangeOverlay(Client client, SoulflameRangePlugin plugin, SoulflameRangeConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
		setPriority(Overlay.PRIORITY_HIGH);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		boolean showRange = config.showRange();
		boolean showRangeSquare = config.showRangeSquare();
		boolean isEquipped = plugin.isSoulflameHornsEquipped();
		
		if (!isEquipped)
		{
			// Still draw other players' ranges even if local player doesn't have horn
			if (config.showOtherPlayers() && showRangeSquare)
			{
				drawOtherPlayersRanges(graphics);
			}
			return null;
		}

		int range = plugin.getRange();
		if (range <= 0)
		{
			return null;
		}

		// Draw range square on the game world for local player (controlled by showRangeSquare)
		if (showRangeSquare && client.getLocalPlayer() != null)
		{
			drawRangeSquare(graphics, client.getLocalPlayer().getWorldLocation(), range, config.rangeColor());
		}

		// Draw other players' ranges (controlled by showOtherPlayers and showRangeSquare)
		if (config.showOtherPlayers() && showRangeSquare)
		{
			drawOtherPlayersRanges(graphics);
		}

		// Draw info panel (controlled by showRange)
		if (!showRange)
		{
			return null;
		}

		panelComponent.getChildren().clear();
		panelComponent.setBackgroundColor(new Color(0, 0, 0, 150));

		panelComponent.getChildren().add(TitleComponent.builder()
			.text("Soulflame Horn")
			.color(Color.ORANGE)
			.build());

		panelComponent.getChildren().add(TitleComponent.builder()
			.text("Range: " + range + " tiles")
			.color(Color.WHITE)
			.build());

		return panelComponent.render(graphics);
	}

	private void drawRangeSquare(Graphics2D graphics, WorldPoint playerWorldPoint, int range, Color rangeColor)
	{
		if (playerWorldPoint == null)
		{
			return;
		}

		// Draw a square around the player showing the range
		// The range is the radius, so we need to draw from -range to +range
		// Get the four corners of the square
		WorldPoint[] corners = new WorldPoint[4];
		corners[0] = new WorldPoint(playerWorldPoint.getX() - range, playerWorldPoint.getY() - range, playerWorldPoint.getPlane());
		corners[1] = new WorldPoint(playerWorldPoint.getX() + range, playerWorldPoint.getY() - range, playerWorldPoint.getPlane());
		corners[2] = new WorldPoint(playerWorldPoint.getX() + range, playerWorldPoint.getY() + range, playerWorldPoint.getPlane());
		corners[3] = new WorldPoint(playerWorldPoint.getX() - range, playerWorldPoint.getY() + range, playerWorldPoint.getPlane());

		// Convert world points to screen coordinates and draw lines
		int[] xPoints = new int[4];
		int[] yPoints = new int[4];
		boolean allVisible = true;

		// Get the center point for calculating offsets
		LocalPoint centerLocal = LocalPoint.fromWorld(client, playerWorldPoint);
		if (centerLocal == null)
		{
			return;
		}
		net.runelite.api.Point centerScreen = Perspective.localToCanvas(client, centerLocal, playerWorldPoint.getPlane());
		if (centerScreen == null)
		{
			return;
		}

		for (int i = 0; i < 4; i++)
		{
			LocalPoint localPoint = LocalPoint.fromWorld(client, corners[i]);
			if (localPoint == null)
			{
				allVisible = false;
				break;
			}

			net.runelite.api.Point screenPoint = Perspective.localToCanvas(client, localPoint, playerWorldPoint.getPlane());
			if (screenPoint == null)
			{
				allVisible = false;
				break;
			}

			// Extend the border outward by offsetting the screen coordinates
			// Calculate direction from center to corner and extend it
			int dx = screenPoint.getX() - centerScreen.getX();
			int dy = screenPoint.getY() - centerScreen.getY();
			// Normalize and extend by approximately half a tile (64 pixels at typical zoom)
			double length = Math.sqrt(dx * dx + dy * dy);
			if (length > 0)
			{
				int offsetPixels = 32; // Approximate half-tile offset
				xPoints[i] = screenPoint.getX() + (int)((dx / length) * offsetPixels);
				yPoints[i] = screenPoint.getY() + (int)((dy / length) * offsetPixels);
			}
			else
			{
				xPoints[i] = screenPoint.getX();
				yPoints[i] = screenPoint.getY();
			}
		}

		if (allVisible)
		{
			// Draw outline only with transparency
			Polygon square = new Polygon(xPoints, yPoints, 4);
			graphics.setColor(new Color(rangeColor.getRed(), rangeColor.getGreen(), rangeColor.getBlue(), 150));
			graphics.setStroke(new java.awt.BasicStroke(2));
			graphics.drawPolygon(square);
		}
	}

	private void drawOtherPlayersRanges(Graphics2D graphics)
	{
		// Check all players in the area
		for (Player player : client.getPlayers())
		{
			if (player == null || player == client.getLocalPlayer())
			{
				continue;
			}

			// Check if player has Soulflame horn equipped by checking their appearance
			if (hasSoulflameHorn(player))
			{
				WorldPoint playerWorldPoint = player.getWorldLocation();
				if (playerWorldPoint == null)
				{
					continue;
				}

				// We can't get other players' varbit values directly
				// Use the configured radius for other players
				int otherPlayerRange = config.otherPlayersRadius();
				
				// Clamp to valid range (0-3)
				if (otherPlayerRange < 0)
				{
					otherPlayerRange = 0;
				}
				else if (otherPlayerRange > 3)
				{
					otherPlayerRange = 3;
				}
				
				// Only draw if range > 0
				if (otherPlayerRange > 0)
				{
					drawRangeSquare(graphics, playerWorldPoint, otherPlayerRange, config.otherPlayersColor());
				}
			}
		}
	}

	private boolean hasSoulflameHorn(Player player)
	{
		if (player == null)
		{
			return false;
		}

		// Check the player's composition for equipment
		net.runelite.api.PlayerComposition composition = player.getPlayerComposition();
		if (composition == null)
		{
			return false;
		}

		// Check the weapon slot for Soulflame horn
		// Equipment slots: 3 = weapon
		int weaponId = composition.getEquipmentId(net.runelite.api.kit.KitType.WEAPON);
		return weaponId == 30759; // SOULFLAME_HORNS item ID
	}
}

