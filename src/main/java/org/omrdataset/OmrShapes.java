//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        O m r S h a p e s                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
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
package org.omrdataset;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Class {@code OmrShapes} complements enum {@link OmrShape} with related features.
 *
 * @author Hervé Bitteur
 */
public abstract class OmrShapes
{
    //~ Static fields/initializers -----------------------------------------------------------------

    public static final List<String> NAMES = getNames();

    public static final Map<OmrShape, Shape> toShape = buildShapeMap();

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * Report the list of OmrShape values, to be used by DL4J.
     *
     * @return OmrShape values, as a List
     */
    public static final List<String> getNames ()
    {
        List<String> list = new ArrayList<String>();

        for (OmrShape shape : OmrShape.values()) {
            list.add(shape.toString());
        }

        return list;
    }

    /**
     * Print out the omrShape ordinal and name.
     */
    public static void printOmrShapes ()
    {
        for (OmrShape shape : OmrShape.values()) {
            System.out.printf("%3d %s%n", shape.ordinal(), shape.toString());
        }
    }

    /**
     * Build the map (OmrShape -> Shape).
     *
     * @return the initialized map
     */
    private static Map<OmrShape, Shape> buildShapeMap ()
    {
        final Map<OmrShape, Shape> map = new EnumMap<OmrShape, Shape>(OmrShape.class);

        map.put(OmrShape.None, Shape.CLUTTER);
        map.put(OmrShape.Accent, Shape.ACCENT);
        map.put(OmrShape.Arpeggiato, Shape.ARPEGGIATO);
        map.put(OmrShape.AugmentationDot, Shape.AUGMENTATION_DOT);
        map.put(OmrShape.Brace, Shape.BRACE);
        map.put(OmrShape.BreathMark, Shape.BREATH_MARK);
        map.put(OmrShape.Breve, Shape.BREVE);
        map.put(OmrShape.Caesura, Shape.CAESURA);
        map.put(OmrShape.ClefC, Shape.C_CLEF);
        map.put(OmrShape.ClefF, Shape.F_CLEF);
        map.put(OmrShape.ClefF8a, Shape.F_CLEF_8VA);
        map.put(OmrShape.ClefF8b, Shape.F_CLEF_8VB);
        map.put(OmrShape.ClefFSmall, Shape.F_CLEF_SMALL);
        map.put(OmrShape.ClefG, Shape.G_CLEF);
        map.put(OmrShape.ClefG8a, Shape.G_CLEF_8VA);
        map.put(OmrShape.ClefG8b, Shape.G_CLEF_8VB);
        map.put(OmrShape.ClefGSmall, Shape.G_CLEF_SMALL);
        map.put(OmrShape.ClefPercussion, Shape.PERCUSSION_CLEF);
        map.put(OmrShape.Coda, Shape.CODA);
        map.put(OmrShape.CommonTime, Shape.COMMON_TIME);
        map.put(OmrShape.CutTime, Shape.CUT_TIME);
        map.put(OmrShape.DaCapo, Shape.DA_CAPO);
        map.put(OmrShape.DalSegno, Shape.DAL_SEGNO);
        map.put(OmrShape.Digit0, Shape.DIGIT_0);
        map.put(OmrShape.Digit1, Shape.DIGIT_1);
        map.put(OmrShape.Digit2, Shape.DIGIT_2);
        map.put(OmrShape.Digit3, Shape.DIGIT_3);
        map.put(OmrShape.Digit4, Shape.DIGIT_4);
        map.put(OmrShape.DoubleFlat, Shape.DOUBLE_FLAT);
        map.put(OmrShape.DoubleSharp, Shape.DOUBLE_SHARP);
        map.put(OmrShape.DynamicF, Shape.DYNAMICS_F);
        map.put(OmrShape.DynamicFF, Shape.DYNAMICS_FF);
        //        map.put(OmrShape.DynamicFFF, null);
        //        map.put(OmrShape.DynamicFFFF, null);
        //        map.put(OmrShape.DynamicFFFFF, null);
        //        map.put(OmrShape.DynamicFFFFFF, null);
        map.put(OmrShape.DynamicFP, Shape.DYNAMICS_FP);
        //        map.put(OmrShape.DynamicM, null);
        map.put(OmrShape.DynamicMF, Shape.DYNAMICS_MF);
        map.put(OmrShape.DynamicMP, Shape.DYNAMICS_MP);
        map.put(OmrShape.DynamicP, Shape.DYNAMICS_P);
        map.put(OmrShape.DynamicPP, Shape.DYNAMICS_PP);
        //        map.put(OmrShape.DynamicPPP, null);
        //        map.put(OmrShape.DynamicPPPP, null);
        //        map.put(OmrShape.DynamicPPPPP, null);
        //        map.put(OmrShape.DynamicPPPPPP, null);
        //        map.put(OmrShape.DynamicR, null);
        //        map.put(OmrShape.DynamicRF, null);
        //        map.put(OmrShape.DynamicRFZ, null);
        //        map.put(OmrShape.DynamicS, null);
        //        map.put(OmrShape.DynamicSF, null);
        //        map.put(OmrShape.DynamicSFF, null);
        //        map.put(OmrShape.DynamicSFFZ, null);
        //        map.put(OmrShape.DynamicSFP, null);
        //        map.put(OmrShape.DynamicSFPP, null);
        map.put(OmrShape.DynamicSFZ, Shape.DYNAMICS_SFZ);
        //        map.put(OmrShape.DynamicZ, null);
        map.put(OmrShape.FermataAbove, Shape.FERMATA);
        map.put(OmrShape.FermataBelow, Shape.FERMATA_BELOW);
        //        map.put(OmrShape.Fine, null);
        map.put(OmrShape.Flag1Down, Shape.FLAG_1);
        map.put(OmrShape.Flag1Up, Shape.FLAG_1_UP);
        map.put(OmrShape.Flag1UpSmall, Shape.SMALL_FLAG);
        map.put(OmrShape.Flag1UpSmallSlash, Shape.SMALL_FLAG_SLASH);
        map.put(OmrShape.Flag2Down, Shape.FLAG_2);
        map.put(OmrShape.Flag2Up, Shape.FLAG_2_UP);
        map.put(OmrShape.Flag3Down, Shape.FLAG_3);
        map.put(OmrShape.Flag3Up, Shape.FLAG_3_UP);
        map.put(OmrShape.Flag4Down, Shape.FLAG_4);
        map.put(OmrShape.Flag4Up, Shape.FLAG_4_UP);
        map.put(OmrShape.Flag5Down, Shape.FLAG_5);
        map.put(OmrShape.Flag5Up, Shape.FLAG_5_UP);
        map.put(OmrShape.Flat, Shape.FLAT);
        map.put(OmrShape.GraceNote, Shape.GRACE_NOTE);
        map.put(OmrShape.GraceNoteSlash, Shape.GRACE_NOTE_SLASH);
        //        map.put(OmrShape.HairpinSegment, null);
        map.put(OmrShape.KeyFlat1, Shape.KEY_FLAT_1);
        map.put(OmrShape.KeyFlat2, Shape.KEY_FLAT_2);
        map.put(OmrShape.KeyFlat3, Shape.KEY_FLAT_3);
        map.put(OmrShape.KeyFlat4, Shape.KEY_FLAT_4);
        map.put(OmrShape.KeyFlat5, Shape.KEY_FLAT_5);
        map.put(OmrShape.KeyFlat6, Shape.KEY_FLAT_6);
        map.put(OmrShape.KeyFlat7, Shape.KEY_FLAT_7);
        map.put(OmrShape.KeySharp1, Shape.KEY_SHARP_1);
        map.put(OmrShape.KeySharp2, Shape.KEY_SHARP_2);
        map.put(OmrShape.KeySharp3, Shape.KEY_SHARP_3);
        map.put(OmrShape.KeySharp4, Shape.KEY_SHARP_4);
        map.put(OmrShape.KeySharp5, Shape.KEY_SHARP_5);
        map.put(OmrShape.KeySharp6, Shape.KEY_SHARP_6);
        map.put(OmrShape.KeySharp7, Shape.KEY_SHARP_7);
        map.put(OmrShape.Ledger, Shape.LEDGER);
        map.put(OmrShape.Marcato, Shape.STRONG_ACCENT);
        map.put(OmrShape.Mordent, Shape.MORDENT);
        map.put(OmrShape.MordentSlash, Shape.MORDENT_INVERTED);
        map.put(OmrShape.Natural, Shape.NATURAL);
        map.put(OmrShape.NoteHeadBlack, Shape.NOTEHEAD_BLACK);
        map.put(OmrShape.NoteHeadBlackSmall, Shape.NOTEHEAD_BLACK_SMALL);
        map.put(OmrShape.NoteHeadVoid, Shape.NOTEHEAD_VOID);
        map.put(OmrShape.NoteHeadVoidSmall, Shape.NOTEHEAD_VOID_SMALL);
        map.put(OmrShape.NoteWhole, Shape.WHOLE_NOTE);
        map.put(OmrShape.NoteWholeSmall, Shape.WHOLE_NOTE_SMALL);
        map.put(OmrShape.OttavaAlta, Shape.OTTAVA_ALTA);
        map.put(OmrShape.OttavaBassa, Shape.OTTAVA_BASSA);
        map.put(OmrShape.PedalDown, Shape.PEDAL_MARK);
        map.put(OmrShape.PedalUp, Shape.PEDAL_UP_MARK);
        map.put(OmrShape.PluckA, Shape.PLUCK_A);
        map.put(OmrShape.PluckI, Shape.PLUCK_I);
        map.put(OmrShape.PluckM, Shape.PLUCK_M);
        map.put(OmrShape.PluckP, Shape.PLUCK_P);
        map.put(OmrShape.Rest2, Shape.HALF_REST);
        map.put(OmrShape.Rest4, Shape.QUARTER_REST);
        map.put(OmrShape.Rest8, Shape.EIGHTH_REST);
        map.put(OmrShape.Rest16, Shape.ONE_16TH_REST);
        map.put(OmrShape.Rest32, Shape.ONE_32ND_REST);
        map.put(OmrShape.Rest64, Shape.ONE_64TH_REST);
        map.put(OmrShape.Rest128, Shape.ONE_128TH_REST);
        map.put(OmrShape.RestBreve, Shape.BREVE_REST);
        map.put(OmrShape.RestLong, Shape.LONG_REST);
        map.put(OmrShape.RestWhole, Shape.WHOLE_REST);
        map.put(OmrShape.RomanI, Shape.ROMAN_I);
        map.put(OmrShape.RomanII, Shape.ROMAN_II);
        map.put(OmrShape.RomanIII, Shape.ROMAN_III);
        map.put(OmrShape.RomanIV, Shape.ROMAN_IV);
        map.put(OmrShape.RomanV, Shape.ROMAN_V);
        map.put(OmrShape.RomanVI, Shape.ROMAN_VI);
        map.put(OmrShape.RomanVII, Shape.ROMAN_VII);
        map.put(OmrShape.RomanVIII, Shape.ROMAN_VIII);
        map.put(OmrShape.RomanIX, Shape.ROMAN_IX);
        map.put(OmrShape.RomanX, Shape.ROMAN_X);
        map.put(OmrShape.RomanXI, Shape.ROMAN_XI);
        map.put(OmrShape.RomanXII, Shape.ROMAN_XII);
        map.put(OmrShape.Segno, Shape.SEGNO);
        map.put(OmrShape.Sharp, Shape.SHARP);
        map.put(OmrShape.Staccatissimo, Shape.STACCATISSIMO);
        map.put(OmrShape.StaccatoDot, Shape.STACCATO);
        map.put(OmrShape.Stem, Shape.STEM);
        map.put(OmrShape.Tenuto, Shape.TENUTO);
        map.put(OmrShape.Time0, Shape.TIME_ZERO);
        map.put(OmrShape.Time1, Shape.TIME_ONE);
        map.put(OmrShape.Time2, Shape.TIME_TWO);
        map.put(OmrShape.Time3, Shape.TIME_THREE);
        map.put(OmrShape.Time4, Shape.TIME_FOUR);
        map.put(OmrShape.Time5, Shape.TIME_FIVE);
        map.put(OmrShape.Time6, Shape.TIME_SIX);
        map.put(OmrShape.Time7, Shape.TIME_SEVEN);
        map.put(OmrShape.Time8, Shape.TIME_EIGHT);
        map.put(OmrShape.Time9, Shape.TIME_NINE);
        map.put(OmrShape.Time12, Shape.TIME_TWELVE);
        map.put(OmrShape.Time16, Shape.TIME_SIXTEEN);
        map.put(OmrShape.Time2_2, Shape.TIME_TWO_TWO);
        map.put(OmrShape.Time3_4, Shape.TIME_THREE_FOUR);
        map.put(OmrShape.Time3_8, Shape.TIME_THREE_EIGHT);
        map.put(OmrShape.Time4_4, Shape.TIME_FOUR_FOUR);
        map.put(OmrShape.Time5_4, Shape.TIME_FIVE_FOUR);
        map.put(OmrShape.Time6_8, Shape.TIME_SIX_EIGHT);
        //        map.put(OmrShape.ToCoda, null);
        map.put(OmrShape.Tr, Shape.TR);
        map.put(OmrShape.Tuplet3, Shape.TUPLET_THREE);
        map.put(OmrShape.Tuplet6, Shape.TUPLET_SIX);
        map.put(OmrShape.Turn, Shape.TURN);
        map.put(OmrShape.TurnInverted, Shape.TURN_INVERTED);
        map.put(OmrShape.TurnSlash, Shape.TURN_SLASH);
        map.put(OmrShape.TurnUp, Shape.TURN_UP);

        return map;
    }
}
