//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                          S t r i n g L i s t O p t i o n h a n d l e r                         //
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
package org.audiveris.omr.util;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Localizable;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.OptionHandler;
import org.kohsuke.args4j.spi.Parameters;
import org.kohsuke.args4j.spi.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

/**
 * Class {@code StringListOptionhandler}
 *
 * @author Hervé Bitteur
 */
public class StringListOptionhandler
        extends OptionHandler<String>
{

    //~ Static fields/initializers -----------------------------------------------------------------
    private static final Logger logger = LoggerFactory.getLogger(StringListOptionhandler.class);

    //~ Instance fields ----------------------------------------------------------------------------
    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create an StringListOptionhandler object.
     *
     * @param parser Command line argument owner
     * @param option Run-time copy of the Option or Argument annotation
     * @param setter Setter interface
     */
    public StringListOptionhandler (CmdLineParser parser,
                                    OptionDef option,
                                    Setter<String> setter)
    {
        super(parser, option, setter);
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public String getDefaultMetaVariable ()
    {
        return "String[]";
    }

    @Override
    public int parseArguments (Parameters params)
            throws CmdLineException
    {
        int counter = 0;
        int paramsSize = params.size();

        for (; counter < paramsSize; counter++) {
            String param = params.getParameter(counter);

            if (param.startsWith("-")) {
                break;
            }

            for (String p : param.split(" ")) {
                if (!p.isEmpty()) {
                    setter.addValue(p);
                }
            }
        }

        if (counter == 0) {
            throw new CmdLineException(owner, Messages.MISSING_OPERAND, params.getParameter(counter));
        }

        return counter;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //----------//
    // Messages //
    //----------//
    private static enum Messages
            implements Localizable
    {
        MISSING_OPERAND;

        @Override
        public String formatWithLocale (Locale locale,
                                        Object... args)
        {
//        ResourceBundle localized = ResourceBundle.getBundle(Messages.class.getName(), locale);
//        return MessageFormat.format(localized.getString(name()),args);
            return "Missing String value after argument '" + args[0] + "'";
        }

        @Override
        public String format (Object... args)
        {
            return formatWithLocale(Locale.getDefault(), args);
        }
    }
}
