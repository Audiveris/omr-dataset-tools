//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  D e e p S c o r e s L a b e l                                 //
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
 * Class <code>DeepScoresLabel</code> gathers the labels used in DeepScores V2.
 * <p>
 * This is a list <em>derived</em> from the original "categories" found in the DeepScores-XXXX.json
 * files, with an important modification regarding the dynamics names:
 * <ul>
 * <li>The original list contained only the six 1-letter dynamic symbols:
 * dynamicP,
 * dynamicM,
 * dynamicF,
 * dynamicS,
 * dynamicZ,
 * dynamicR.
 * <li>The modified list contains compound dynamic symbols:
 * dynamicPPPP,
 * dynamicPPP,
 * dynamicPP,
 * dynamicP,
 * dynamicMP,
 * dynamicMF,
 * dynamicPF,
 * dynamicF,
 * dynamicFF,
 * dynamicFFF,
 * dynamicFFFF,
 * dynamicFP,
 * dynamicFZ,
 * dynamicSF,
 * dynamicSFP,
 * dynamicSFPP,
 * dynamicSFZ,
 * dynamicSFZP,
 * dynamicSFFZ,
 * dynamicRF,
 * dynamicRFZ.
 * </ul>
 *
 * @author Hervé Bitteur
 */
public enum DeepScoresLabel
{
    brace,

    ledgerLine,

    repeatDot,
    segno,
    coda,

    clefG,
    clefCAlto,
    clefCTenor,
    clefF,
    clefUnpitchedPercussion,
    clef8,
    clef15,

    timeSig0,
    timeSig1,
    timeSig2,
    timeSig3,
    timeSig4,
    timeSig5,
    timeSig6,
    timeSig7,
    timeSig8,
    timeSig9,
    timeSigCommon,
    timeSigCutCommon,

    noteheadBlackOnLine,
    noteheadBlackOnLineSmall,
    noteheadBlackInSpace,
    noteheadBlackInSpaceSmall,
    noteheadHalfOnLine,
    noteheadHalfOnLineSmall,
    noteheadHalfInSpace,
    noteheadHalfInSpaceSmall,
    noteheadWholeOnLine,
    noteheadWholeOnLineSmall,
    noteheadWholeInSpace,
    noteheadWholeInSpaceSmall,
    noteheadDoubleWholeOnLine,
    noteheadDoubleWholeOnLineSmall,
    noteheadDoubleWholeInSpace,
    noteheadDoubleWholeInSpaceSmall,

    augmentationDot,

    stem,

    tremolo1,
    tremolo2,
    tremolo3,
    tremolo4,
    tremolo5,

    flag8thUp,
    flag8thUpSmall,
    flag16thUp,
    flag32ndUp,
    flag64thUp,
    flag128thUp,
    flag8thDown,
    flag8thDownSmall,
    flag16thDown,
    flag32ndDown,
    flag64thDown,
    flag128thDown,

    accidentalFlat,
    accidentalFlatSmall,
    accidentalNatural,
    accidentalNaturalSmall,
    accidentalSharp,
    accidentalSharpSmall,
    accidentalDoubleSharp,
    accidentalDoubleFlat,

    keyFlat,
    keyNatural,
    keySharp,

    articAccentAbove,
    articAccentBelow,
    articStaccatoAbove,
    articStaccatoBelow,
    articTenutoAbove,
    articTenutoBelow,
    articStaccatissimoAbove,
    articStaccatissimoBelow,
    articMarcatoAbove,
    articMarcatoBelow,

    fermataAbove,
    fermataBelow,
    caesura,

    restDoubleWhole,
    restWhole,
    restHalf,
    restQuarter,
    rest8th,
    rest16th,
    rest32nd,
    rest64th,
    rest128th,
    restHNr,

    //    dynamicP,
    //    dynamicM,
    //    dynamicF,
    //    dynamicS,
    //    dynamicZ,
    //    dynamicR,
    dynamicPPPPPP, // ?
    dynamicPPPPP, // ?
    dynamicPPPP, // ?
    dynamicPPP, // pianississimo
    dynamicPP, // pianissimo
    dynamicP, // piano
    dynamicM, // mezzo
    dynamicMP, // mezzo piano
    dynamicMF, // mezzo forte
    dynamicPF, // piano forte
    dynamicF, // forte
    dynamicFF, // fortissimo
    dynamicFFF, // fortississimo
    dynamicFFFF, // ?
    dynamicFFFFF, // ?
    dynamicFFFFFF, // ?
    dynamicFP, // forte piano
    dynamicFZ, // forzando
    dynamicSF, // sforzando
    dynamicSFP, // sforzandoPiano
    dynamicSFPP, // sforzandoPianissimo
    dynamicSFZ, // sforzato
    dynamicSFZP, // sforzatoPiano
    dynamicSFF, // Abbreviation of SFFZ...
    dynamicSFFZ, // sforzatoFF
    dynamicRF, //  rinforzando1
    dynamicRFZ, // rinforzando2

    graceNoteAcciaccaturaStemUp,
    graceNoteAppoggiaturaStemUp,
    graceNoteAcciaccaturaStemDown,
    graceNoteAppoggiaturaStemDown,
    ornamentTrill,
    ornamentTurn,
    ornamentTurnInverted,
    ornamentMordent,

    stringsDownBow,
    stringsUpBow,

    arpeggiato,

    keyboardPedalPed,
    keyboardPedalUp,

    tuplet3,
    tuplet6,

    fingering0,
    fingering1,
    fingering2,
    fingering3,
    fingering4,
    fingering5,

    slur,
    beam,
    tie,

    restHBar,

    dynamicCrescendoHairpin,
    dynamicDiminuendoHairpin,

    tuplet1,
    tuplet2,
    tuplet4,
    tuplet5,
    tuplet7,
    tuplet8,
    tuplet9,
    tupletBracket,

    staff,

    ottavaBracket;

    //~ Static Methods -----------------------------------------------------------------------------

    /**
     * Report the YoloLabel, if any, equivalent to the provided DeepScoresLabel.
     *
     * @param label the provided label from DeepScores
     * @return the corresponding YoloLabel, null if none
     */
    public static YoloLabel of (DeepScoresLabel label)
    {
        return switch (label) {

            case repeatDot -> null;

            case clefG -> YoloLabel.gClef;
            case clefCAlto, clefCTenor -> YoloLabel.cClef;
            case clefF -> YoloLabel.fClef;
            case clefUnpitchedPercussion -> YoloLabel.unpitchedPercussionClef1;

            // No inSpace vs onLine
            case noteheadBlackInSpace, noteheadBlackOnLine -> YoloLabel.noteheadBlack;
            case noteheadHalfInSpace, noteheadHalfOnLine -> YoloLabel.noteheadHalf;
            case noteheadWholeInSpace, noteheadWholeOnLine -> YoloLabel.noteheadWhole;
            case noteheadDoubleWholeInSpace, noteheadDoubleWholeOnLine -> YoloLabel.noteheadDoubleWhole;

            // Removed small shapes
            case noteheadBlackOnLineSmall -> null;
            case noteheadBlackInSpaceSmall -> null;
            case noteheadHalfOnLineSmall -> null;
            case noteheadHalfInSpaceSmall -> null;
            case noteheadWholeOnLineSmall -> null;
            case noteheadWholeInSpaceSmall -> null;
            case noteheadDoubleWholeOnLineSmall -> null;
            case noteheadDoubleWholeInSpaceSmall -> null;

            case flag8thUpSmall -> null;
            case flag8thDownSmall -> null;

            case accidentalFlatSmall -> null;
            case accidentalNaturalSmall -> null;
            case accidentalSharpSmall -> null;

            // Irrelevant above / below
            case articAccentAbove, articAccentBelow -> YoloLabel.articAccent;
            case articStaccatoAbove, articStaccatoBelow -> YoloLabel.articStaccato;
            case articTenutoAbove, articTenutoBelow -> YoloLabel.articTenuto;

            // Don't know what this is
            case restHNr -> null;

            // Abbreviated dynamic
            case dynamicSFF -> null;

            // Unreliable, and useless in fact
            case staff -> null;

            default -> YoloLabel.valueOf(label.name());
        };
    }
}
