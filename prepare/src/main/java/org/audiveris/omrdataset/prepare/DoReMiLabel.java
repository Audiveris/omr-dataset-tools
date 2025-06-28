//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      D o R e M i L a b e l                                     //
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
 * Class <code>DoReMiLabel</code> gathers the labels used in DoReMi V1.
 *
 * @author Hervé Bitteur
 */
public enum DoReMiLabel
{
    gClef,
    cClef,
    fClef,
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
    noteheadBlack,
    noteheadHalf,
    noteheadWhole,
    augmentationDot,
    stem,
    flag8thUp,
    flag16thUp,
    flag32ndUp,
    flag8thDown,
    flag16thDown,
    flag32ndDown,
    accidentalFlat,
    accidentalNatural,
    accidentalSharp,
    accidentalDoubleSharp,
    accidentalDoubleFlat,
    accidentalQuarterToneFlatStein,
    accidentalQuarterToneSharpStein,
    accidentalThreeQuarterTonesSharpStein,
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
    restWhole,
    restHalf,
    restQuarter,
    rest8th,
    rest16th,
    rest32nd,
    dynamicPiano,
    dynamicPP,
    dynamicPPP,
    dynamicForte,
    dynamicFF,
    dynamicFFF,
    dynamicMP,
    dynamicMF,
    dynamicFortePiano,
    dynamicForzando,
    dynamicSforzato,
    dynamicText,
    gradualDynamic,
    ornamentTrill,
    barline,
    slur,
    beam,
    tie,
    kStaffLine,
    systemicBarline,
    timeSignatureComponent,
    tupletBracket,
    tupletText;

    //~ Static Methods -----------------------------------------------------------------------------

    /**
     * Report the YoloLabel, if any, equivalent to the provided DoReMiLabel.
     *
     * @param label the provided label from DoReMi
     * @return the corresponding YoloLabel, null if none
     */
    public static YoloLabel of (DoReMiLabel label)
    {
        return switch (label) {

            case gClef -> YoloLabel.clefG;
            case cClef -> null; // YoloLabel.clefCAlto or YoloLabel.clefCTenor;
            case fClef -> YoloLabel.clefF;

            case noteheadBlack -> null; // YoloLabel.noteheadBlackOnLine or YoloLabel.noteheadBlackInSpace
            case noteheadHalf -> null; // YoloLabel.noteheadHalfOnLine or YoloLabel.noteheadHalfInSpace
            case noteheadWhole -> null; // YoloLabel.noteheadWholeOnLine or YoloLabel.noteheadWholeInSpace

            case accidentalQuarterToneFlatStein -> null;
            case accidentalQuarterToneSharpStein -> null;
            case accidentalThreeQuarterTonesSharpStein -> null;

            case dynamicPiano -> YoloLabel.dynamicP;
            case dynamicPP -> null;
            case dynamicPPP -> null;
            case dynamicForte -> YoloLabel.dynamicF;
            case dynamicFF -> null;
            case dynamicFFF -> null;
            case dynamicMP -> null;
            case dynamicMF -> null;
            case dynamicFortePiano -> null;
            case dynamicForzando -> null;
            case dynamicSforzato -> null;
            case dynamicText -> null;
            case gradualDynamic -> null;

            case barline -> null; // Interesting?
            case systemicBarline -> null; // Interesting?
            case tupletText -> null;
            case timeSignatureComponent -> null;
            case kStaffLine -> null;

            default -> YoloLabel.valueOf(label.name());
        };
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //Classes found in DoReMi train/val:

    //accidentalDoubleFlat
    //accidentalDoubleSharp
    //accidentalFlat
    //accidentalNatural
    //accidentalQuarterToneFlatStein
    //accidentalQuarterToneSharpStein
    //accidentalSharp
    //accidentalThreeQuarterTonesSharpStein
    //articAccentAbove
    //articAccentBelow
    //articMarcatoAbove
    //articMarcatoBelow
    //articStaccatissimoAbove
    //articStaccatissimoBelow
    //articStaccatoAbove
    //articStaccatoBelow
    //articTenutoAbove
    //articTenutoBelow
    //augmentationDot
    //barline
    //beam
    //cClef
    //dynamicFF
    //dynamicFFF
    //dynamicForte
    //dynamicFortePiano
    //dynamicForzando
    //dynamicMF
    //dynamicMP
    //dynamicPP
    //dynamicPPP
    //dynamicPiano
    //dynamicSforzato
    //dynamicText
    //fClef
    //flag16thDown
    //flag16thUp
    //flag32ndDown
    //flag32ndUp
    //flag8thDown
    //flag8thUp
    //gClef
    //gradualDynamic
    //kStaffLine
    //noteheadBlack
    //noteheadHalf
    //noteheadWhole
    //ornamentTrill
    //rest16th
    //rest32nd
    //rest8th
    //restHalf
    //restQuarter
    //restWhole
    //slur
    //stem
    //systemicBarline
    //tie
    //timeSig2
    //timeSig3
    //timeSig4
    //timeSig5
    //timeSig6
    //timeSig7
    //timeSig8
    //timeSig9
    //timeSigCommon
    //timeSigCutCommon
    //timeSignatureComponent
    //tupletBracket
    //tupletText

}
