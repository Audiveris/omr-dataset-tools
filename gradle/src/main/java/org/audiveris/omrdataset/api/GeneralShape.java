//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     G e n e r a l S h a p e                                    //
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
package org.audiveris.omrdataset.api;

/**
 * Class {@code GeneralShape} is a subset of OmrShape, meant for general processing
 * outside of head processing.
 * <p>
 * This is meant to be a small first list, just to validate its integration and efficiency in OMR.
 *
 * @author Hervé Bitteur
 */
public enum GeneralShape
{
    none,

    brace,
    repeatDot,
    gClef,
    cClefAlto,
    cClefTenor,
    fClef,
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
    noteheadBlack,
    noteheadBlackSmall,
    noteheadHalf,
    noteheadWhole,
    flag8thUp,
    flag16thUp,
    flag32ndUp,
    flag8thDown,
    flag16thDown,
    flag32ndDown,
    accidentalFlat,
    accidentalNatural,
    accidentalSharp,
    keyFlat,
    keyNatural,
    keySharp,
    restWhole,
    restHalf,
    restQuarter,
    rest8th,
    rest16th,
    rest32nd,
    tuplet3,
    tuplet6;

    //~ Methods ------------------------------------------------------------------------------------
    //------------//
    // toOmrShape //
    //------------//
    public OmrShape toOmrShape ()
    {
        switch (this) {
        default:
        case none:
            return OmrShape.none;
        case brace:
            return OmrShape.brace;
        case repeatDot:
            return OmrShape.repeatDot;
        case gClef:
            return OmrShape.gClef;
        case cClefAlto:
            return OmrShape.cClefAlto;
        case cClefTenor:
            return OmrShape.cClefTenor;
        case fClef:
            return OmrShape.fClef;
        case timeSig0:
            return OmrShape.timeSig0;
        case timeSig1:
            return OmrShape.timeSig1;
        case timeSig2:
            return OmrShape.timeSig2;
        case timeSig3:
            return OmrShape.timeSig3;
        case timeSig4:
            return OmrShape.timeSig4;
        case timeSig5:
            return OmrShape.timeSig5;
        case timeSig6:
            return OmrShape.timeSig6;
        case timeSig7:
            return OmrShape.timeSig7;
        case timeSig8:
            return OmrShape.timeSig8;
        case timeSig9:
            return OmrShape.timeSig9;
        case timeSigCommon:
            return OmrShape.timeSigCommon;
        case timeSigCutCommon:
            return OmrShape.timeSigCutCommon;
        case noteheadBlack:
            return OmrShape.noteheadBlack;
        case noteheadBlackSmall:
            return OmrShape.noteheadBlackSmall;
        case noteheadHalf:
            return OmrShape.noteheadHalf;
        case noteheadWhole:
            return OmrShape.noteheadWhole;
        case flag8thUp:
            return OmrShape.flag8thUp;
        case flag16thUp:
            return OmrShape.flag16thUp;
        case flag32ndUp:
            return OmrShape.flag32ndUp;
        case flag8thDown:
            return OmrShape.flag8thDown;
        case flag16thDown:
            return OmrShape.flag16thDown;
        case flag32ndDown:
            return OmrShape.flag32ndDown;
        case accidentalFlat:
            return OmrShape.accidentalFlat;
        case accidentalNatural:
            return OmrShape.accidentalNatural;
        case accidentalSharp:
            return OmrShape.accidentalSharp;
        case keyFlat:
            return OmrShape.keyFlat;
        case keyNatural:
            return OmrShape.keyNatural;
        case keySharp:
            return OmrShape.keySharp;
        case restWhole:
            return OmrShape.restWhole;
        case restHalf:
            return OmrShape.restHalf;
        case restQuarter:
            return OmrShape.restQuarter;
        case rest8th:
            return OmrShape.rest8th;
        case rest16th:
            return OmrShape.rest16th;
        case rest32nd:
            return OmrShape.rest32nd;
        case tuplet3:
            return OmrShape.tuplet3;
        case tuplet6:
            return OmrShape.tuplet6;
        }
    }

    //----------------//
    // toGeneralShape //
    //----------------//
    public static GeneralShape toGeneralShape (OmrShape omrShape)
    {
        if (omrShape == null) {
            return null;
        }

        switch (omrShape) {
        case none:
            return none;
        case brace:
            return brace;
        case repeatDot:
            return repeatDot;
        case gClef:
            return gClef;
        case cClefAlto:
            return cClefAlto;
        case cClefTenor:
            return cClefTenor;
        case fClef:
            return fClef;
        case timeSig0:
            return timeSig0;
        case timeSig1:
            return timeSig1;
        case timeSig2:
            return timeSig2;
        case timeSig3:
            return timeSig3;
        case timeSig4:
            return timeSig4;
        case timeSig5:
            return timeSig5;
        case timeSig6:
            return timeSig6;
        case timeSig7:
            return timeSig7;
        case timeSig8:
            return timeSig8;
        case timeSig9:
            return timeSig9;
        case timeSigCommon:
            return timeSigCommon;
        case timeSigCutCommon:
            return timeSigCutCommon;
        case noteheadBlack:
            return noteheadBlack;
        case noteheadBlackSmall:
            return noteheadBlackSmall;
        case noteheadHalf:
            return noteheadHalf;
        case noteheadWhole:
            return noteheadWhole;
        case flag8thUp:
            return flag8thUp;
        case flag16thUp:
            return flag16thUp;
        case flag32ndUp:
            return flag32ndUp;
        case flag8thDown:
            return flag8thDown;
        case flag16thDown:
            return flag16thDown;
        case flag32ndDown:
            return flag32ndDown;
        case accidentalFlat:
            return accidentalFlat;
        case accidentalNatural:
            return accidentalNatural;
        case accidentalSharp:
            return accidentalSharp;
        case keyFlat:
            return keyFlat;
        case keyNatural:
            return keyNatural;
        case keySharp:
            return keySharp;
        case restWhole:
            return restWhole;
        case restHalf:
            return restHalf;
        case restQuarter:
            return restQuarter;
        case rest8th:
            return rest8th;
        case rest16th:
            return rest16th;
        case rest32nd:
            return rest32nd;
        case tuplet3:
            return tuplet3;
        case tuplet6:
            return tuplet6;
        default:
            return null;
        }
    }
}
