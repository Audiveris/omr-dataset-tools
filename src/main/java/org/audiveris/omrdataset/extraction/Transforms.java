//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       T r a n s f o r m s                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
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
package org.audiveris.omrdataset.extraction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jhlabs.image.DeinterlaceFilter;
import com.jhlabs.image.GaussianFilter;
import com.jhlabs.image.SharpenFilter;
import com.jhlabs.image.UnsharpFilter;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.imageio.ImageIO;

/**
 * Class <code>Transforms</code>
 *
 * @author Hervé Bitteur
 */
public class Transforms
{

    //~ Static fields/initializers -----------------------------------------------------------------
    private static final Logger logger = LoggerFactory.getLogger(Transforms.class);

    //~ Enumerations -------------------------------------------------------------------------------
    //~ Instance fields ----------------------------------------------------------------------------
    //~ Constructors -------------------------------------------------------------------------------
    //~ Methods ------------------------------------------------------------------------------------
    public static void main (String... args)
            throws Exception
    {
        final Path dir = Paths.get("data/tests");
        final Path org = dir.resolve("org.png");
        if (!Files.exists(org)) {
            logger.warn("File {} does not exist", org);
            return;
        }

        final BufferedImage img = ImageIO.read(org.toFile());

        {
            final GaussianFilter filter = new GaussianFilter();
            final BufferedImage modif = filter.filter(img, null);
            ImageIO.write(modif, "png", dir.resolve("gauss.png").toFile());
        }

        {
            final UnsharpFilter filter = new UnsharpFilter();
            final BufferedImage modif = filter.filter(img, null);

            ImageIO.write(modif, "png", dir.resolve("unsharp.png").toFile());
        }

        {
            final DeinterlaceFilter filter = new DeinterlaceFilter();
            filter.setMode(DeinterlaceFilter.AVERAGE);
            final BufferedImage modif = filter.filter(img, null);

            ImageIO.write(modif, "png", dir.resolve("deinterlace.png").toFile());
        }

        {
            final SharpenFilter filter = new SharpenFilter();
            final BufferedImage modif = filter.filter(img, null);

            ImageIO.write(modif, "png", dir.resolve("sharpen.png").toFile());
        }
    }
    //~ Inner Classes ------------------------------------------------------------------------------
}
