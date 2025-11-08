package com.soulflamerange;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
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
		boolean isEquipped = plugin.isSoulflameHornsEquipped();
		
		if (!showRange || !isEquipped)
		{
			return null;
		}

		int range = plugin.getRange();
		if (range <= 0)
		{
			return null;
		}

		// Draw range square on the game world
		if (config.showRangeSquare() && client.getLocalPlayer() != null)
		{
			drawRangeSquare(graphics, range);
		}

		// Draw info panel
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

	private void drawRangeSquare(Graphics2D graphics, int range)
	{
		if (client.getLocalPlayer() == null)
		{
			return;
		}

		WorldPoint playerWorldPoint = client.getLocalPlayer().getWorldLocation();
		LocalPoint playerLocalPoint = LocalPoint.fromWorld(client, playerWorldPoint);

		if (playerLocalPoint == null)
		{
			return;
		}

		Color rangeColor = config.rangeColor();

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

			xPoints[i] = screenPoint.getX();
			yPoints[i] = screenPoint.getY();
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
}

