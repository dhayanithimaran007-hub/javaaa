package com.smartcampus.ui.components;

import com.smartcampus.ui.theme.DesignSystem;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

public final class VectorIcons {

    public enum Kind {
        DASHBOARD,
        CLIPBOARD,
        WRENCH,
        GRADUATION,
        USERS,
        USER,
        TREND,
        SHIELD,
        DATABASE,
        PLUS,
        MENU,
        LOGOUT,
        REFRESH,
        ALERT,
        BOLT,
        CHECK,
        CROSS,
        INFO,
        CLOCK,
        STATUS_DOT,
        CIRCLE_OUTLINE,
        CHEVRON_LEFT,
        CHEVRON_RIGHT,
        EYE,
        EYE_OFF,
        STAR
    }

    private VectorIcons() {}

    public static Icon of(Kind kind, int size) {
        return new VectorIcon(kind, size, null);
    }

    public static Icon of(Kind kind, Color color, int size) {
        return new VectorIcon(kind, size, color);
    }

    public static Icon statusDot(Color color, int size) { return of(Kind.STATUS_DOT, color, size); }
    public static Icon circleOutline(Color color, int size) { return of(Kind.CIRCLE_OUTLINE, color, size); }
    public static Icon check(Color color, int size) { return of(Kind.CHECK, color, size); }
    public static Icon cross(Color color, int size) { return of(Kind.CROSS, color, size); }
    public static Icon info(Color color, int size) { return of(Kind.INFO, color, size); }
    public static Icon warning(Color color, int size) { return of(Kind.ALERT, color, size); }
    public static Icon refresh(Color color, int size) { return of(Kind.REFRESH, color, size); }
    public static Icon bolt(Color color, int size) { return of(Kind.BOLT, color, size); }
    public static Icon chart(Color color, int size) { return of(Kind.TREND, color, size); }
    public static Icon database(Color color, int size) { return of(Kind.DATABASE, color, size); }
    public static Icon chevronLeft(int size) { return of(Kind.CHEVRON_LEFT, size); }
    public static Icon chevronRight(int size) { return of(Kind.CHEVRON_RIGHT, size); }
    public static Icon eye(Color color, int size) { return of(Kind.EYE, color, size); }
    public static Icon eyeOff(Color color, int size) { return of(Kind.EYE_OFF, color, size); }
    public static Icon star(Color color, int size) { return of(Kind.STAR, color, size); }

    private static final class VectorIcon implements Icon {
        private final Kind kind;
        private final int size;
        private final Color fixedColor;

        VectorIcon(Kind kind, int size, Color fixedColor) {
            this.kind = kind;
            this.size = size;
            this.fixedColor = fixedColor;
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            DesignSystem.applyQualityRendering(g2);
            g2.translate(x, y);

            double s = size / 16.0;
            g2.scale(s, s);

            Color color = fixedColor != null
                    ? fixedColor
                    : (c != null && c.getForeground() != null ? c.getForeground() : DesignSystem.TEXT_PRIMARY);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            switch (kind) {
                case DASHBOARD -> {
                    g2.draw(new RoundRectangle2D.Double(2, 2, 5, 5, 1.5, 1.5));
                    g2.draw(new RoundRectangle2D.Double(9, 2, 5, 5, 1.5, 1.5));
                    g2.draw(new RoundRectangle2D.Double(2, 9, 5, 5, 1.5, 1.5));
                    g2.draw(new RoundRectangle2D.Double(9, 9, 5, 5, 1.5, 1.5));
                }
                case CLIPBOARD -> {
                    g2.draw(new RoundRectangle2D.Double(3, 3, 10, 11.5, 2, 2));
                    g2.draw(new RoundRectangle2D.Double(5.8, 1.5, 4.4, 2.5, 1, 1));
                    g2.draw(new Line2D.Double(5.5, 7.2, 10.5, 7.2));
                    g2.draw(new Line2D.Double(5.5, 10.5, 9.2, 10.5));
                }
                case WRENCH -> {
                    Path2D p = new Path2D.Double();
                    p.moveTo(10.8, 2.5);
                    p.curveTo(9.0, 2.5, 7.8, 4.0, 8.2, 5.8);
                    p.lineTo(3.2, 10.8);
                    p.curveTo(2.4, 11.6, 2.4, 12.8, 3.2, 13.5);
                    p.curveTo(3.9, 14.2, 5.1, 14.2, 5.8, 13.5);
                    p.lineTo(10.8, 8.5);
                    p.curveTo(12.6, 8.9, 14.1, 7.7, 14.1, 5.8);
                    p.lineTo(12.0, 6.4);
                    p.lineTo(10.3, 4.7);
                    p.lineTo(10.8, 2.5);
                    g2.draw(p);
                }
                case GRADUATION -> {
                    Path2D cap = new Path2D.Double();
                    cap.moveTo(8, 2.8);
                    cap.lineTo(14.5, 6.0);
                    cap.lineTo(8, 9.2);
                    cap.lineTo(1.5, 6.0);
                    cap.closePath();
                    g2.draw(cap);
                    Path2D base = new Path2D.Double();
                    base.moveTo(4.5, 8.0);
                    base.lineTo(4.5, 11.4);
                    base.quadTo(8, 13.4, 11.5, 11.4);
                    base.lineTo(11.5, 8.0);
                    g2.draw(base);
                    g2.draw(new Line2D.Double(13.5, 6.5, 13.5, 11.5));
                }
                case USERS -> {
                    g2.draw(new Ellipse2D.Double(3.8, 2.8, 4.4, 4.4));
                    Path2D a1 = new Path2D.Double();
                    a1.moveTo(1.8, 13.5);
                    a1.quadTo(6.0, 9.2, 10.2, 13.5);
                    g2.draw(a1);
                    g2.draw(new Ellipse2D.Double(9.5, 3.6, 3.6, 3.6));
                    Path2D a2 = new Path2D.Double();
                    a2.moveTo(9.8, 10.5);
                    a2.quadTo(12.2, 9.6, 14.2, 13.5);
                    g2.draw(a2);
                }
                case USER -> {
                    g2.draw(new Ellipse2D.Double(5.4, 2.6, 5.2, 5.2));
                    Path2D shoulders = new Path2D.Double();
                    shoulders.moveTo(2.6, 13.6);
                    shoulders.quadTo(8.0, 9.0, 13.4, 13.6);
                    g2.draw(shoulders);
                }
                case TREND -> {
                    g2.draw(new Line2D.Double(2.5, 2.5, 2.5, 13.5));
                    g2.draw(new Line2D.Double(2.5, 13.5, 13.5, 13.5));
                    Path2D line = new Path2D.Double();
                    line.moveTo(4.0, 10.8);
                    line.lineTo(6.8, 7.8);
                    line.lineTo(9.4, 9.4);
                    line.lineTo(13.0, 4.4);
                    g2.draw(line);
                    g2.draw(new Line2D.Double(10.6, 4.4, 13.0, 4.4));
                    g2.draw(new Line2D.Double(13.0, 4.4, 13.0, 6.8));
                }
                case SHIELD -> {
                    Path2D shield = new Path2D.Double();
                    shield.moveTo(8.0, 1.8);
                    shield.lineTo(13.2, 3.8);
                    shield.lineTo(13.2, 8.0);
                    shield.curveTo(13.2, 11.2, 10.6, 13.3, 8.0, 14.4);
                    shield.curveTo(5.4, 13.3, 2.8, 11.2, 2.8, 8.0);
                    shield.lineTo(2.8, 3.8);
                    shield.closePath();
                    g2.draw(shield);
                    Path2D chk = new Path2D.Double();
                    chk.moveTo(5.8, 8.0);
                    chk.lineTo(7.4, 9.7);
                    chk.lineTo(10.5, 6.2);
                    g2.draw(chk);
                }
                case DATABASE -> {
                    g2.draw(new Ellipse2D.Double(3.0, 2.0, 10.0, 3.4));
                    g2.draw(new Line2D.Double(3.0, 3.7, 3.0, 12.2));
                    g2.draw(new Line2D.Double(13.0, 3.7, 13.0, 12.2));
                    g2.draw(new Arc2D.Double(3.0, 6.3, 10.0, 3.4, 180, 180, Arc2D.OPEN));
                    g2.draw(new Arc2D.Double(3.0, 10.5, 10.0, 3.4, 180, 180, Arc2D.OPEN));
                }
                case PLUS -> {
                    g2.draw(new Line2D.Double(8.0, 3.0, 8.0, 13.0));
                    g2.draw(new Line2D.Double(3.0, 8.0, 13.0, 8.0));
                }
                case MENU -> {
                    g2.draw(new Line2D.Double(2.5, 4.0, 13.5, 4.0));
                    g2.draw(new Line2D.Double(2.5, 8.0, 13.5, 8.0));
                    g2.draw(new Line2D.Double(2.5, 12.0, 13.5, 12.0));
                }
                case LOGOUT -> {
                    Path2D frame = new Path2D.Double();
                    frame.moveTo(8.0, 2.5);
                    frame.lineTo(3.2, 2.5);
                    frame.lineTo(3.2, 13.5);
                    frame.lineTo(8.0, 13.5);
                    g2.draw(frame);
                    g2.draw(new Line2D.Double(6.5, 8.0, 13.2, 8.0));
                    g2.draw(new Line2D.Double(10.6, 5.4, 13.2, 8.0));
                    g2.draw(new Line2D.Double(10.6, 10.6, 13.2, 8.0));
                }
                case REFRESH -> {
                    g2.draw(new Arc2D.Double(2.8, 2.8, 10.4, 10.4, 40, 285, Arc2D.OPEN));
                    Path2D head = new Path2D.Double();
                    head.moveTo(10.2, 3.2);
                    head.lineTo(13.2, 3.4);
                    head.lineTo(12.6, 6.2);
                    g2.draw(head);
                }
                case ALERT -> {
                    Path2D tri = new Path2D.Double();
                    tri.moveTo(8.0, 2.2);
                    tri.lineTo(14.2, 13.2);
                    tri.lineTo(1.8, 13.2);
                    tri.closePath();
                    g2.draw(tri);
                    g2.draw(new Line2D.Double(8.0, 6.0, 8.0, 9.4));
                    g2.fill(new Ellipse2D.Double(7.1, 10.6, 1.8, 1.8));
                }
                case BOLT -> {
                    GeneralPath bolt = new GeneralPath();
                    bolt.moveTo(9.2, 1.6);
                    bolt.lineTo(3.8, 8.6);
                    bolt.lineTo(7.8, 8.6);
                    bolt.lineTo(6.8, 14.4);
                    bolt.lineTo(12.2, 7.4);
                    bolt.lineTo(8.2, 7.4);
                    bolt.closePath();
                    g2.fill(bolt);
                }
                case CHECK -> {
                    Path2D chk = new Path2D.Double();
                    chk.moveTo(3.2, 8.4);
                    chk.lineTo(6.6, 11.8);
                    chk.lineTo(12.8, 4.6);
                    g2.draw(chk);
                }
                case CROSS -> {
                    g2.draw(new Line2D.Double(4.0, 4.0, 12.0, 12.0));
                    g2.draw(new Line2D.Double(12.0, 4.0, 4.0, 12.0));
                }
                case INFO -> {
                    g2.draw(new Ellipse2D.Double(2.2, 2.2, 11.6, 11.6));
                    g2.fill(new Ellipse2D.Double(7.1, 4.6, 1.8, 1.8));
                    g2.draw(new Line2D.Double(8.0, 7.6, 8.0, 11.4));
                }
                case CLOCK -> {
                    g2.draw(new Ellipse2D.Double(2.2, 2.2, 11.6, 11.6));
                    g2.draw(new Line2D.Double(8.0, 4.6, 8.0, 8.2));
                    g2.draw(new Line2D.Double(8.0, 8.2, 10.6, 9.8));
                }
                case STATUS_DOT -> g2.fill(new Ellipse2D.Double(3.0, 3.0, 10.0, 10.0));
                case CIRCLE_OUTLINE -> g2.draw(new Ellipse2D.Double(3.0, 3.0, 10.0, 10.0));
                case CHEVRON_LEFT -> {
                    Path2D ch = new Path2D.Double();
                    ch.moveTo(10.2, 3.4);
                    ch.lineTo(5.6, 8.0);
                    ch.lineTo(10.2, 12.6);
                    g2.draw(ch);
                }
                case CHEVRON_RIGHT -> {
                    Path2D ch = new Path2D.Double();
                    ch.moveTo(5.8, 3.4);
                    ch.lineTo(10.4, 8.0);
                    ch.lineTo(5.8, 12.6);
                    g2.draw(ch);
                }
                case EYE -> {
                    Path2D eye = new Path2D.Double();
                    eye.moveTo(1.8, 8.0);
                    eye.quadTo(8.0, 2.6, 14.2, 8.0);
                    eye.quadTo(8.0, 13.4, 1.8, 8.0);
                    g2.draw(eye);
                    g2.draw(new Ellipse2D.Double(6.0, 6.0, 4.0, 4.0));
                }
                case EYE_OFF -> {
                    Path2D eye = new Path2D.Double();
                    eye.moveTo(1.8, 8.0);
                    eye.quadTo(8.0, 2.6, 14.2, 8.0);
                    eye.quadTo(8.0, 13.4, 1.8, 8.0);
                    g2.draw(eye);
                    g2.draw(new Ellipse2D.Double(6.0, 6.0, 4.0, 4.0));
                    g2.draw(new Line2D.Double(2.8, 13.2, 13.2, 2.8));
                }
                case STAR -> {
                    GeneralPath star = new GeneralPath();
                    double cx = 8.0, cy = 8.2, outerR = 6.2, innerR = 2.6;
                    for (int i = 0; i < 10; i++) {
                        double angle = Math.toRadians(-90 + i * 36);
                        double r = (i % 2 == 0) ? outerR : innerR;
                        double px = cx + Math.cos(angle) * r;
                        double py = cy + Math.sin(angle) * r;
                        if (i == 0) star.moveTo(px, py);
                        else star.lineTo(px, py);
                    }
                    star.closePath();
                    g2.fill(star);
                }
            }

            g2.dispose();
        }
    }
}
