//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           F i l t e r                                          //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omrdataset.extraction;

import org.audiveris.omrdataset.api.OmrShape;
import org.audiveris.omrdataset.api.SheetAnnotations;
import org.audiveris.omrdataset.api.SymbolInfo;
import org.audiveris.omrdataset.api.SymbolInfo.Cause;
import org.audiveris.omrdataset.api.TablatureAreas;
import org.audiveris.omrdataset.extraction.SourceInfo.USheetId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Class {@code Filter} filters the input data.
 *
 * @author Hervé Bitteur
 */
public class Filter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(Filter.class);

    private static final double MAX_OVERLAP = 0.6;

    private static final double OUTER_MARGIN = 0.25; // WRT interline

    private static final int TABLATURE_X_MARGIN = 20;

    private static final int TABLATURE_Y_MARGIN = 20;

    /** Maximum width for some shapes. */
    private static final Map<OmrShape, Double> maxWidths = new EnumMap<>(OmrShape.class);

    /** Maximum height for some shapes. */
    private static final Map<OmrShape, Double> maxHeights = new EnumMap<>(OmrShape.class);

    static {
        populateWidths();
        populateHeights();
    }

    //~ Instance fields ----------------------------------------------------------------------------
    private final USheetId uSheetId;

    private final SheetAnnotations annotations;

    private final List<Rectangle> excludedAreas;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a {@code Filter} object working on a sheet annotations.
     *
     * @param uSheetId    universal sheet ID
     * @param annotations related annotations
     * @param tablatures  related tablature areas, or null
     */
    public Filter (USheetId uSheetId,
                   SheetAnnotations annotations,
                   TablatureAreas tablatures)
    {
        this.uSheetId = uSheetId;
        this.annotations = annotations;

        // Exclusions found in annotations file
        excludedAreas = annotations.getSheetInfo().excludedAreas;

        // Add exclusions based on tablature areas
        excludeTablatures(tablatures);
    }

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * Filter the annotations.
     *
     * @throws IOException in case of IO problem
     */
    public void process ()
            throws IOException
    {
        logger.debug("Filtering ...");

        try {
            // Assign a sequential ID to each unmarshalled symbol
            annotations.setIds(annotations.getOuterSymbolsLiveList());

            convertInnerDots();

            checkZeroDimensions();
            checkExcludedAreas();
            checkOutliers();
            checkOverlaps();
            checkInners();
            checkDimensions();

            // Now that all symbols are correct, address the cClefs samples
            convertCClefs();
        } catch (Throwable ex) {
            logger.warn("{} Error processing annotations {}", uSheetId, annotations, ex);
        }
    }

    //-------------------//
    // excludeTablatures //
    //-------------------//
    /**
     * Consider tablatures as excluded areas.
     *
     * @param tablatures tablatures core areas
     */
    private void excludeTablatures (TablatureAreas tablatures)
    {
        if (tablatures == null) {
            return;
        }

        for (Rectangle rect : tablatures.areas) {
            Rectangle r = new Rectangle(rect); // Copy
            r.grow(TABLATURE_X_MARGIN, TABLATURE_Y_MARGIN);
            excludedAreas.add(r);
        }
    }

    private void checkDimensions ()
    {
        for (SymbolInfo symbol : annotations.getGoodSymbols()) {
            checkSymbolWidth(symbol);
            checkSymbolHeight(symbol);
        }
    }

    private void checkSymbolWidth (SymbolInfo symbol)
    {
        final OmrShape shape = symbol.getOmrShape();
        final Double maxWidthFraction = maxWidths.get(shape);

        if (maxWidthFraction != null) {
            double max = symbol.getInterline() * maxWidthFraction;
            if (symbol.getBounds().getWidth() > max) {
                logger.debug("Too wide symbol {}", symbol);
                symbol.setInvalid(Cause.TooWide);
            }
        }

    }

    private void checkSymbolHeight (SymbolInfo symbol)
    {
        final OmrShape shape = symbol.getOmrShape();
        final Double maxHeightFraction = maxHeights.get(shape);

        if (maxHeightFraction != null) {
            double max = symbol.getInterline() * maxHeightFraction;

            if (symbol.getBounds().getHeight() > max) {
                logger.debug("Too high symbol {}", symbol);
                symbol.setInvalid(Cause.TooHigh);
            }
        }
    }

    /**
     * Remove symbols located in excluded areas (such as tablatures).
     */
    private void checkExcludedAreas ()
    {
        for (SymbolInfo symbol : annotations.getGoodSymbols()) {
            final Rectangle2D box = symbol.getBounds();

            // Check symbol center is not in some excluded area
            final Point2D center = new Point2D.Double(box.getCenterX(), box.getCenterY());
            for (Rectangle area : excludedAreas) {
                if (area.contains(center)) {
                    logger.debug("Excluded area for {}", symbol);
                    symbol.setInvalid(Cause.InExcludedArea, true);
                    break;
                }
            }
        }
    }

    /**
     * Check inner symbols are contained within the bounds of the outer symbol.
     */
    private void checkInners ()
    {
        for (SymbolInfo symbol : annotations.getGoodSymbols()) {
            final List<SymbolInfo> inners = symbol.getInnerSymbols();

            if (!inners.isEmpty()) {
                final Rectangle outerBox = symbol.getBounds().getBounds();
                // Add some margin to cope with approximate bounds values
                final int margin = (int) Math.rint(symbol.getInterline() * OUTER_MARGIN);
                outerBox.grow(margin, margin);

                for (SymbolInfo inner : inners) {
                    final Rectangle2D innerbox = inner.getBounds();
                    if (!outerBox.contains(innerbox)) {
                        logger.debug("Too large inner {}", inner);
                        inner.setInvalid(Cause.TooLargeInner);
                    }
                }
            }
        }
    }

    /**
     * Check that symbols are fully within image bounds.
     */
    private void checkOutliers ()
    {
        final Rectangle sheetBounds = new Rectangle(annotations.getSheetInfo().dim);

        for (SymbolInfo symbol : annotations.getGoodSymbols()) {
            final Rectangle2D box = symbol.getBounds();

            if (!sheetBounds.contains(box)) {
                logger.debug("Outlier {}", symbol);
                symbol.setInvalid(Cause.OutOfImage);
            }
        }
    }

    /**
     * Check that no pair of symbols overlap too much.
     * If so, both symbols are discarded.
     * <p>
     * We have observed full overlap (identical bounds) between identical shape.
     * This is a bug in MuseScore extractor, we keep the first symbol and invalidate the second.
     */
    private void checkOverlaps ()
    {
        final List<SymbolInfo> sortedSymbols = new ArrayList<>(annotations.getGoodSymbols());

        // Sort by starting abscissa
        Collections.sort(sortedSymbols, (SymbolInfo s1, SymbolInfo s2)
                         -> Double.compare(s1.getBounds().getX(), s2.getBounds().getX()));

        SymbolLoop:
        for (int i = 0; i < sortedSymbols.size(); i++) {
            final SymbolInfo symbol = sortedSymbols.get(i);

            if (symbol.isInvalid()) {
                continue;
            }

            final Rectangle2D box = symbol.getBounds();
            final double boxArea = box.getWidth() * box.getHeight();
            final double xMax = Math.ceil(box.getMaxX());

            for (SymbolInfo s : sortedSymbols.subList(i + 1, sortedSymbols.size())) {
                final Rectangle2D b = s.getBounds();
                if (b.getX() > xMax) {
                    break;
                }

                if (s.isInvalid()) {
                    continue;
                }

                if (box.intersects(b)) {
                    // Accept the case "outer symbol vs one of its inner symbol(s)"
                    if (areParents(symbol, s)) {
                        logger.debug("Parents {} {}", symbol, s);
                        continue;
                    }

                    final double bArea = b.getWidth() * b.getHeight();
                    final Rectangle2D inter = box.createIntersection(b);
                    final double interArea = inter.getWidth() * inter.getHeight();
                    final double ioBox = interArea / boxArea;
                    final double ioB = interArea / bArea;

                    if (ioBox > MAX_OVERLAP || ioB > MAX_OVERLAP) {
                        logger.info("{} {}/{} {} {}",
                                    uSheetId,
                                    String.format("%.2f", ioBox),
                                    String.format("%.2f", ioB), symbol, s);

                        if (symbol.getOmrShape() == s.getOmrShape() && box.equals(b)) {
                            // Just duplication
                            s.setInvalid(Cause.Duplication, true);
                        } else {
                            // Real overlap, discard both symbols
                            symbol.setInvalid(Cause.StrongOverlap);
                            s.setInvalid(Cause.StrongOverlap);
                            continue SymbolLoop;
                        }
                    }
                }
            }
        }
    }

    private boolean areParents (SymbolInfo s1,
                                SymbolInfo s2)
    {
        int id1 = s1.getId();
        int id2 = s2.getId();
        SymbolInfo old = (id1 < id2) ? s1 : s2;
        SymbolInfo young = (id1 < id2) ? s2 : s1;

        return old.getInnerSymbols().contains(young);
    }

    /**
     * Check for concrete width & height of each symbol.
     */
    private void checkZeroDimensions ()
    {
        SymbolLoop:
        for (SymbolInfo symbol : annotations.getGoodSymbols()) {
            final Rectangle2D box = symbol.getBounds();

            if (box.getWidth() == 0.0 || box.getHeight() == 0.0) {
                logger.debug("Zero dimension for {}", symbol);
                symbol.setInvalid(Cause.ZeroDimension);
            }
        }
    }

    //---------------//
    // convertCClefs //
    //---------------//
    /**
     * Convert cClef samples as cClefAlto or cClefTenor samples.
     * <p>
     * MuseScore provides cClef samples but makes no difference between cClefAlto and cClefTenor.
     * We can use the vertical clef position with respect to the underlying staff to disambiguate.
     * Staff vertical position can be inferred from (valid) barline samples that vertically overlap
     * with the cClef sample.
     */
    private void convertCClefs ()
    {
        // First retrieve the cClef population
        List<SymbolInfo> clefs = new ArrayList<>();
        for (SymbolInfo symbol : annotations.getGoodSymbols()) {
            OmrShape shape = symbol.getOmrShape();
            if (shape == OmrShape.cClef || shape == OmrShape.cClefChange) {
                clefs.add(symbol);
            }
        }

        if (clefs.isEmpty()) {
            return;
        }

        // Retrieve staves vertical ranges, using one representative barline for each staff
        List<Rectangle2D> boxes = new ArrayList<>();
        for (SymbolInfo symbol : annotations.getGoodSymbols()) {
            final OmrShape shape = symbol.getOmrShape();

            if (shape.isBarline()) {
                final Rectangle2D box = symbol.getBounds();
                boolean isolated = true;
                for (Rectangle2D b : boxes) {
                    if (b.intersects(box)) {
                        isolated = false;
                        break;
                    }
                }

                if (isolated) {
                    boxes.add(box);
                }
            }
        }

        // For each clef, look for suitable barline box.
        SymbolLoop:
        for (SymbolInfo clef : clefs) {
            Rectangle2D cBounds = clef.getBounds();
            double cMin = cBounds.getMinY();
            double cMax = cBounds.getMaxY();
            double cMiddle = cBounds.getCenterY();

            for (Rectangle2D box : boxes) {
                double bMin = box.getMinY();
                double bMax = box.getMaxY();
                double commonMin = Math.max(cMin, bMin);
                double commonMax = Math.min(cMax, bMax);

                if (commonMax > commonMin) {
                    // We have a vertical overlap between clef and staff/barline
                    OmrShape oldShape = clef.getOmrShape();
                    OmrShape newShape = cClefVariantOf(oldShape, cMiddle, box);
                    logger.debug("{} {} set to {}", uSheetId, clef, newShape);
                    clef.setOmrShape(newShape);

                    continue SymbolLoop;
                }
            }

            // Here, we have found no staff compatible with current clef
            clef.setInvalid(Cause.NoRelatedStaff);
        }
    }

    /**
     * Determine precise cClef shape based on relative vertical positions of clef center
     * and related staff.
     *
     * @param clefShape   clef shape (either cClef or cClefChange)
     * @param clefMid     ordinate of clef center
     * @param staffBounds staff bounds, actually barline bounds, but we care about ordinates only
     * @return precise cClef variant
     */
    private OmrShape cClefVariantOf (OmrShape clefShape,
                                     double clefMid,
                                     Rectangle2D staffBounds)
    {
        final boolean change = clefShape == OmrShape.cClefChange;
        final double staffMid = staffBounds.getCenterY();
        final int toClef = (int) Math.rint(4 * (clefMid - staffMid) / staffBounds.getHeight());

        switch (toClef) {
//        case -2:
//            return change ? OmrShape.cClefBaritoneChange : OmrShape.cClefBaritone;
        case -1:
            return change ? OmrShape.cClefTenorChange : OmrShape.cClefTenor;
        case 0:
            return change ? OmrShape.cClefAltoChange : OmrShape.cClefAlto;
//        case +1:
//            return change ? OmrShape.cClefMezzoSopranoChange : OmrShape.cClefMezzoSoprano;
//        case +2:
//            return change ? OmrShape.cClefSopranoChange : OmrShape.cClefSoprano;
        default:
            logger.warn("Cannot infer cClef variant toClef:{}", toClef);
            return null;
        }
    }

    //------------------//
    // convertInnerDots //
    //------------------//
    /**
     * Check the dots as inner symbol of an outer repeat symbol are assigned the repeatDot
     * shape rather than the augmentationDot shape.
     */
    private void convertInnerDots ()
    {
        for (SymbolInfo symbol : annotations.getOuterSymbolsLiveList()) {
            OmrShape shape = symbol.getOmrShape();

            if (shape == OmrShape.repeatLeft
                        || shape == OmrShape.repeatRight
                        || shape == OmrShape.repeatRightLeft) {
                for (SymbolInfo inner : symbol.getInnerSymbols()) {
                    if (inner.getOmrShape() == OmrShape.augmentationDot) {
                        logger.debug("Inner repeat dot {}", inner);
                        inner.setOmrShape(OmrShape.repeatDot);
                    }
                }
            }
        }
    }

    private static void populateWidths ()
    {
        //
        // 4.3 Barlines
        //
        maxWidths.put(OmrShape.barlineSingle, 0.6);

    }

    private static void populateHeights ()
    {
        //
        // 4.3 Barlines
        //
        final double MAX_BARLINE_HEIGHT = 4.4; // WRT interline
        maxHeights.put(OmrShape.barlineSingle, MAX_BARLINE_HEIGHT);
        maxHeights.put(OmrShape.barlineDouble, MAX_BARLINE_HEIGHT);
        maxHeights.put(OmrShape.barlineFinal, MAX_BARLINE_HEIGHT);
        maxHeights.put(OmrShape.barlineReverseFinal, MAX_BARLINE_HEIGHT);
        maxHeights.put(OmrShape.barlineHeavy, MAX_BARLINE_HEIGHT);
        maxHeights.put(OmrShape.barlineHeavyHeavy, MAX_BARLINE_HEIGHT);
        maxHeights.put(OmrShape.barlineDashed, MAX_BARLINE_HEIGHT);
        maxHeights.put(OmrShape.barlineDotted, MAX_BARLINE_HEIGHT);

        //
        // 4.4 Repeats
        //
        maxHeights.put(OmrShape.repeatLeft, MAX_BARLINE_HEIGHT);
        maxHeights.put(OmrShape.repeatRight, MAX_BARLINE_HEIGHT);
        maxHeights.put(OmrShape.repeatRightLeft, MAX_BARLINE_HEIGHT);

        //
        // 4.6 Time signatures
        //
        final double MAX_TIME_HEIGHT = 2.2; // WRT interline
        maxHeights.put(OmrShape.timeSig0, MAX_TIME_HEIGHT);
        maxHeights.put(OmrShape.timeSig1, MAX_TIME_HEIGHT);
        maxHeights.put(OmrShape.timeSig2, MAX_TIME_HEIGHT);
        maxHeights.put(OmrShape.timeSig3, MAX_TIME_HEIGHT);
        maxHeights.put(OmrShape.timeSig4, MAX_TIME_HEIGHT);
        maxHeights.put(OmrShape.timeSig5, MAX_TIME_HEIGHT);
        maxHeights.put(OmrShape.timeSig6, MAX_TIME_HEIGHT);
        maxHeights.put(OmrShape.timeSig7, MAX_TIME_HEIGHT);
        maxHeights.put(OmrShape.timeSig8, MAX_TIME_HEIGHT);
        maxHeights.put(OmrShape.timeSig9, MAX_TIME_HEIGHT);
        maxHeights.put(OmrShape.timeSig12, MAX_TIME_HEIGHT);
        maxHeights.put(OmrShape.timeSig16, MAX_TIME_HEIGHT);
    }
}
