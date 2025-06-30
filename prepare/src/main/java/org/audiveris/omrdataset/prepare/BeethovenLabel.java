//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   B e e t h o v e n L a b e l                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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
package org.audiveris.omrdataset.prepare;

/**
 * Class <code>BeethovenLabel</code>
 *
 * @author Hervé Bitteur
 */
public enum BeethovenLabel
{
    grpSymBracket, // bracket
    system, // System starting barline
    systemBoundingBox, // Bounding box of whole system entities

    ledgerLine,
    staffLine,

    barlineSingle,
    barlineDouble,
    barlineHeavy,

    barlineRepeatBoth,
    barlineRepeatEnd,
    barlineRepeatStart,
    voltaBracket, // Alternate ending

    gClef,
    cClef,
    fClef,

    timeSigCut, // timeSigCutCommon

    noteheadBlack,
    noteheadHalf,
    noteheadWhole,

    dot, // augmentation dot

    stem,

    flag8thUp,
    flag8thDown,
    flag16thUp,
    flag16thDown,

    accidentalFlat,
    accidentalNatural,
    accidentalSharp,
    keyFlat,
    keySharp,

    articStaccato,

    fermataAbove,
    fermataBelow,

    restWhole,
    restHalf,
    restQuarter,
    rest8th,
    rest16th,

    octaveUp, // The octave dashed line following the 8/15/22 symbol (until a hook?)

    dynamicF, // f original: dynam-f ATTENTION
    dynamicP, // p original: dynam-p ATTENTION
    dynamicFF, // ff
    dynamicFortePiano, // fp
    dynamicPP, // pp
    dynamicSforzando1, // sf
    dynamicSforzandoPiano, // sfp
    hairpinCrescendo, // <
    hairpinDiminuendo, // >

    trillSig, // tr, ornamentTrill
    ornamentTrill, // ornamentTremblement (like a long mordent)
    ornamentTurn,

    beam,
    slur,
    tie,

    // Miscellaneous
    word,
    charFullStop,

    dirDash, // A line of spaced dashes, following a direction

    digit0,
    digit1,
    digit2,
    digit3,
    digit4,
    digit5,
    digit6,
    digit7,
    digit8,
    digit9,

    letter_a,
    letter_c,
    letter_cA,
    letter_cP,
    letter_d,
    letter_e,
    letter_g,
    letter_i,
    letter_l,
    letter_m,
    letter_o,
    letter_p,
    letter_r,
    letter_s,
    letter_t,
    letter_z;

    //~ Static Methods -----------------------------------------------------------------------------

    /**
     * Report the YoloLabel, if any, equivalent to the provided BeethovenLabel.
     *
     * @param label the provided label from Beethoven
     * @return the corresponding YoloLabel, null if none
     */
    public static YoloLabel of (BeethovenLabel label)
    {
        return switch (label) {
            case grpSymBracket -> YoloLabel.bracket;
            case system -> null; // System barline
            case systemBoundingBox -> null;

            case staffLine -> null;

            case barlineRepeatBoth -> YoloLabel.repeatRightLeft;
            case barlineRepeatEnd -> YoloLabel.repeatRight;
            case barlineRepeatStart -> YoloLabel.repeatLeft;
            case voltaBracket -> null;

            case timeSigCut -> YoloLabel.timeSigCutCommon;

            case dot -> YoloLabel.augmentationDot;

            case keyFlat -> YoloLabel.accidentalFlat; // Not really, but...
            case keySharp -> YoloLabel.accidentalSharp;

            case octaveUp -> null; // Line following YoloLabel.ottava

            case dynamicFF -> null;
            case dynamicFortePiano -> null;
            case dynamicPP -> null;
            case dynamicSforzando1 -> null;
            case dynamicSforzandoPiano -> null;

            case hairpinCrescendo -> YoloLabel.dynamicCrescendoHairpin;
            case hairpinDiminuendo -> YoloLabel.dynamicDiminuendoHairpin;

            case trillSig -> YoloLabel.ornamentTrill;
            case ornamentTrill -> YoloLabel.ornamentMordent; // or null perhaps?

            case word -> null;
            case charFullStop -> null;

            case dirDash -> null;

            case digit0 -> null;
            case digit1 -> null;
            case digit2 -> null;
            case digit3 -> null;
            case digit4 -> null;
            case digit5 -> null;
            case digit6 -> null;
            case digit7 -> null;
            case digit8 -> null;
            case digit9 -> null;

            case letter_a -> null;
            case letter_c -> null;
            case letter_cA -> null;
            case letter_cP -> null;
            case letter_d -> null;
            case letter_e -> null;
            case letter_g -> null;
            case letter_i -> null;
            case letter_l -> null;
            case letter_m -> null;
            case letter_o -> null;
            case letter_p -> null;
            case letter_r -> null;
            case letter_s -> null;
            case letter_t -> null;
            case letter_z -> null;

            default -> YoloLabel.valueOf(label.name());
        };
    }
}

// Classes based on instances found in Beethoven corpus:

// accidentalFlat
// accidentalNatural
// accidentalSharp
// articStaccato
// barlineDouble
// barlineHeavy
// barlineRepeatBoth
// barlineRepeatEnd
// barlineRepeatStart
// barlineSingle
// beam
// cClef
// charFullStop
// digit0
// digit1
// digit2
// digit3
// digit4
// digit5
// digit6
// digit7
// digit8
// digit9
// dirDash
// dot
// dynam-f
// dynam-p
// dynamicFF
// dynamicFortePiano
// dynamicPP
// dynamicSforzando1
// dynamicSforzandoPiano
// fClef
// fermataAbove
// flag16thUp
// flag8thDown
// flag8thUp
// gClef
// grpSymBracket
// hairpinCrescendo
// hairpinDiminuendo
// keyFlat
// ledgerLine
// letter_a
// letter_c
// letter_cA
// letter_cP
// letter_d
// letter_e
// letter_g
// letter_i
// letter_l
// letter_m
// letter_o
// letter_p
// letter_r
// letter_s
// letter_t
// letter_z
// noteheadBlack
// noteheadHalf
// noteheadWhole
// octaveUp
// ornamentTrill
// ornamentTurn
// rest16th
// rest8th
// restHalf
// restQuarter
// restWhole
// slur
// staffLine
// stem
// system
// systemBoundingBox
// tie
// timeSigCut
// trillSig
// voltaBracket
// word
