package com.smartcampus.ui.theme;

import javax.swing.BorderFactory;
import javax.swing.border.Border;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public final class DesignSystem {

    private DesignSystem() {}

    public static final Color MIDNIGHT_NAVY   = new Color(0x10, 0x17, 0x2A);
    public static final Color DEEP_NAVY       = new Color(0x17, 0x23, 0x3B);
    public static final Color NAVY_HOVER      = new Color(0x1E, 0x29, 0x3B);
    public static final Color NAVY_LIGHT      = new Color(0x33, 0x41, 0x55);

    public static final Color ELECTRIC_BLUE   = new Color(0x47, 0x76, 0xFF);
    public static final Color BLUE_HOVER      = new Color(0x35, 0x61, 0xE5);
    public static final Color VIOLET          = new Color(0x86, 0x65, 0xFF);
    public static final Color CYAN            = new Color(0x25, 0xC6, 0xDA);

    public static final Color BG_CANVAS       = new Color(0xF4, 0xF6, 0xFB);
    public static final Color CARD_BG         = Color.WHITE;
    public static final Color CARD_BORDER     = new Color(0xE2, 0xE8, 0xF0);
    public static final Color CARD_BORDER_SUBTLE = new Color(0xED, 0xF2, 0xF7);

    public static final Color TEXT_PRIMARY    = new Color(0x0F, 0x17, 0x2A);
    public static final Color TEXT_SECONDARY  = new Color(0x47, 0x55, 0x69);
    public static final Color TEXT_MUTED      = new Color(0x94, 0xA3, 0xB8);
    public static final Color TEXT_WHITE      = Color.WHITE;

    public static final Color SUCCESS         = new Color(0x16, 0xB9, 0x78);
    public static final Color SUCCESS_LIGHT   = new Color(0xDC, 0xFC, 0xE7);
    public static final Color SUCCESS_TEXT    = new Color(0x15, 0x80, 0x3D);

    public static final Color WARNING         = new Color(0xF5, 0xA6, 0x23);
    public static final Color WARNING_LIGHT   = new Color(0xFE, 0xF3, 0xC7);
    public static final Color WARNING_TEXT    = new Color(0xB4, 0x53, 0x09);

    public static final Color CRITICAL        = new Color(0xF0, 0x44, 0x52);
    public static final Color CRITICAL_LIGHT  = new Color(0xFF, 0xE4, 0xE6);
    public static final Color CRITICAL_TEXT   = new Color(0xBE, 0x12, 0x3C);

    public static final Color INFO_BLUE       = new Color(0x3B, 0x82, 0xF6);
    public static final Color INFO_LIGHT      = new Color(0xDB, 0xEA, 0xFE);
    public static final Color INFO_TEXT       = new Color(0x1D, 0x4E, 0xD8);

    public static final String FONT_FAMILY = "Segoe UI";

    public static final Font FONT_DISPLAY_LG = new Font(FONT_FAMILY, Font.BOLD, 24);
    public static final Font FONT_DISPLAY_MD = new Font(FONT_FAMILY, Font.BOLD, 20);
    public static final Font FONT_TITLE      = new Font(FONT_FAMILY, Font.BOLD, 16);
    public static final Font FONT_SUBTITLE   = new Font(FONT_FAMILY, Font.BOLD, 14);
    public static final Font FONT_CARD_LABEL = new Font(FONT_FAMILY, Font.BOLD, 11);
    public static final Font FONT_CARD_VALUE = new Font(FONT_FAMILY, Font.BOLD, 26);
    public static final Font FONT_BODY       = new Font(FONT_FAMILY, Font.PLAIN, 13);
    public static final Font FONT_BODY_BOLD  = new Font(FONT_FAMILY, Font.BOLD, 13);
    public static final Font FONT_SMALL      = new Font(FONT_FAMILY, Font.PLAIN, 12);
    public static final Font FONT_SMALL_BOLD = new Font(FONT_FAMILY, Font.BOLD, 12);
    public static final Font FONT_TINY       = new Font(FONT_FAMILY, Font.PLAIN, 11);
    public static final Font FONT_CODE       = new Font("Consolas", Font.PLAIN, 12);

    public static void applyQualityRendering(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    public static Border createCardBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CARD_BORDER, 1, true),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)
        );
    }

    public static Border createPaddedBorder(int top, int left, int bottom, int right) {
        return BorderFactory.createEmptyBorder(top, left, bottom, right);
    }
}
