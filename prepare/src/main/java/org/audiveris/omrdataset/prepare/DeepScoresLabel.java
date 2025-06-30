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
 *
 * @author Hervé Bitteur
 */
public enum DeepScoresLabel
{
    brace, //  0
    ledgerLine, //  1
    repeatDot, //  2
    segno, //  3
    coda, //  4
    clefG, //  5
    clefCAlto, //  6
    clefCTenor, //  7
    clefF, //  8
    clefUnpitchedPercussion, //  9
    clef8, //  10
    clef15, //  11
    timeSig0, //  12
    timeSig1, //  13
    timeSig2, //  14
    timeSig3, //  15
    timeSig4, //  16
    timeSig5, //  17
    timeSig6, //  18
    timeSig7, //  19
    timeSig8, //  20
    timeSig9, //  21
    timeSigCommon, //  22
    timeSigCutCommon, //  23
    noteheadBlackOnLine, //  24
    noteheadBlackOnLineSmall, //  25
    noteheadBlackInSpace, //  26
    noteheadBlackInSpaceSmall, //  27
    noteheadHalfOnLine, //  28
    noteheadHalfOnLineSmall, //  29
    noteheadHalfInSpace, //  30
    noteheadHalfInSpaceSmall, //  31
    noteheadWholeOnLine, //  32
    noteheadWholeOnLineSmall, //  33
    noteheadWholeInSpace, //  34
    noteheadWholeInSpaceSmall, //  35
    noteheadDoubleWholeOnLine, //  36
    noteheadDoubleWholeOnLineSmall, //  37
    noteheadDoubleWholeInSpace, //  38
    noteheadDoubleWholeInSpaceSmall, //  39
    augmentationDot, //  40
    stem, //  41
    tremolo1, //  42
    tremolo2, //  43
    tremolo3, //  44
    tremolo4, //  45
    tremolo5, //  46
    flag8thUp, //  47
    flag8thUpSmall, //  48
    flag16thUp, //  49
    flag32ndUp, //  50
    flag64thUp, //  51
    flag128thUp, //  52
    flag8thDown, //  53
    flag8thDownSmall, //  54
    flag16thDown, //  55
    flag32ndDown, //  56
    flag64thDown, //  57
    flag128thDown, //  58
    accidentalFlat, //  59
    accidentalFlatSmall, //  60
    accidentalNatural, //  61
    accidentalNaturalSmall, //  62
    accidentalSharp, //  63
    accidentalSharpSmall, //  64
    accidentalDoubleSharp, //  65
    accidentalDoubleFlat, //  66
    keyFlat, //  67
    keyNatural, //  68
    keySharp, //  69
    articAccentAbove, //  70
    articAccentBelow, //  71
    articStaccatoAbove, //  72
    articStaccatoBelow, //  73
    articTenutoAbove, //  74
    articTenutoBelow, //  75
    articStaccatissimoAbove, //  76
    articStaccatissimoBelow, //  77
    articMarcatoAbove, //  78
    articMarcatoBelow, //  79
    fermataAbove, //  80
    fermataBelow, //  81
    caesura, //  82
    restDoubleWhole, //  83
    restWhole, //  84
    restHalf, //  85
    restQuarter, //  86
    rest8th, //  87
    rest16th, //  88
    rest32nd, //  89
    rest64th, //  90
    rest128th, //  91
    restHNr, //  92
    dynamicP, //  93
    dynamicM, //  94
    dynamicF, //  95
    dynamicS, //  96
    dynamicZ, //  97
    dynamicR, //  98
    graceNoteAcciaccaturaStemUp, //  99
    graceNoteAppoggiaturaStemUp, //  100
    graceNoteAcciaccaturaStemDown, //  101
    graceNoteAppoggiaturaStemDown, //  102
    ornamentTrill, //  103
    ornamentTurn, //  104
    ornamentTurnInverted, //  105
    ornamentMordent, //  106
    stringsDownBow, //  107
    stringsUpBow, //  108
    arpeggiato, //  109
    keyboardPedalPed, //  110
    keyboardPedalUp, //  111
    tuplet3, //  112
    tuplet6, //  113
    fingering0, //  114
    fingering1, //  115
    fingering2, //  116
    fingering3, //  117
    fingering4, //  118
    fingering5, //  119
    slur, //  120
    beam, //  121
    tie, //  122
    restHBar, //  123
    dynamicCrescendoHairpin, //  124
    dynamicDiminuendoHairpin, //  125
    tuplet1, //  126
    tuplet2, //  127
    tuplet4, //  128
    tuplet5, //  129
    tuplet7, //  130
    tuplet8, //  131
    tuplet9, //  132
    tupletBracket, //  133
    staff, //  134
    ottavaBracket; //  135

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

            //    keyFlat ?
            //    keyNatural ?
            //    keySharp ?

            // Irrelevant above / below
            case articAccentAbove, articAccentBelow -> YoloLabel.articAccent;
            case articStaccatoAbove, articStaccatoBelow -> YoloLabel.articStaccato;
            case articTenutoAbove, articTenutoBelow -> YoloLabel.articTenuto;

            // Don't know what this is
            case restHNr -> null;

            //    dynamicP, //  ?
            //    dynamicM, //
            //    dynamicF, //
            //    dynamicS, //
            //    dynamicZ, //
            //    dynamicR, //

            // Unreliable, and useless in fact
            case staff -> null;

            default -> YoloLabel.valueOf(label.name());
        };
    }
}
