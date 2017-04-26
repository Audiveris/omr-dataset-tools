//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         O m r S h a p e                                        //
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
 * Class {@code OmrShape} is the OMR-Dataset definition of shapes.
 * <p>
 * Except the very first shape (None), the shape names are listed (rather) alphabetically for easier
 * visual browsing.
 * <p>
 * Flags direction (up/down) follow their stem direction, meaning that on say a stem down one can
 * find only flags down.
 * (Beware: this if the opposite of Audiveris initial definition of flag direction!).
 * <p>
 * NOTA for single-sign signatures: the 1-flat key is different from individual flat alteration
 * sign, and similarly the 1-sharp key is different from individual sharp alteration sign.
 * <p>
 * Shapes still to be studied: <ul>
 * <li>Repeat dot pair (?)
 * <li>Bracket (?)
 * <li>Additional predefined names for time combos
 * <li>Repeat signs (assuming we can limit bounding box to staff height): <ul>
 * <li>Left repeat sign (thick + thin + dots)
 * <li>Right repeat sign (dots + thin + thick)
 * <li>Back to back repeat sign (dots + thin + thick + thin + dots)
 * </ul>
 * </ul>
 *
 * @author Hervé Bitteur
 */
public enum OmrShape
{
    None("No valid shape"),
    Accent("Accent"),
    Arpeggiato("Arpeggiato"),
    AugmentationDot("Augmentation Dot"),
    Brace("Brace"),
    BreathMark("Breath Mark"),
    Breve("Double Whole"),
    Caesura("Caesura"),
    ClefC("Ut Clef"),
    ClefF("Bass Clef"),
    ClefF8a("Bass Clef Ottava Alta"),
    ClefF8b("Bass Clef Ottava Bassa"),
    ClefFSmall("Small Bass Clef"),
    ClefG("Treble Clef"),
    ClefG8a("Treble Clef Ottava Alta"),
    ClefG8b("Treble Clef Ottava Bassa"),
    ClefGSmall("Small Treble Clef"),
    ClefPercussion("Percussion Clef"),
    Coda("Closing section"),
    CommonTime("Alpha = 4/4"),
    CutTime("Semi-Alpha = 2/2"),
    DaCapo("D.C.: Repeat from the beginning"),
    DalSegno("D.S.: Repeat from the sign"),
    Digit0("Digit 0"),
    Digit1("Digit 1"),
    Digit2("Digit 2"),
    Digit3("Digit 3"),
    Digit4("Digit 4"),
    DoubleFlat("Double Flat"),
    DoubleSharp("Double Sharp"),
    DynamicF("F Forte"),
    DynamicFF("FF Fortissimo"),
    DynamicFFF("FFF"),
    DynamicFFFF("FFFF"),
    DynamicFFFFF("FFFFF"),
    DynamicFFFFFF("FFFFFF"),
    DynamicFP("FP FortePiano"),
    DynamicM("M"),
    DynamicMF("MF Mezzo Forte"),
    DynamicMP("MP Mezzo Piano"),
    DynamicP("P Piano"),
    DynamicPP("PP Pianissimo"),
    DynamicPPP("PPP"),
    DynamicPPPP("PPPP"),
    DynamicPPPPP("PPPPP"),
    DynamicPPPPPP("PPPPPP"),
    DynamicR("R"),
    DynamicRF("RF"),
    DynamicRFZ("RFZ"),
    DynamicS("S"),
    DynamicSF("SF"),
    DynamicSFF("SFF"),
    DynamicSFFZ("SFFZ"),
    DynamicSFP("SFP"),
    DynamicSFPP("SFPP"),
    DynamicSFZ("SFZ Sforzando"),
    DynamicZ("Z"),
    FermataAbove("Fermata above staff"),
    FermataBelow("Fermata below staff"),
    Fine("Fine"),
    Flag1Down("Single flag down"),
    Flag1Up("Single flag up"),
    Flag1UpSmall("Small single flag up"),
    Flag1UpSmallSlash("Small single flag up with a slash"),
    Flag2Down("Double flag down"),
    Flag2Up("Double flag up"),
    Flag3Down("Triple flag down"),
    Flag3Up("Triple flag up"),
    Flag4Down("Quadruple flag down"),
    Flag4Up("Quadruple flag up"),
    Flag5Down("Quintuple flag down"),
    Flag5Up("Quintuple flag up"),
    Flat("Minus one half step"),
    GraceNote("Grace Note"),
    GraceNoteSlash("Grace Note with a slash"),
    HairpinSegment("Wedge segment"),
    KeyFlat1("1-flat key"),
    KeyFlat2("2-flat key"),
    KeyFlat3("3-flat key"),
    KeyFlat4("4-flat key"),
    KeyFlat5("5-flat key"),
    KeyFlat6("6-flat key"),
    KeyFlat7("7-flat key"),
    KeySharp1("1-sharp key"),
    KeySharp2("2-sharp key"),
    KeySharp3("3-sharp key"),
    KeySharp4("4-sharp key"),
    KeySharp5("5-sharp key"),
    KeySharp6("6-sharp key"),
    KeySharp7("7-sharp key"),
    Ledger("Ledger"),
    Marcato("Strong accent"),
    Mordent("Mordent"),
    MordentSlash("Mordent with a slash"),
    Natural("Natural value"),
    NoteHeadBlack("Filled node head for quarters and less"),
    NoteHeadBlackSmall("Small filled note head for grace or cue"),
    NoteHeadVoid("Hollow node head for halves"),
    NoteHeadVoidSmall("Small hollow note head for grace or cue"),
    NoteWhole("Whole Note"),
    NoteWholeSmall("Small Whole Note"),
    OttavaAlta("8 va"),
    OttavaBassa("8 vb"),
    PedalDown("Engage Pedal"),
    PedalUp("Release Pedal"),
    PluckA("Plucking annulaire/anular/ring"),
    PluckI("Plucking index/indicio/index"),
    PluckM("Plucking majeur/medio/middle"),
    PluckP("Plucking pouce/pulgar/thumb"),
    Rest2("Rest for a 1/2"),
    Rest4("Rest for a 1/4"),
    Rest8("Rest for a 1/8"),
    Rest16("Rest for a 1/16"),
    Rest32("Rest for a 1/32"),
    Rest64("Rest for a 1/64"),
    Rest128("Rest for a 1/128"),
    Rest256("Rest for a 1/256"),
    Rest512("Rest for a 1/512"),
    Rest1024("Rest for a 1/1024"),
    RestBreve("Rest for 2 measures"),
    RestLong("Rest for 4 measures"),
    RestWhole("Rest for whole measure"),
    RomanI("Roman number 1"),
    RomanII("Roman number 2"),
    RomanIII("Roman number 3"),
    RomanIV("Roman number 4"),
    RomanV("Roman number 5"),
    RomanVI("Roman number 6"),
    RomanVII("Roman number 7"),
    RomanVIII("Roman number 8"),
    RomanIX("Roman number 9"),
    RomanX("Roman number 10"),
    RomanXI("Roman number 11"),
    RomanXII("Roman number 12"),
    Segno("Sign"),
    Sharp("Plus one half step"),
    Staccatissimo("Staccatissimo"),
    StaccatoDot("Staccato dot"),
    Stem("Stem"),
    Tenuto("To hold"),
    Time0("Time digit 0"),
    Time1("Time digit 1"),
    Time2("Time digit 2"),
    Time3("Time digit 3"),
    Time4("Time digit 4"),
    Time5("Time digit 5"),
    Time6("Time digit 6"),
    Time7("Time digit 7"),
    Time8("Time digit 8"),
    Time9("Time digit 9"),
    Time12("Time number 12"),
    Time16("Time number 16"),
    Time2_2("Time 2/2"),
    Time3_4("Time 3/4"),
    Time3_8("Time 3/8"),
    Time4_4("Time 4/4"),
    Time5_4("Time 5/4"),
    Time6_8("Time 6/8"),
    ToCoda("To coda"),
    Tr("Trill"),
    Tuplet3("Tuplet 3"),
    Tuplet6("Tuplet 6"),
    Turn("Turn"),
    TurnInverted("Turn Inverted"),
    TurnSlash("Turn with a slash"),
    TurnUp("Turn Up");

    /** Short explanation of the symbol shape. */
    public final String description;

    /**
     * Define a symbol shape
     *
     * @param description textual symbol description
     */
    OmrShape (String description)
    {
        this.description = description;
    }
}
