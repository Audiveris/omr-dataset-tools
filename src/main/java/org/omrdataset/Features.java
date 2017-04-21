//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         F e a t u r e s                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
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

import static org.omrdataset.App.*;
import org.omrdataset.util.FileUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import javax.imageio.ImageIO;

/**
 * Class {@code Features} reads input images data (collection of pairs: sheet image and
 * symbol descriptors) and produces the CSV file meant for NN training.
 * <p>
 * In the CSV file, there must be one record per symbol, containing the pixels of the sub-image
 * centered on the symbol center, followed by the (index of) symbol name.
 *
 * @author Hervé Bitteur
 */
public class Features
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(Features.class);

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * Process the available pairs (image + annotations) to extract the corresponding
     * symbols features, later used to train the classifier.
     *
     * @throws Exception
     */
    public void process ()
            throws Exception
    {
        // Scan the data folder for pairs of files: foo.png and foo.xml
        try {
            final OutputStream os = new FileOutputStream(CSV_PATH.toFile());
            final BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            final PrintWriter out = new PrintWriter(bw);

            Files.walkFileTree(IMAGES_PATH,
                               new SimpleFileVisitor<Path>()
                       {
                           @Override
                           public FileVisitResult visitFile (Path path,
                                                             BasicFileAttributes attrs)
                                   throws IOException
                           {
                               final String fileName = path.getFileName().toString();

                               if (fileName.endsWith(IMAGE_EXT)) {
                                   BufferedImage img = ImageIO.read(path.toFile());
                                   logger.info("\n*** We got image {}", path);

                                   WritableRaster raster = img.getRaster();

                                   byte[] bytes = getBytes(img);

                                   // Make sure we have the xml counterpart
                                   // And unmarshal the xml information
                                   String radix = FileUtil.getNameSansExtension(path);
                                   String infoName = radix + App.INFO_EXT;
                                   Path infoPath = path.resolveSibling(infoName);
                                   PageAnnotations pageInfo = PageAnnotations.unmarshal(infoPath);
                                   logger.info("We got info {}", pageInfo);

                                   PageProcessor processor = new PageProcessor(
                                           raster.getWidth(),
                                           raster.getHeight(),
                                           bytes,
                                           pageInfo);

                                   try {
                                       processor.process(out);
                                   } catch (Exception ex) {
                                       logger.warn("Exception " + ex, ex);
                                   }
                               }

                               return FileVisitResult.CONTINUE;
                           }
                       });
            out.flush();
            out.close();
        } catch (Throwable ex) {
            logger.warn("Error loading data from " + IMAGES_PATH + " " + ex, ex);
        }
    }

    /**
     * Direct entry point.
     *
     * @param args not used
     * @throws Exception
     */
    public static void main (String[] args)
            throws Exception
    {
        new Features().process();
    }

    /**
     * Extract the bytes array of the provided BufferedImage.
     *
     * @param bi the provided image
     * @return the bytes array
     */
    private byte[] getBytes (BufferedImage bi)
    {
        logger.info("type={}", bi.getType());

        if (bi.getType() != BufferedImage.TYPE_BYTE_GRAY) {
            throw new IllegalArgumentException("Type!=TYPE_BYTE_GRAY");
        }

        WritableRaster raster = bi.getRaster();
        DataBuffer buffer = raster.getDataBuffer();
        DataBufferByte byteBuffer = (DataBufferByte) buffer;
        byte[] bytes = byteBuffer.getData();

        //        int width = raster.getWidth();
        //        int height = raster.getHeight();
        // Invert bytes, so that 0=background and 255=foreground
        for (int i = bytes.length - 1; i >= 0; i--) {
            int val = bytes[i] & 0xFF;
            val = 255 - val;
            bytes[i] = (byte) val;
        }

        return bytes;
    }
}
