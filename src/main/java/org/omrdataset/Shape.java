//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                            S h a p e                                           //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
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

/**
 * Class {@code Shape} was derived from current Audiveris Shape class, in order to run
 * the OMR-Dataset hack.
 * <p>
 * We use it now to make sure that all needed shapes are actually available and provided in some way
 * from OmrShape / MuseScore.
 *
 * @author Hervé Bitteur
 */
public enum Shape
{
    //
    // dots --------------------------------------------------------------------
    //
    REPEAT_DOT_PAIR("Pair of repeat dots"),
    AUGMENTATION_DOT("Augmentation Dot"),
    STACCATO("Staccato dot"),

    //
    //
    // Bars --------------------------------------------------------------------
    //
    DAL_SEGNO("D.S.: Repeat from the sign"),
    DA_CAPO("D.C.: Repeat from the beginning"),
    SEGNO("Sign"),
    CODA("Closing section"),
    BREATH_MARK("Breath Mark"),
    CAESURA("Caesura"),
    FERMATA("Fermata above"),
    FERMATA_BELOW("Fermata below"),

    //
    // Clefs -------------------------------------------------------------------
    //
    G_CLEF("Treble Clef"),
    G_CLEF_SMALL("Small Treble Clef"),
    G_CLEF_8VA("Treble Clef Ottava Alta"),
    G_CLEF_8VB("Treble Clef Ottava Bassa"),
    C_CLEF("Ut Clef"),
    F_CLEF("Bass Clef"),
    F_CLEF_SMALL("Small Bass Clef"),
    F_CLEF_8VA("Bass Clef Ottava Alta"),
    F_CLEF_8VB("Bass Clef Ottava Bassa"),
    PERCUSSION_CLEF("Percussion Clef"),

    //
    // Alterations -------------------------------------------------------------
    //
    FLAT("Minus one half step"),
    NATURAL("Natural value"),
    SHARP("Plus one half step"),
    DOUBLE_SHARP("Double Sharp"),
    DOUBLE_FLAT("Double Flat"),

    //
    // Time --------------------------------------------------------------------
    //
    TIME_ZERO("Time digit 0"),
    TIME_ONE("Time digit 1"),
    TIME_TWO("Time digit 2"),
    TIME_THREE("Time digit 3"),
    TIME_FOUR("Time digit 4"),
    TIME_FIVE("Time digit 5"),
    TIME_SIX("Time digit 6"),
    TIME_SEVEN("Time digit 7"),
    TIME_EIGHT("Time digit 8"),
    TIME_NINE("Time digit 9"),
    TIME_TWELVE("Time number 12"),
    TIME_SIXTEEN("Time number 16"),

    // Whole time sigs
    COMMON_TIME("Alpha = 4/4"),
    CUT_TIME("Semi-Alpha = 2/2"),
    // Predefined time combos
    TIME_FOUR_FOUR("Rational 4/4"),
    TIME_TWO_TWO("Rational 2/2"),
    TIME_TWO_FOUR("Rational 2/4"),
    TIME_THREE_FOUR("Rational 3/4"),
    TIME_FIVE_FOUR("Rational 5/4"),
    TIME_THREE_EIGHT("Rational 3/8"),
    TIME_SIX_EIGHT("Rational 6/8"),

    // Octave shifts
    OTTAVA_ALTA("8 va"),
    OTTAVA_BASSA("8 vb"),
    //
    // Rests -------------------------------------------------------------------
    //
    LONG_REST("Rest for 4 measures"),
    BREVE_REST("Rest for 2 measures"),
    WHOLE_REST("Rest for whole measure"),
    HALF_REST("Rest for a 1/2"),
    QUARTER_REST("Rest for a 1/4"),
    EIGHTH_REST("Rest for a 1/8"),
    ONE_16TH_REST("Rest for a 1/16"),
    ONE_32ND_REST("Rest for a 1/32"),
    ONE_64TH_REST("Rest for a 1/64"),
    ONE_128TH_REST("Rest for a 1/128"),

    //
    // Flags -------------------------------------------------------------------
    //
    FLAG_1("Single flag down"),
    FLAG_1_UP("Single flag up"),
    FLAG_2("Double flag down"),
    FLAG_2_UP("Double flag up"),
    FLAG_3("Triple flag down"),
    FLAG_3_UP("Triple flag up"),
    FLAG_4("Quadruple flag down"),
    FLAG_4_UP("Quadruple flag up"),
    FLAG_5("Quintuple flag down"),
    FLAG_5_UP("Quintuple flag up"),

    //
    // Small Flags
    //
    SMALL_FLAG("Flag for grace note"),
    SMALL_FLAG_SLASH("Flag for slashed grace note"),

    //
    // StemLessHeads -----------------------------------------------------------
    //
    BREVE("Double Whole"),
    //
    // Articulation ------------------------------------------------------------
    //
    ACCENT,
    TENUTO,
    STACCATISSIMO,
    STRONG_ACCENT("Marcato"),
    ARPEGGIATO,

    //
    // Dynamics ----------------------------------------------------------------
    //
    //    DYNAMICS_CHAR_M("m character"),
    //    DYNAMICS_CHAR_R("r character"),
    //    DYNAMICS_CHAR_S("s character"),
    //    DYNAMICS_CHAR_Z("z character"),
    //    DYNAMICS_FFF("Fortississimo"),
    //    DYNAMICS_FZ("Forzando"),
    //    DYNAMICS_PPP("Pianississimo"),
    //    DYNAMICS_RF,
    //    DYNAMICS_RFZ("Rinforzando"),
    //    DYNAMICS_SFFZ,
    //    DYNAMICS_SFP("Subito fortepiano"),
    //    DYNAMICS_SFPP,
    DYNAMICS_P("Piano"),
    DYNAMICS_PP("Pianissimo"),
    DYNAMICS_MP("Mezzo piano"),
    DYNAMICS_F("Forte"),
    DYNAMICS_FF("Fortissimo"),
    DYNAMICS_MF("Mezzo forte"),
    DYNAMICS_FP("FortePiano"),
    DYNAMICS_SF,
    DYNAMICS_SFZ("Sforzando"),

    //
    // Ornaments ---------------------------------------------------------------
    //
    TR("Trill"),
    TURN("Turn"),
    TURN_INVERTED("Inverted Turn"),
    TURN_UP("Turn Up"),
    TURN_SLASH("Turn with a Slash"),
    MORDENT("Mordent"),
    MORDENT_INVERTED("Mordent with a Slash"),

    //
    // Tuplets -----------------------------------------------------------------
    //
    TUPLET_THREE("3"),
    TUPLET_SIX("6"),
    PEDAL_MARK("Pedal down"),
    PEDAL_UP_MARK("Pedal downup"),

    //
    // Small digits ------------------------------------------------------------
    //
    DIGIT_0("Digit 0"),
    DIGIT_1("Digit 1"),
    DIGIT_2("Digit 2"),
    DIGIT_3("Digit 3"),
    DIGIT_4("Digit 4"),

    //    DIGIT_5("Digit 5"),
    //    DIGIT_6("Digit 6"),
    //    DIGIT_7("Digit 7"),
    //    DIGIT_8("Digit 8"),
    //    DIGIT_9("Digit 9"),
    //
    // Roman numerals ----------------------------------------------------------
    //
    ROMAN_I("Roman number 1"),
    ROMAN_II("Roman number 2"),
    ROMAN_III("Roman number 3"),
    ROMAN_IV("Roman number 4"),
    ROMAN_V("Roman number 5"),
    ROMAN_VI("Roman number 6"),
    ROMAN_VII("Roman number 7"),
    ROMAN_VIII("Roman number 8"),
    ROMAN_IX("Roman number 9"),
    ROMAN_X("Roman number 10"),
    ROMAN_XI("Roman number 11"),
    ROMAN_XII("Roman number 12"),

    //
    // Plucking ----------------------------------------------------------------
    //
    PLUCK_P("Plucking pouce/pulgar/thumb"),
    PLUCK_I("Plucking index/indicio/index"),
    PLUCK_M("Plucking majeur/medio/middle"),
    PLUCK_A("Plucking annulaire/anular/ring"),
    //
    // Noteheads ---------------------------------------------------------------
    //
    NOTEHEAD_BLACK("Filled node head for quarters and less"),
    NOTEHEAD_BLACK_SMALL("Small filled note head for grace or cue"),
    NOTEHEAD_VOID("Hollow node head for halves"),
    NOTEHEAD_VOID_SMALL("Small hollow note head for grace or cue"),
    //
    // StemLessHeads -----------------------------------------------------------
    //
    WHOLE_NOTE("Hollow node head for wholes"),
    WHOLE_NOTE_SMALL("Small hollow node head for grace or cue wholes"),
    //
    // Key signatures ----------------------------------------------------------
    //
    KEY_FLAT_7("Seven Flats"),
    KEY_FLAT_6("Six Flats"),
    KEY_FLAT_5("Five Flats"),
    KEY_FLAT_4("Four Flats"),
    KEY_FLAT_3("Three Flats"),
    KEY_FLAT_2("Two Flats"),
    KEY_FLAT_1("One Flat"), // Different from flat alteration sign
    KEY_SHARP_1("One Sharp"), // Different from sharp alteration sign
    KEY_SHARP_2("Two Sharps"),
    KEY_SHARP_3("Three Sharps"),
    KEY_SHARP_4("Four Sharps"),
    KEY_SHARP_5("Five Sharps"),
    KEY_SHARP_6("Six Sharps"),
    KEY_SHARP_7("Seven Sharps"),

    //
    // Miscellaneous -----------------------------------------------------------
    //
    CLUTTER("Pure clutter"),
    /**
     * =================================================================================
     * End of physical shapes, beginning of logical shapes.
     * Shapes below will not be handled by the classifier
     * =============================================================================================
     */
    TEXT("Sequence of letters & spaces"),
    CHARACTER("Any letter"),

    //
    // Wedges ------------------------------------------------------------------
    //
    CRESCENDO("Crescendo"),
    DIMINUENDO("Diminuendo"),
    //
    // Miscellaneous
    //
    BRACE("Brace"),
    BRACKET("Bracket"),
    NOISE("Too small stuff"),
    LEDGER("Ledger"),
    ENDING_HORIZONTAL("Horizontal part of ending"),
    ENDING_VERTICAL("Vertical part of ending"),
    SEGMENT("Wedge or ending segment"),

    //
    // Stems
    //
    STEM("Stem"),
    //
    //
    // Ornaments ---------------------------------------------------------------
    //
    GRACE_NOTE_SLASH("Grace Note with a Slash"),
    GRACE_NOTE("Grace Note with no slash");

    /** Explanation of the glyph shape */
    private final String description;

    //-------//
    // Shape //
    //-------//
    Shape ()
    {
        this("");
    }

    //-------//
    // Shape //
    //-------//
    Shape (String description)
    {
        this.description = description;
    }
}
