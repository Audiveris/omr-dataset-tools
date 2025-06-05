//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  D e e p S c o r e s S h a p e                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate=,collapsed, desc=,hdr,>
//
//  Copyright © Audiveris 2022. All rights reserved.
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
package org.audiveris.omrdataset.api;

import java.util.EnumSet;

/**
 * Class <code>DeepScoresShape</code> gathers all shapes used in DeepScores dataset,
 * with the addition of the 'none' shape.
 *
 * @author Hervé Bitteur
 */
public enum DeepScoresShape
{
    none,

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
    dynamicP,
    dynamicM,
    dynamicF,
    dynamicS,
    dynamicZ,
    dynamicR,
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

    //~ Static fields/initializers -----------------------------------------------------------------
    public static final EnumSet<DeepScoresShape> Ignored = EnumSet.of(
            ledgerLine,
            noteheadBlackOnLineSmall,
            noteheadBlackInSpaceSmall,
            noteheadHalfOnLineSmall,
            noteheadHalfInSpaceSmall,
            noteheadWholeOnLineSmall,
            noteheadWholeInSpaceSmall,
            noteheadDoubleWholeOnLineSmall,
            noteheadDoubleWholeInSpaceSmall,
            stem,
            flag8thUpSmall,
            flag8thDownSmall,
            accidentalFlatSmall,
            accidentalNaturalSmall,
            accidentalSharpSmall,
            graceNoteAcciaccaturaStemUp,
            graceNoteAppoggiaturaStemUp,
            graceNoteAcciaccaturaStemDown,
            graceNoteAppoggiaturaStemDown,
            slur,
            beam,
            tie,
            dynamicCrescendoHairpin,
            dynamicDiminuendoHairpin,
            staff,
            ottavaBracket);

    //~ Methods ------------------------------------------------------------------------------------
    //-----------//
    // isIgnored //
    //-----------//
    public boolean isIgnored ()
    {
        return Ignored.contains(this);
    }

    //-------------//
    // toHeadShape //
    //-------------//
    public HeadShape toHeadShape ()
    {
        switch (this) {
        default:
        case none:
            return HeadShape.none;

        case noteheadBlackOnLine:
            return HeadShape.noteheadBlack;
        case noteheadBlackOnLineSmall:
            return HeadShape.noteheadBlackSmall;
        case noteheadBlackInSpace:
            return HeadShape.noteheadBlack;
        case noteheadBlackInSpaceSmall:
            return HeadShape.noteheadBlackSmall;
        case noteheadHalfOnLine:
            return HeadShape.noteheadHalf;
        case noteheadHalfOnLineSmall:
            return HeadShape.noteheadHalfSmall;
        case noteheadHalfInSpace:
            return HeadShape.noteheadHalf;
        case noteheadHalfInSpaceSmall:
            return HeadShape.noteheadHalfSmall;
        case noteheadWholeOnLine:
            return HeadShape.noteheadWhole;
        case noteheadWholeOnLineSmall:
            return HeadShape.noteheadWholeSmall;
        case noteheadWholeInSpace:
            return HeadShape.noteheadWhole;
        case noteheadWholeInSpaceSmall:
            return HeadShape.noteheadWholeSmall;
        case noteheadDoubleWholeOnLine:
            return HeadShape.noteheadDoubleWhole;
        case noteheadDoubleWholeOnLineSmall:
            return HeadShape.noteheadDoubleWholeSmall;
        case noteheadDoubleWholeInSpace:
            return HeadShape.noteheadDoubleWhole;
        case noteheadDoubleWholeInSpaceSmall:
            return HeadShape.noteheadDoubleWholeSmall;
        }
    }

    //----------------//
    // toGeneralShape //
    //----------------//
    public GeneralShape toGeneralShape ()
    {
        if (isIgnored()) {
            return null;
        }

        return GeneralShape.valueOf(toString());
    }
}
