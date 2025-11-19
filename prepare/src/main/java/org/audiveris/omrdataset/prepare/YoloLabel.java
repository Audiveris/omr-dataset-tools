//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        Y o l o L a b e l                                       //
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
 * Class <code>YoloLabel</code> represents the labels handled by a YOLO model.
 * <p>
 * We try to stick as much as possible with the SMuFL naming.
 *
 * @see https://w3c.github.io/smufl/latest/tables/index.html
 * @see https://github.com/w3c/smufl/blob/gh-pages/metadata/glyphnames.json
 * @author Hervé Bitteur
 */
public enum YoloLabel
{
    // 4.1 Staff brackets and dividers
    brace,
    bracket,

    // 4.2 Staves
    ledgerLine,

    // 4.3 Barlines
    barlineSingle,
    barlineDouble,
    barlineHeavy,

    // 4.4 Repeats
    repeatLeft,
    repeatRight,
    repeatRightLeft,
    segno,
    coda,

    // 4.5 Clefs
    gClef,
    cClef, // TODO: Should differentiate Alto or Tenor
    fClef,
    unpitchedPercussionClef1,
    clef8,
    clef15,

    // 4.6 Time signatures
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

    // 4.7 Noteheads
    noteheadBlack,
    noteheadHalf,
    noteheadWhole,
    noteheadDoubleWhole,

    // 4.14
    augmentationDot,

    // 4.15 Stems
    stem,

    // 4.16 Tremolos
    tremolo1,
    tremolo2,
    tremolo3,
    tremolo4,
    tremolo5,

    // 4.17 Flags
    flag8thUp,
    flag8thDown,
    flag16thUp,
    flag16thDown,
    flag32ndUp,
    flag32ndDown,
    flag64thUp,
    flag64thDown,
    flag128thUp,
    flag128thDown,

    // 4.18 Standard accidentals
    accidentalFlat,
    accidentalNatural,
    accidentalSharp,
    accidentalDoubleSharp,
    accidentalDoubleFlat,
    keyFlat,
    keyNatural,
    keySharp,

    // 4.39 Articulations
    articAccent, // TODO: Could differentiate Above or Below
    articStaccato, // TODO: Could differentiate Above or Below
    articTenuto, // TODO: Could differentiate Above or Below
    articStaccatissimoAbove,
    articStaccatissimoBelow,
    articMarcatoAbove,
    articMarcatoBelow,

    // 4.40 Holds and pauses
    fermataAbove,
    fermataBelow,
    caesura,

    // 4.41 Rests
    restDoubleWhole,
    restWhole,
    restHalf,
    restQuarter,
    rest8th,
    rest16th,
    rest32nd,
    rest64th,
    rest128th,
    restHBar,

    // 4.42 Bar repeats
    repeat1Bar,
    repeat2Bars,
    repeat4Bars,

    // 4.43 Octaves
    ottava, // '8'
    quindicesima, // '15'
    ottavaBracket, // the following dashes, including the final hook if any

    // 4.44 Dynamics
    dynamicPPPPPP, //
    dynamicPPPPP, //
    dynamicPPPP, //
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
    dynamicFFFF, //
    dynamicFFFFF, //
    dynamicFFFFFF, //
    dynamicFP, // forte piano
    dynamicFZ, // forzando
    dynamicSF, // sforzando
    dynamicSFP, // sforzandoPiano
    dynamicSFPP, // sforzandoPianissimo
    dynamicSFZ, // sforzato
    dynamicSFZP, // sforzatoPiano
    dynamicSFFZ, // sforzatoFF
    dynamicRF, //  rinforzando1
    dynamicRFZ, // rinforzando2
    dynamicCrescendoHairpin, // <
    dynamicDiminuendoHairpin, // >

    // 4.46 Common ornaments
    graceNoteAcciaccaturaStemUp,
    graceNoteAcciaccaturaStemDown,
    graceNoteAppoggiaturaStemUp,
    graceNoteAppoggiaturaStemDown,
    ornamentTrill,
    ornamentTurn,
    ornamentTurnInverted,
    ornamentTurnSlash,
    ornamentTurnUp,
    ornamentMordent,

    // 4.52 String techniques
    stringsDownBow,
    stringsUpBow,

    // 4.53 Plucked techniques
    arpeggiato,

    // 4.55 Keyboard techniques
    keyboardPedalPed,
    keyboardPedalUp,

    // 4.75 Tuplets
    tuplet1,
    tuplet2,
    tuplet3,
    tuplet4,
    tuplet5,
    tuplet6,
    tuplet7,
    tuplet8,
    tuplet9,
    tupletBracket,

    // 4.78 Beams and slurs
    beam,
    slur,
    tie,

    // 4.115 Fingering
    fingering0,
    fingering1,
    fingering2,
    fingering3,
    fingering4,
    fingering5;
}
